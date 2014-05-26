(ns swinemeeper.core
  (:use
   [swinemeeper.board :as s]
   compojure.core
   [ring.adapter.jetty :as ring]
   [hiccup.core]
   [hiccup.middleware :only (wrap-base-url)])
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
  (println session)
  (->
   (html
    [:html
     [:head
      [:title "I am a title"]]
     [:body
      [:h1 "Hello!"]
      [:div {:id "board"}]
      [:input {:type "button"
               :value "New Game"
               :id "new-game"}]
      [:div {:id "board-me-do:"}]

      ;; (for [i (range 9)]
      ;;   [:img {:src (str "images/" i ".png")
      ;;          :id (str i)}])
                                        ;(include-js "/js/script.js")
      (run-clojurescript
       "/js/script.js"
       "swinemeeper.cljs.repl.connect()")


      ]])
   (ring-response/response)
   (assoc :session (assoc session :ston "huss"))))

(defn dump-session [session]
  (str session))

(defn ajax-hello [skank session]
  (pr-str
   [
    skank session]))

(def board (atom (s/make-board (s/make-swines 10 10 10 [5 5]) 10 10)))

(defn ajax-click [x y]
  (swap! board s/uncover [[x y]])
  @board)

(defn ajax-right-click [x y]
  (swap! board s/mark [x y]))

(defn ajax-new-board []
  (let [swines (s/make-swines 10 10 10 [5 5])]
    (reset! board (s/make-board swines 10 10))
    @board))

(defn ajax-skankston [skank session]
  (pr-str ["skankston" skank session]))

(defroutes main-routes
  (GET "/" {session :session } (index session))
  (GET "/dump-session" {session :session} (dump-session session))
  (GET "/ajax-hello" {{skank :skank} :params
                 session :session} (ajax-hello skank session))
  (POST "/ajax-skankston" {{skank :skank} :params
                           session :session}
        (ajax-skankston skank session))
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

(def server (atom nil))

(defn make-server []
  (when (not (nil? @server))
    (.stop @server))
  (reset! server (run-jetty (var app) {:port 8080 :join? false})))

(defn -main []
 ; (run-jetty routes {:port 8080 :join? false})
  (make-server))

(comment
  (def s (make-server))
  (.start @s)
  (.stop @s)

  )
