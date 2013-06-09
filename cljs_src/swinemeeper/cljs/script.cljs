(ns swinemeeper.cljs.script
  (:require [domina :as dom]
            [domina.events :as ev]
            [hiccups.runtime :as hiccupsrt]
            [shoreleave.remotes.http-rpc :refer [remote-callback]]
            )
  (:require-macros [hiccups.core :as h]))

(.write js/document "Hello, ClojureScript!")

(defn update-thar-board [board]
  (doseq [x (range 10)
          y (range 10)]
    (dom/set-attr! (dom/by-id (str x "_" y)) :src (str "images/" (board [x y]) ".png"))))

(defn ^:export init []
  (let [board (dom/by-id :board)]
    (dom/append! board (h/html
                        [:table
                         (for [y (range 10)]
                           [:tr
                            (for [x (range 10)]
                              [:td
                               [:img {:src (str "images/:unknown.png")
                                      :id (str x "_" y)}]])])])))
  (doseq [x (range 10)
          y (range 10)]
    (ev/listen! (dom/by-id (str x "_" y))
                :click
                #((remote-callback :click
                                   [x y]
                                   (fn [result]
                                     (update-thar-board result))))))
  (remote-callback :revealed-board
                   []
                   (fn [result]
                     (dom/log result)
                     (doseq [x (range 10)
                             y (range 10)]
                       (dom/set-attr! (dom/by-id (str x "_" y)) :src (str "images/" (result [x y]) ".png"))))))

(defn do-stuff []
  (init))

(set! (.-onload js/window) do-stuff)
