(defproject swinemeeper "0.11.2-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "1.1.8"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [domina "1.0.2"]
                 [ring-transit "0.1.2"]

                 [org.clojure/clojurescript "0.0-2227"]
                 [hiccups "0.3.0"]
                 [cljs-ajax "0.3.3"]


]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.11"]
            [cider/cider-nrepl "0.6.0"]]

  :source-paths ["src"]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds {:dev
                       {:incremental false
                        :source-paths ["cljs_src"]
                        :jar true
                        :compiler {:output-to "resources/public/js/script.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}}}
  :min-lein-version "2.0.0"
  :ring {:handler swinemeeper.routes/app
         :uberwar-name "swinemeeper.war"}
  :main swinemeeper.routes
  :aot :all
  :uberjar-name "swinemeeper.jar"
)

