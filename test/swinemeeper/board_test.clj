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
  (let [swines {[0 0] :swine :width 2 :height 2}]
    (make-board swines 2 2)))

(deftest test-make-board-2x2
  (testing "2x2 board with one swine"
    (let [board-lose (fully-reveal-board-on-lose board-2x2)
          board-win  (fully-reveal-board-on-win  board-2x2)]
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
              :swines (:swines board-2x2)}))
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
              :swines (:swines board-2x2)})))))

(deftest test-fully-reveal-2x2
  (is (= (fully-reveal-board-on-lose board-2x2)
         {[0 0]   :exploding-swine
          [0 1]   :unknown
          [1 0]   :unknown
          [1 1]   :unknown
          :width  2
          :height 2
          :state  :game-playing ; TODO because this fn doesn't set game state
          :num-swines 1
          :remaining-swines 1   ; TODO because this fn doesn't set remaining-swines either
          :swines (:swines board-2x2)})))
