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

;; TDO - to cljx
(def states-to-strings
  {:game-playing "Game Playing"
   :game-won     "Game Won"
   :game-lost    "Game Lost"})

(defn update-thar-board [board]
  (doseq [y (range (:height board))
          x (range (:width board))]
    (dom/set-attr! (dom/by-id (str x "_" y)) :src (str "images/" (board [x y]) ".png")))
  (dom/set-text! (dom/by-id "swines-remaining")
                 (:remaining-swines board))
  (dom/set-text! (dom/by-id "game-state")
                 (states-to-strings (:state board))))

(defn create-board [board]
  (let [board-div (dom/by-id :board)]
    (dom/destroy-children! board-div)
    (dom/append! board-div
                 (h/html
                  [:table
                   [:tr
                    [:td {:colspan "3"}
                     [:div {:id "swines-remaining"} (:remaining-swines board)]]
                    (for [i (range (- (:width board) 10))]
                      [:td])
                    [:td {:colspan "7"}
                     [:div {:id "game-state"} (:state board)]]]
                   (for [y (range (:height board))]
                     [:tr
                      (for [x (range (:width board))]
                        [:td
                         [:img {:src (str "images/:unknown.png")
                                :id (str x "_" y)
                                ;:width 25
                                ;:height 25
                                }]])])
                   [:tr
                    [:td {:colspan (str (:width board))}
                     [:form {:id "new-game-form"}
                      [:center
                       [:div {:id "game-types"}
                        [:span {:id "game-type"}
                         "Easy"
                         [:input {:type "radio" :name "type" :value "Easy" :id "easy-button"}]]
                        [:span {:id "game-type"}
                         "Medium"
                         [:input {:type "radio" :name "type" :value "Medium" :id "medium-button"}]]
                        [:span {:id "game-type"}
                         "Hard"
                         [:input {:type "radio" :name "type" :value "Hard" :id "hard-button"}]]]
                       [:div {:id "new-game-button-div"}
                        [:input {:type "button"
                                 :value "New Game"
                                 :id "new-game"}]]]]]]])))

  (ev/listen! (dom/by-id "easy-button")
              :click)

  (ev/listen! (dom/by-id "medium-button")
              :click)

  (ev/listen! (dom/by-id "hard-button")
              :click)

  (doseq [y (range (:height board))
          x (range (:width board))]
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
