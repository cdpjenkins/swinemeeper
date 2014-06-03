(ns swinemeeper.core
  (:use
   [swinemeeper.board :as s]
   compojure.core
   [ring.adapter.jetty :as ring]
   [hiccup.core]
   [hiccup.middleware :only (wrap-base-url)]
   [ring.middleware.params :only [wrap-params]]
   [ring.middleware.edn :only [wrap-edn-params]])
  
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [ring.middleware.session :as session]

            [ring.util.response :as ring-response]
            [hiccup
             [page :refer [html5]]
             [page :refer [include-js]]
             [element :refer [javascript-tag]]]))
  ;; (:require [compojure.handler :as handler]
  ;;           [compojure.route :as route])

(defn- run-clojurescript [path init]
  (list
    (include-js path)
    (javascript-tag init)))

(defn index [session]
  (->
   (html
    [:html
     [:head
      [:title "Swinemeeper"]]
     [:body
      [:h1 "Swinemeeper!"]
      [:div {:id "board"}]
      [:input {:type "button"
               :value "New Game"
               :id "new-game"}]
      [:div {:id "board-me-do:"}]

      (run-clojurescript
       "js/script.js"
       "swinemeeper.cljs.repl.connect()")]])
   (ring-response/response)
   (assoc :session (assoc session :ston "huss"))))

(defn dump-session [session]
  (str session))

(def counter (atom 0))

(defn ajax-click [session x y]
  (let [board (-> (:board session)
                  (uncover [[x y]]))]
    (-> board
        (sanitise-board)
        (pr-str)
        (ring-response/response)
        (assoc :session (assoc session :board board)))))
;; last line there is a bit weird eh

(defn ajax-right-click [session x y]
  (let [board (:board session)
        board (mark board [x y])]
    (-> (pr-str board)
        (ring-response/response)
        (assoc :session (assoc session :board board)))))

(defn ajax-new-board [session]
  (let [swines (s/make-swines 10 10 10 [5 5])
        board (s/make-board swines 10 10)]
    (-> (pr-str board)
        (ring-response/response)
        (assoc :session {:board board}))))

(defn wrap-me-do [handler]
  (fn [req]
    (ring-response/header (handler req) "Cache-Control" "max-age=3" )))

(defroutes main-routes
  (GET "/" {session :session } (index session))
  (GET "/dump-session" {session :session} (dump-session session))
  (POST "/ajax-new-board" {session :session} (ajax-new-board session))
  (POST "/ajax-click" {{x :x, y :y} :params
                       session :session} (do
                                           (ajax-click session x y)))
  (POST "/ajax-right-click" {{x :x, y :y} :params
                             session :session}
        (do (ajax-right-click session x y)))
  (-> (route/resources "/")
      (wrap-me-do))
  (route/not-found "Page not found"))

(def app
  (-> main-routes
      (handler/site)
      (wrap-edn-params)))

(def server (atom nil))

(defn make-server
  ([]
     (make-server 8000))
  ([port]
     (let [port port]
       ( when (not (nil? @server))
         (.stop @server))
       (reset! server (run-jetty (var app) {:port port :join? false})))))

(defn -main [port ]
  (make-server (Integer/parseInt port)))

(comment
  (def s (make-server))
  (.start s)
  (.stop s)
)
