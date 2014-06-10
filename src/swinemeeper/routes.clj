(ns swinemeeper.routes
  (:use [compojure.core]
        [ring.middleware.edn :only [wrap-edn-params]]
        [ring.adapter.jetty :only [run-jetty]])
  (:require [swinemeeper.views :as view]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as ring-response])
  (:gen-class))

(defn- wrap-cache-control [handler]
  (fn [req]
    (ring-response/header (handler req) "Cache-Control" "max-age=3600" )))

(defroutes main-routes
  (GET "/" {session :session } (view/index session))
  (GET "/dump-session" {session :session} (view/dump-session session))
  (POST "/ajax-new-board" {session :session} (view/ajax-new-board session))
  (POST "/ajax-click" {{x :x, y :y} :params
                       session :session} (do
                                           (view/ajax-click session x y)))
  (POST "/ajax-right-click" {{x :x, y :y} :params
                             session :session} (view/ajax-right-click session x y))
  (-> (route/resources "/")
      (wrap-cache-control))
  (route/not-found "Page not found"))

(def app
  (-> main-routes
      (handler/site)
      (wrap-edn-params)))

(defn make-server
  ([]
     (make-server 8000))
  ([port]
     (let [port port]
       (run-jetty (var app) {:port port :join? false}))))

(defn -main [port ]
  (make-server (Integer/parseInt port)))

(comment
  (def s (make-server))
  (.start s)
  (.stop s)
)
