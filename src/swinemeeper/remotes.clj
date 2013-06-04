(ns swinemeeper.remotes
  (:require [swinemeeper.core :refer [app]]
            [swinemeeper.board :as s]
            [compojure.handler :refer [site]]
            [shoreleave.middleware.rpc :refer [defremote wrap-rpc]]))

(defremote add-me-do [x y]
  (println x y)
  (+ x y))

(defremote revealed-board []
  (let [swines (s/make-swines 10 10 10 [5 5])
        board (s/make-board swines 10 10)
        revealed-board (s/fully-reveal-board-on-win board)]
    revealed-board))

(def remote-app (-> (var app)
             (wrap-rpc)
             (site)))
