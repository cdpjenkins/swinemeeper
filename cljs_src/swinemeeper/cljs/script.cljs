(ns swinemeeper.cljs.script
  (:use [domina.xpath :only [xpath]])
  (:require [domina :as dom]
            [domina.events :as ev]
            [hiccups.runtime :as hiccupsrt]
            [ajax.core :refer [GET POST]]
            [swinemeeper.cljs.repl :as smrepl])
  (:require-macros [hiccups.core :as h]))

(defn log [& rest]
  (.log js/console (apply str rest)))

;; TODO - to cljx
(def states-to-strings
  {:pregame      "Pregame"
   :game-playing "Game Playing"
   :game-won     "Game Won"
   :game-lost    "Game Lost"})

(defn update-board [board]
  (doseq [y (range (:height board))
          x (range (:width board))]
    (dom/set-attr! (dom/by-id (str x "_" y)) :src (str "images/" (board [x y]) ".png")))
  (dom/set-text! (dom/by-id "swines-remaining")
                 (:remaining-swines board))
  (dom/set-text! (dom/by-id "game-state")
                 (states-to-strings (:state board))))

(defn- make-type-radio [board type id]
  (let [attrs {:type "radio"
            :name "type"
            :value type
            :id id}
        attrs (if (= (:type board) type)
                (assoc attrs :checked "true")
                attrs)]
    [:span {:id "game-type"}
     type
     [:input attrs]]))

(defn create-board [board]
  (let [board-div (dom/by-id :board)]
    (dom/destroy-children! board-div)
    (dom/append! board-div
                 (h/html
                  [:table
                   [:tr
                    [:td {:colspan (str (:width board))}
                     [:div {:id "swines-remaining"} (:remaining-swines board)]
                     [:div {:id "game-state"}
                      (states-to-strings (:state board))]]]
                   (for [y (range (:height board))]
                     [:tr
                      (for [x (range (:width board))]
                        [:td
                         [:img {:src (str "images/:unknown.png")
                                :id (str x "_" y)
                                :width 30
                                :height 30}]])])
                   [:tr
                    [:td {:colspan (str (:width board))}
                     [:form {:id "new-game-form"}
                      [:center
                       [:div {:id "game-types"}
                        (make-type-radio board "Easy" "easy-button")
                        (make-type-radio board "Medium" "medium-button")
                        (make-type-radio board "Hard" "hard-button")]
                       [:div {:id "new-game-button-div"}
                        [:input {:type "button"
                                 :value "New Game"
                                 :id "new-game"}]]]]]]])))

  (ev/listen! (dom/by-id "easy-button")
              :click
              #(log "Easy"))

  (ev/listen! (dom/by-id "medium-button")
              :click
              #(log "Med"))

  (ev/listen! (dom/by-id "hard-button")
              :click
              #(log "Hard"))

  (doseq [y (range (:height board))
          x (range (:width board))]
    (ev/listen! (dom/by-id (str x "_" y))
                :click
                (fn [event]
                  (POST "ajax-click"
                        {:params {:x x
                                  :y y}
                         :handler (fn [response]
                                    (update-board response))
                         :error-handler (fn [{:keys [status status-text]}]
                                          (log "HTTP " status ":" status-text)
)})))
    (ev/listen! (dom/by-id (str x "_" y))
                :contextmenu
                (fn [event]
                  (log "ffs")
                  (POST "ajax-right-click"
                        {:params {:x x
                                  :y y}
                         :handler (fn [response]
                                    (update-board response))})
                  (ev/prevent-default event))))
  
  (ev/listen! (dom/by-id "new-game")
              :click
              (fn [event]
                (let [buttons (dom/nodes (xpath "//div[@id='game-types']//input[@name='type']"))
                      selected (dom/single-node (filter #(.-checked %) buttons))
                      game-type (if selected
                                  (.-value selected)
                                  "Easy")]
                  (POST "ajax-new-board "
                        {:params {:game-type game-type}
                         :handler (fn [response]
                                    (create-board response))})))))



(defn ^:export init []
  (smrepl/connect)
  (POST "ajax-new-board "
                      {:params {}
                       :handler (fn [response]
                                  (create-board response))}))

(set! (.-onload js/window) init)
