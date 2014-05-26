(defproject swinemeeper "0.9"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "1.0.4"]
                 [hiccup "1.0.0"]
                 [hiccups "0.2.0"]
                 [ring/ring-jetty-adapter "0.2.3"]
                 [domina "1.0.2-SNAPSHOT"]
                 [cljs-ajax "0.2.3"]
                 [fogus/ring-edn "0.2.0"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-ring "0.7.0"]]
  :source-paths ["src"]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {
              :repl-listen-port 9000

              :repl-launch-commands
              {"firefox" ["firefox"
                          :stdout ".repl-firefox-out"
                          :stderr ".repl-firefox-err"]
               ;; Launch command for interacting with your ClojureScript at a REPL,
               ;; without browsing to the app (a static HTML file is used).
               ;;     $ lein trampoline cljsbuild repl-launch firefox-naked
               "firefox-naked" ["firefox"
                                "resources/private/html/naked.html"
                                :stdout ".repl-firefox-naked-out"
                                :stderr ".repl-firefox-naked-err"]
               "google-chrome" ["google-chrome"
                                :stdout ".repl-google-chrome-out"
                                :stderr ".repl-google-chrome-err"]


               }

              :builds {
                       :dev
                       {
                        :incremental false
                        ;; CLJS source code path
                        :source-paths ["cljs_src"]

                        :jar true
                        ;; Google Closure (CLS) options configuration
                        :compiler {;; CLS generated JS script filename
                                   :output-to "resources/public/js/script.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}}

              }
  :ring {:handler swinemeeper.core/app}
  :main swinemeeper.core)
