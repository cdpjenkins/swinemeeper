(ns swinemeeper.remotes
  (:require [swinemeeper.core :refer [app]]
            [swinemeeper.board :as s]
            [compojure.handler :refer [site]]
            [ring.adapter.jetty :refer [run-jetty]]
            [shoreleave.middleware.rpc :refer [defremote wrap-rpc]]))

(defremote add-me-do [x y]
  (println x y)
  (+ x y))

(def board (atom (s/make-board (s/make-swines 10 10 10 [5 5]) 10 10)))

(defremote new-board []
  (println "in here!!!")
  (let [swines (s/make-swines 10 10 10 [5 5])]
    (reset! board (s/make-board swines 10 10))
    (println @board)
    @board))

(defremote revealed-board []
  (let [swines (s/make-swines 10 10 10 [5 5])
        board (s/make-board swines 10 10)
;        revealed-board (s/fully-reveal-board-on-win board)
        ]
    (println (str "swines: " swines))
    (println (str "board:  " board))
    board))

(defremote click [x y]
  (println "click")
  (println @board)
  (swap! board s/uncover [[x y]])
  (println @board)
  @board
  )

(defremote right-click [x y]
  (println "right click" x ", " y)
  (swap! board s/mark [x y]))

(def remote-app (-> (var app)
             (wrap-rpc)
             (site)))

(defn make-server []
  (let [server  (run-jetty (var remote-app) {:port 3000 :join? false})]
    (.stop server)
    (atom server)))

;; (def s (make-server))
;; (.start @s)
