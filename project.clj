(defproject swinemeeper "0.10"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "1.1.8"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [domina "1.0.2"]
                 [fogus/ring-edn "0.2.0"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [org.clojars.cdpjenkins/lein-ring "0.8.11-SNAPSHOT"]
            [cider/cider-nrepl "0.6.0"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-2227"]
                                  [hiccups "0.3.0"]
                                  [cljs-ajax "0.2.4"]
]}}

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
  :ring {:handler swinemeeper.core/app
         :servlet-version "2.5"
         :uberwar-name "swinemeeper.war"}
  :main swinemeeper.core
  :uberjar-name "swinemeeper.jar"
)

