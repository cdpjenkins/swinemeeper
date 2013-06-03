(ns swinemeeper.cljs.script
  (:require [domina :as dom]
            [domina.events :as ev]
            [hiccups.runtime :as hiccupsrt])
  (:require-macros [hiccups.core :as h]))

(.write js/document "Hello, ClojureScript!")


;; (defn ^:export connect []
;;   (.log js/console "About to ...")
;;   (repl/connect "http://localhost:9000/repl")
;;     (.log js/console "Dunnit"))

(defn clicked [x]
  (dom/log (str "You clicked on " x))
  )

(defn ^:export init []
  (if (and js/document
           (.-getElementById js/document))
    (doseq [i (range 9)]
      (ev/listen! (dom/by-id (str i)) :click #(clicked (str i)))))
  (let [board (dom/by-id :board)]
    (dom/append! board (h/html
                        [:table
                         (for [y (range 9)]
                           [:tr
                            (for [x (range 9)]
                              [:td
                               [:img {:src (str "images/" (mod (+ x y) 9) ".png")
                                      :id (str x "_" y)}]])])])))
  (doseq [x (range 9)
          y (range 9)]
    (ev/listen! (dom/by-id (str x "_" y)) :click #(dom/log (str "You clicked on " x "_" y)))))

(defn do-stuff []
  (init)
  ;; (let [board (dom/by-id "board")]
  ;;       (dom/set-text! board "ston"))
  (doseq [i (range 9)]
    (let [img (dom/by-id (str i))]
      (dom/log img)
      (dom/set-attr! img "src" (str "images/" (mod (inc i) 9) ".png")))))

(dom/log (str "Waaaaston"))
(set! (.-onload js/window) do-stuff)
