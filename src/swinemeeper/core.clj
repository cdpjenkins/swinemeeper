(ns swinemeeper.core
  (:use swinemeeper.board
        [compojure.core :only (defroutes GET)]
        [ring.adapter.jetty :as ring]
        [hiccup.core]))

(defn index []
  (html
   [:html
    [:head
     [:title "I am a title"]]
    [:body
     [:h1 "Hello!"]
     "This is about it, to be honest"
     [:div {:id "board"}
      "This is where the board would go. If there was one"]]]))

(defroutes routes
  (GET "/" [] (index)))

(defn make-server []
  (let [s (atom (run-jetty (var routes) {:port 8080 :join? false}))]
;   (.stop @s)
   s))

(defn -main []
 ; (run-jetty routes {:port 8080 :join? false})
  (make-server))
