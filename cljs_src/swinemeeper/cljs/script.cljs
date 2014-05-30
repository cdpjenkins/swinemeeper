(ns swinemeeper.cljs.script
  (:require [domina :as dom]
            [domina.events :as ev]
            [hiccups.runtime :as hiccupsrt]
            [ajax.core :refer [GET POST]]
            )
  (:require-macros [hiccups.core :as h]))

(defn log [& rest]
  (.log js/console (apply str rest)))

(def states-to-strings
  {
   :game-playing "Game Playing"
   :game-won     "Game Won"
   :game-lost    "Game Lost"})

(defn create-board [board-state]
  (let [board (dom/by-id :board)]
    (dom/destroy-children! board)
    (dom/append! board
                 (h/html
                  [:table
                   (for [y (range 10)]
                     [:tr
                      (for [x (range 10)]
                        [:td
                         [:img {:src (str "images/:unknown.png")
                                :id (str x "_" y)}]])])]
                  [:div {:id "swines-remaining"}]
                  [:div {:id "game-state"}])))
    (doseq [x (range 10)
            y (range 10)]
      (ev/listen! (dom/by-id (str x "_" y))
                  :click
                  (fn [event]
                    (POST "/ajax-click"
                          {:params {:x x
                                    :y y}
                           :handler (fn [response]
                                      (log "before: " (.getTime (js/Date.)))
                                      (update-thar-board response)
                                      (log "after:  " (.getTime (js/Date.))))})))
      (ev/listen! (dom/by-id (str x "_" y))
                  :contextmenu
                  (fn [event]
                    (POST "/ajax-right-click"
                          {:params {:x x
                                    :y y}
                           :handler (fn [response]
                                      (update-thar-board response))})
                    ;; TODO
                    ;; (remote-callback :right-click
                    ;;                  [x y]
                    ;;                  (fn [result]
                    ;;                    (update-thar-board result)))
                    (ev/prevent-default event)))))

(defn update-thar-board [board]
  (doseq [x (range 10)
          y (range 10)]
    (dom/set-attr! (dom/by-id (str x "_" y)) :src (str "images/" (board [x y]) ".png")))
  (dom/set-text! (dom/by-id "swines-remaining")
                 (:num-swines board))
  (dom/set-text! (dom/by-id "game-state")
                 (states-to-strings (:state board))))

(defn ^:export init []
;;  (create-board nil)
  (ev/listen! (dom/by-id "new-game")
              :click
              (fn [event]
                (POST "/ajax-new-board "
                      {:params {}
                       :handler (fn [response]
                                  (create-board board))}))))

(defn do-stuff []
  (init))

(set! (.-onload js/window) do-stuff)
