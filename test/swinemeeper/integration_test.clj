(ns swinemeeper.integration-test
  (:use [etaoin.api]
        [clojure.test])
  (:require [etaoin.keys :as k]
            [swinemeeper.routes :as sm]))

(def ^:dynamic
  *driver*)
(def ^:dynamic
  *server*)

(defn with-server-fixture [f]
  (let [server (sm/make-server)]
    (binding [*server* server]
      (f)
      (.stop *server*))))

(defn webdriver-fixture
  "Executes a test running a driver. Bounds a driver
   with the global *driver* variable."
  [f]
  (with-phantom {} driver
    (binding [*driver* driver]
      (f))))

(use-fixtures :each webdriver-fixture with-server-fixture)

(deftest smoke-test
  "Ensure that the game can at least start"
  (go *driver* "http://localhost:8000")
  (wait-visible *driver* {:id "game-state" :fn/text "Pregame"})
  (click *driver* {:id "5_5"})
  (wait-visible *driver* {:id "game-state" :fn/text "Game Playing"})
  (wait-visible *driver* {:id "swines-remaining" :fn/text "10"})
  (is true))
