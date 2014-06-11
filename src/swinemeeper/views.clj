(ns swinemeeper.views
  (:use [swinemeeper.board :as s]
        [compojure.core]
        [hiccup.core]
        [hiccup.page :only [include-js]]
        [hiccup.element :only [javascript-tag]])
  (:require [ring.util.response :as ring-response]))

(defn- include-clojurescript []
  (list
   (include-js "js/script.js")))

(defn index [session]
  (->
   (html
    [:html
     [:head
      [:title "Swinemeeper"]
      [:link {:href "style/style.css"
              :rel "stylesheet"
              :type "text/css"}]
      (include-clojurescript)]
     [:body
      [:center {:id "swinemeeper"}
       [:h1 "Swinemeeper!"]
       [:div {:id "board"}]]]])
   (ring-response/response)))

(defn dump-session [session]
  (str session))

(defn ajax-response
  "Common function for returning response to client and saving board state into session"
  [board session]
  (-> board
      (sanitise-board)
      (pr-str)
      (ring-response/response)
      (assoc :session (assoc session :board board))))

(defn ajax-fn [session f & rest]
  (let [board (:board session)
        board (apply f (cons board rest))]
    (ajax-response board session)))

(defn ajax-click [session x y]
  (ajax-fn session uncover [[x y]]))

(defn ajax-right-click [session x y]
  (ajax-fn session mark [x y]))

(defn ajax-new-board [session]
  (let [[width height num-swines] [10 10 10] ; [30 16 99] ; [16 16 40] ; 
        swines (s/make-swines width height num-swines [5 5])
        board (s/make-board swines width height)]
    (ajax-response board session)))


