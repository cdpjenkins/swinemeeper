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
      (include-clojurescript)
      [:meta {:charset "utf-8"}]]
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
      (ring-response/response)
      (assoc :session (assoc session :board board))))

(defn ajax-fn [session f & rest]
  (let [board (:board session)
        board (apply f (cons board rest))]
    (ajax-response board session)))

(defn ajax-click [session x y]
  (println "ajax-click: " [x y])
  (ajax-fn session uncover [[x y]]))

(defn ajax-right-click [session x y]
  (ajax-fn session mark [x y]))

(def game-types {"Easy" [10 10 10]
                 "Medium" [16 16 40]
                 "Hard" [30 16 99]})

(defn ajax-new-board [session game-type]
  (let [game-type (if game-type game-type "Easy")
        [width height num-swines] (game-types game-type)
        board (s/make-board num-swines width height game-type)]
    (println "ajax-new-board: " game-type)
    (println "board: " board)
    (ajax-response board session)))


