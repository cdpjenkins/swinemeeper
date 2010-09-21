(ns board-test
  (:use [clojure.test]
	[board CBoardModel]))

(def board (CBoardModel. 10 10 {[5 5] [5 6] [5 7]
			        [6 5]       [6 7]
			        [7 5] [7 6] [7 7]}))

(deftest huss
  (is true))
