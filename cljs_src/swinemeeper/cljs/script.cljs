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

(defn create-board [board]
  (let [board-div (dom/by-id :board)]
    (dom/destroy-children! board-div)
    (dom/append! board-div
                 (h/html
                  [:table
                   [:tr
                    [:td {:colspan "5"}
                     [:div {:id "swines-remaining"} (:remaining-swines board)]]
                    (for [i (range (- (:width board) 10))]
                      [:td])
                    [:td {:colspan "5"}
                     [:div {:id "game-state"} (:state board)]]]
                   (for [y (range (:height board))]
                     [:tr
                      (for [x (range (:width board))]
                        [:td
                         [:img {:src (str "images/:unknown.png")
                                :id (str x "_" y)}]])])]
                  [:form {:id "new-game-form"}
                   [:fieldset
                    [:label
                     "Easy"
                     [:input {:type "radio" :name "type" :checked "no"}]]
                    [:label
                     "Medium"
                     [:input {:type "radio" :name "type" :checked "no"}]]
                    [:label
                     "Hard"
                     [:input {:type "radio" :name "type" :checked "no"}]]
                    [:input {:type "button"
                             :value "New Game"
                             :id "new-game"}]]])))

    (doseq [x (range (:height board))
            y (range (:width board))]
      (ev/listen! (dom/by-id (str x "_" y))
                  :click
                  (fn [event]
                    (POST "ajax-click"
                          {:params {:x x
                                    :y y}
                           :handler (fn [response]
                                      (update-thar-board response))})))
      (ev/listen! (dom/by-id (str x "_" y))
                  :contextmenu
                  (fn [event]
                    (POST "ajax-right-click"
                          {:params {:x x
                                    :y y}
                           :handler (fn [response]
                                      (update-thar-board response))})
                    (ev/prevent-default event))))
    (ev/listen! (dom/by-id "new-game")
                :click
                (fn [event]
                  (POST "ajax-new-board "
                        {:params {}
                         :handler (fn [response]
                                    (create-board response))}))))

(defn update-thar-board [board]
  (doseq [x (range (:height board))
          y (range (:width board))]
    (dom/set-attr! (dom/by-id (str x "_" y)) :src (str "images/" (board [x y]) ".png")))
  (dom/set-text! (dom/by-id "swines-remaining")
                 (:remaining-swines board))
  (dom/set-text! (dom/by-id "game-state")
                 (states-to-strings (:state board))))

(defn ^:export init []
  (POST "ajax-new-board "
                      {:params {}
                       :handler (fn [response]
                                  (create-board response))})
  )

(defn do-stuff []
  (init))

(set! (.-onload js/window) do-stuff)
