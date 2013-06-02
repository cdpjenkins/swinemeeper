(ns swinemeeper.cljs.script
  (:use [domina :only [by-id value set-text! text xpath set-attr!]])
  (:require [domina.events :as ev])

  )

(.write js/document "Hello, ClojureScript!")

(defn do-stuff []
  (let [board (by-id "board")]
        (set-text! board "ston"))
  (doseq [i (range 9)]
    (let [img ( by-id (str i))]
      (.log js/console img)
      (set-attr! img "src" (str "images/" (mod (inc i) 9) ".png")))))

(.log js/console (str "Waaaaston"))
(set! (.-onload js/window) do-stuff)

;; (defn ^:export connect []
;;   (.log js/console "About to ...")
;;   (repl/connect "http://localhost:9000/repl")
;;     (.log js/console "Dunnit"))

(defn clicked [x]
  (.log js/console (str "You clicked on " x))
  )

(defn ^:export init []
  (if (and js/document
           (.-getElementById js/document))
    (doseq [i (range 9)]
      (ev/listen! (by-id (str i)) :click #(clicked (str i))))))
