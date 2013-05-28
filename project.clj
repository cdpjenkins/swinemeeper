(defproject swinemeeper "0.9"
    :dependencies [[org.clojure/clojure "1.5.1"]
                   [org.clojure/clojure-contrib "1.2.0"]
                   [compojure "1.0.4"]
                   [hiccup "1.0.0"]
;                   [ring/ring-core "0.2.3"]
                   [ring/ring-jetty-adapter "0.2.3"]
                   ]

    :plugins [[lein-cljsbuild "0.3.2"]
              [lein-ring "0.7.0"]]

    :source-paths ["src"]


    ;; cljsbuild options configuration
    :cljsbuild {:builds
                [{;; CLJS source code path
                  :source-paths ["cljs_src"]

                  ;; Google Closure (CLS) options configuration
                  :compiler {;; CLS generated JS script filename
                             :output-to "resources/public/js/script.js"

                             ;; minimal JS optimization directive
                             :optimizations :whitespace

                             ;; generated JS code prettyfication
                             :pretty-print true}}]}

    :ring {:handler swinemeeper.core/app}


;    :main swinemeeper.core
    )
