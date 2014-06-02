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

(def board-2x2
  (let [swines {[0 0] :swine}]
    (make-board swines 2 2)))

(deftest test-uncover-bad-2x2
  (is (= (uncover board-2x2 [[0 0]])
         {[0 0]   :exploding-swine
          [0 1]   :unknown
          [1 0]   :unknown
          [1 1]   :unknown
          :width  2
          :height 2
          :state  :game-lost
          :num-swines 1
          :remaining-swines 1
          :swines (:swines board-2x2)})))

(deftest test-uncover-good-2x2
  (is (= (uncover board-2x2 [[1 1]])
           {[0 0]   :unknown
            [0 1]   :unknown
            [1 0]   :unknown
            [1 1]   1
            :width  2
            :height 2
            :state  :game-playing
            :num-swines 1
            :remaining-swines 1
            :swines (:swines board-2x2)})))

(deftest test-mark-good-2x2
  (is (= (mark board-2x2 [0 0])
         {[0 0]   :marked
          [0 1]   :unknown
          [1 0]   :unknown
          [1 1]   :unknown
          :width  2
          :height 2
          :state  :game-playing
          :num-swines 1
          :remaining-swines 0
          :swines (:swines board-2x2)}
)))

(deftest test-win-2x2
  (is (= (uncover board-2x2 [[1 0] [0 1] [1 1]])
         {[0 0]   :marked
          [0 1]   1
          [1 0]   1
          [1 1]   1
          :width  2
          :height 2
          :state  :game-won
          :num-swines 1
          :remaining-swines 0
          :swines (:swines board-2x2)})))
