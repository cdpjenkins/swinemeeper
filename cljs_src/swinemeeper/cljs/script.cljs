(ns swinemeeper.cljs.script
  (:require [domina :as dom]
            [domina.events :as ev]
            [hiccups.runtime :as hiccupsrt]
            [shoreleave.remotes.http-rpc :refer [remote-callback]]
            )
  (:require-macros [hiccups.core :as h]))

(dom/log "prrrrr!")
(.write js/document "Hello, ClojureScript!")


;; (defn ^:export connect []
;;   (.log js/console "About to ...")
;;   (repl/connect "http://localhost:9000/repl")
;;     (.log js/console "Dunnit"))

(defn clicked [x]
  (dom/log (str "You clicked on " x))
  )

(dom/log "moo")

(defn ^:export init []
  (if (and js/document
           (.-getElementById js/document))
    (doseq [i (range 9)]
      (ev/listen! (dom/by-id (str i)) :click #(clicked (str i)))))
  (let [board (dom/by-id :board)]
    (dom/append! board (h/html
                        [:table
                         (for [y (range 10)]
                           [:tr
                            (for [x (range 10)]
                              [:td
                               [:img {:src (str "images/" (mod (+ x y) 9) ".png")
                                      :id (str x "_" y)}]])])])))
  (doseq [x (range 9)
          y (range 9)]
    (ev/listen! (dom/by-id (str x "_" y)) :click #((dom/log (str "You clicked on " x "_" y))
                                                   (dom/log "waaa")
                                                   (remote-callback :add-me-do
                                                                    [x y]

                                                                    (fn [result]
                                                                      (dom/log "husssss!")
                                                                      (dom/log (str "Total is " result))))

                                                   ))
    )
  (remote-callback :revealed-board
                   []
                   (fn [result]
                     (dom/log result)
                     (doseq [x (range 10)
                             y (range 10)]
                       (dom/set-attr! (dom/by-id (str x "_" y)) :src (str "images/" (result [x y]) ".png"))
                       )

                     )))

(defn do-stuff []
  (init)
  ;; (let [board (dom/by-id "board")]
  ;;       (dom/set-text! board "ston"))
  ;; (doseq [i (range 9)]
  ;;   (let [img (dom/by-id (str i))]
  ;;     (dom/log img)
  ;;     (dom/set-attr! img "src" (str "images/" (mod (inc i) 9) ".png"))))
  )

(dom/log (str "Waaaaston"))
(set! (.-onload js/window) do-stuff)
