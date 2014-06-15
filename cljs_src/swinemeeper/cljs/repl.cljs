(ns swinemeeper.cljs.repl
  (:require
    [clojure.browser.repl :as repl]))

(defn ^:export connect []
  (.log js/console "Starting browser repl...")
  (repl/connect "http://localhost:9000/repl"))
