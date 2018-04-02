(ns swinemeeper.routes
  (:use [compojure.core]
        [ring.middleware.transit :only [wrap-transit-response]]
        [ring.middleware.transit :only [wrap-transit-params]]
        [ring.adapter.jetty :only [run-jetty]])
  (:require [swinemeeper.views :as view]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as resp])
  (:gen-class))

(defn- wrap-cache-control [handler]
  (fn [req]
    (resp/header (handler req) "Cache-Control" "max-age=3600" )))

(defroutes main-routes
  (GET "/" {session :session } (view/index session))
  (GET "/dump-session" {session :session} (view/dump-session session))
  (POST "/ajax-new-board" {{game-type :game-type} :params
                           session :session} (view/ajax-new-board session game-type))
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
      ;;(wrap-edn-params)
      (wrap-transit-params)
      (wrap-transit-response)
     ))

(defn make-server
  ([]
     (make-server 8000))
  ([port]
     (let [port port]
       (run-jetty (var app) {:port port :join? false}))))

(defn -main
  ([]
   (make-server (Integer/parseInt (System/getenv "VCAP_APP_PORT"))))
  ([port]
   (make-server (Integer/parseInt port))))

(comment
  (def s (make-server))
  (.start s)
  (.stop s)
)
