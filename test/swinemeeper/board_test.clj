(ns swinemeeper.board-test
  (:use clojure.test
        swinemeeper.board))

(deftest test-adjacent
  (testing "Ston"
    (is (adjacent? [0 0] [1 1]))
    (is (not (adjacent? [0 0] [2 2])))))

(deftest test-make-swines
  (let [swines (make-swines 10 15 2 [5 5])]
    (testing "make-swines"
      (is (:width swines) 10)
      (is (:height swines) 15))))
