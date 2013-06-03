(ns swinemeeper.remotes
  (:require [swinemeeper.core :refer [app]]
            [compojure.handler :refer [site]]
            [shoreleave.middleware.rpc :refer [defremote wrap-rpc]]))

(defremote add-me-do [x y]
  (println x y)
  (+ x y))

(def remote-app (-> (var app)
             (wrap-rpc)
             (site)))
