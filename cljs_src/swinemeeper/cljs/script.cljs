(ns swinemeeper.cljs.script
  (:require [domina :as dom]
            [domina.events :as ev]
            [hiccups.runtime :as hiccupsrt]
            [ajax.core :refer [GET POST]]
            )
  (:require-macros [hiccups.core :as h]))

(.write js/document "Hello, ClojureScript!")

(defn handler [response]
  (.log js/console (str response)))

(defn error-handler [{:keys [status status-text]}]
  (.log js/console
        (str "something bad happened: " status " " status-text)))

(defn hussp []
  (.log js/console
        (POST "/ajax-skankston"
             {:params {:skank "ston"}
              :handler handler
              :error-handler error-handler})))

(hussp)

(defn log [& rest]
  (apply #(.log js/console %) rest))

(defn create-board [board-state]
  (let [board (dom/by-id :board)]
    (dom/destroy-children! board)
    (dom/append! board (h/html
                        [:table
                         (for [y (range 10)]
                           [:tr
                            (for [x (range 10)]
                              [:td
                               [:img {:src (str "images/:unknown.png")
                                      :id (str x "_" y)}]])])])))
    (doseq [x (range 10)
            y (range 10)]
      (ev/listen! (dom/by-id (str x "_" y))
                  :click
                  (fn [event]
                    (POST "/ajax-click"
                          {:params {:x x
                                    :y y}
                           :handler (fn [response]
                                      (log response)
                                      (update-thar-board result))})))
      (ev/listen! (dom/by-id (str x "_" y))
                  :contextmenu
                  (fn [event]
                    ;; TODO
                    ;; (remote-callback :right-click
                    ;;                  [x y]
                    ;;                  (fn [result]
                    ;;                    (update-thar-board result)))
                    (ev/prevent-default event)))))

(defn update-thar-board [board]
  (doseq [x (range 10)
          y (range 10)]
    (dom/set-attr! (dom/by-id (str x "_" y)) :src (str "images/" (board [x y]) ".png"))))

(defn ^:export init []
;;  (create-board nil)
  (ev/listen! (dom/by-id "new-game")
              :click
              (fn [event]
                (POST "/ajax-new-board "
                      {:params {}
                       :handler (fn [response]
                                  (log response)
                                  (create-board board))}))))

(defn do-stuff []
  (init))

(set! (.-onload js/window) do-stuff)
