(ns swinemeeper.integration-test
  (:use [etaoin.api]
        [clojure.test])
  (:require [etaoin.keys :as k]
            [swinemeeper.routes :as sm]))


(def ^:dynamic
  *driver*)
(def ^:dynamic
  *server*)

(defn fixture-driver
  "Executes a test running a driver. Bounds a driver
   with the global *driver* variable."
  [f]
  (let [server (sm/make-server)]
    (with-phantom {} driver
      (binding [*driver* driver]
        (f)))
    (.stop server)))

(use-fixtures
  :each ;; start and stop driver for each test
  fixture-driver)

(deftest smoke-test
  "Ensure that the game can at least start"
  (go *driver* "http://localhost:8000")
  (wait 3)
  (is (=
       (get-element-text *driver* {:id "game-state"})
       "Pregame"))
  (click *driver* {:id "5_5"})
  (wait 1)
  (is (=
       (get-element-text *driver* {:id "game-state"})
       "Game Playing"))
  (is (=
       (get-element-text *driver* {:id "swines-remaining"})
       "10")))
