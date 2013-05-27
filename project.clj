(defproject swinemeeper "0.9"
    :dependencies [[org.clojure/clojure "1.5.1"]
                   [org.clojure/clojure-contrib "1.2.0"]
                   [compojure "0.4.0"]
                   [hiccup "1.0.2"]
                   [ring/ring-core "0.2.3"]
                   [ring/ring-jetty-adapter "0.2.3"]]

    :dev-dependencies [[swank-clojure "1.2.1"]]

    :main swinemeeper.core)
