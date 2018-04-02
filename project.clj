(defproject swinemeeper "0.11.2-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [domina "1.0.3"]
                 [ring-transit "0.1.6"]
                 [etaoin "0.2.8-SNAPSHOT"]


                 [org.clojure/clojurescript "1.10.238"]
                 [hiccups "0.3.0"]
                 [cljs-ajax "0.7.3"]]
  :plugins [[lein-cljsbuild "1.1.7"]
            [com.cemerick/clojurescript.test "0.3.3"]
            [lein-ring "0.12.4"]
            [cider/cider-nrepl "0.16.0"]]

  :source-paths ["src"]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds {:dev
                       {:incremental false
                        :source-paths ["cljs_src"]
                        :jar true
                        :compiler {:output-to "resources/public/js/script.js"
                                   :optimizations :simple
                                   :pretty-print true}}}}
  :min-lein-version "2.0.0"
  :ring {:handler swinemeeper.routes/app
         :uberwar-name "swinemeeper.war"}
  :main swinemeeper.routes
  :aot :all
  :uberjar-name "swinemeeper.jar")
