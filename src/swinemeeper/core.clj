(ns swinemeeper.core
  (:use
;   swinemeeper.board
   compojure.core
   [ring.adapter.jetty :as ring]
   [hiccup.core]
   [hiccup.middleware :only (wrap-base-url)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [compojure.response :as response]
            [hiccup
             [page :refer [html5]]
             [page :refer [include-js]]
             [element :refer [javascript-tag]]
             ]))
  ;; (:require [compojure.handler :as handler]
  ;;           [compojure.route :as route])

(defn- run-clojurescript [path init]
  (list
    (include-js path)
    (javascript-tag init)))

(defn index []
  (html
   [:html
    [:head
     [:title "I am a title"]]
    [:body
     [:h1 "Hello!"]
     "This is about it, to be honest"
     [:div {:id "board"}
      "This is where the board would go. If there was one"]

     (for [i (range 9)]
       [:img {:src (str "images/" i ".png")
              :id (str i)}])
     ;(include-js "/js/script.js")
     (run-clojurescript
        "/js/script.js"
        "swinemeeper.cljs.repl.connect()")


     ]]))

(defroutes main-routes
  (GET "/" [] (index))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site main-routes)
      (wrap-base-url)))

;; (defroutes myroutes
;;   (GET "/" [] (index))
;;   ;; to serve static pages saved in resources/public directory
;; ;  (route/resources "/")
;;   ;; if page is not found
;; ;  (route/not-found "Page not found")
;;   )


;; (def handler
;;   (handler/site myroutes))


(defn make-server []
  (let [s (atom (run-jetty (var app) {:port 8080 :join? false}))]
;   (.stop @s)
   s))

(defn -main []
 ; (run-jetty routes {:port 8080 :join? false})
  (make-server))
