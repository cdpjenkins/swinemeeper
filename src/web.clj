(ns web
  (:use swinemeeper
        gui
        compojure.core
        hiccup.core
        hiccup.page-helpers
        ring.adapter.jetty
        ring.util.response)

  (:require [compojure.route :as route])
  (:gen-class))

; because import of clojure contrib is being annoying
(defn indexed [s]
  (map vector (iterate inc 0) s))

(defn make-left-click-form [x y]
  [:form {:action (str "/swinemeeper/click")
          :method "post"
          :id (str x "_" y)}
    [:input {:type "hidden"
             :name "bx"
             :value (str x)}]
    [:input {:type "hidden"
             :name "by"
             :value (str y)}]])

(defn make-square [x y square]
  [:img {:src (str "/images/" (images square))
         :oncontextmenu (str "rightClick("x ", " y ");return false;")
         :onclick (str "leftClick("x ", " y ");return false;")
         :ondblclick (str "doubleClick("x ", " y ")")
         }])

;;;;; web esque stuff
(defn draw-board []
  (let [view (:view @game)]
    (html
      [:html
        [:head [:title "Swine Meeper"]
          [:script { :src "/js/scripts.js"
                     :type "text/javascript"}]]
        [:body

          (for [y (range (count view))
                x (range (count (view 0)))]
            (do
              (make-left-click-form x y)))


         [:table
           (for [ [y row] (indexed view)]
             [:tr
               (for [ [x square] (indexed row)]
                 [:td
                   (make-square x y square)])])]]])))

(defn serve-image [name]
  (let [path (str "src/" name ".png")]
    (file-response path)))

(defn serve-js [name]
  (let [path (str "src/" name ".js")]
    (file-response path)))

(defn hoss []
  (println "hoss")
  "<h1>Mooing bovine cow!</h1>")

(defn web-left-click [coords]
  (left-click coords))
;  (draw-board))

(defn web-right-click [coords]
  (right-click coords))
;  (draw-board))

(defn web-double-click [coords]
  (double-click coords))
;  (draw-board))

(defroutes example
  (GET "/images/:name.png" [name] (serve-image name))

  (GET "/js/:name.js" [name] (serve-js name))

  (GET "/swinemeeper" [] 
    (draw-board))

  (POST "/swinemeeper/click" [bx by]
    (do
      (println "huss" bx)
      (println "hoss" by)
      (web-left-click 
       [(Integer/parseInt bx)
        (Integer/parseInt by)])
      (redirect "/swinemeeper")))

  (POST "/swinemeeper/rightclick" [bx by]
    (do
      (println "huss" bx)
      (println "hoss" by)
      (web-right-click 
       [(Integer/parseInt bx)
        (Integer/parseInt by)])
      (redirect "/swinemeeper")))

  (POST "/swinemeeper/doubleclick" [bx by]
    (do
      (println "huss" bx)
      (println "hoss" by)
      (web-double-click 
       [(Integer/parseInt bx)
        (Integer/parseInt by)])
      (redirect "/swinemeeper")))

  (route/not-found "Page not found") )

(def server 
 (let [s (atom (run-jetty (var example) {:port 8080 :join? false}))]
   (.stop @s)
   s))


;(defonce server (run-jetty (var example) {:port 8080 :join? false}))
