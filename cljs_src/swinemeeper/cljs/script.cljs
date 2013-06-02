(ns swinemeeper.cljs.script
  (:require [domina :as dom]
            [domina.events :as ev])

  )

(.write js/document "Hello, ClojureScript!")

(defn do-stuff []
  (let [board (dom/by-id "board")]
        (dom/set-text! board "ston"))
  (doseq [i (range 9)]
    (let [img (dom/by-id (str i))]
      (dom/log img)
      (dom/set-attr! img "src" (str "images/" (mod (inc i) 9) ".png")))))

(dom/log (str "Waaaaston"))
(set! (.-onload js/window) do-stuff)

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
      (ev/listen! (dom/by-id (str i)) :click #(clicked (str i))))))
