(ns swinemeeper.remotes
  (:require [swinemeeper.core :refer [app]]
            [swinemeeper.board :as s]
            [compojure.handler :refer [site]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.session :as session]
            [shoreleave.middleware.rpc :refer [defremote wrap-rpc]]))

(defremote add-me-do [x y]
  (+ x y))

(defremote revealed-board []
  (let [swines (s/make-swines 10 10 10 [5 5])
        board (s/make-board swines 10 10)
;        revealed-board (s/fully-reveal-board-on-win board)
        ]
    board))


(def remote-app (-> (var app)
             (wrap-rpc)
             (site)))

(defn make-server []
  (let [server  (run-jetty (var remote-app) {:port 3000 :join? false})]
    (.stop server)
    (atom server)))

;; (def s (make-server))
;; (.start @s)
