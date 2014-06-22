(ns swinemeeper.board-test
  (:use clojure.test
        swinemeeper.board))

(def board-2x2
  (let [swines {[0 0] :swine}]
    (-> (make-board 1 2 2)
        (assoc :swines swines)
        (assoc :state :game-playing))))

(deftest test-initial-state-pregame
  (is (= (:state (make-board 10 10 10)) :pregame)))

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
          :swines (:swines board-2x2)})))

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

(deftest sanitise-2x2
  (is (= (sanitise-board board-2x2)
         {[0 0]   :unknown
          [0 1]   :unknown
          [1 0]   :unknown
          [1 1]   :unknown
          :width  2
          :height 2
          :state  :game-playing
          :num-swines 1
          :remaining-swines 1})))

(deftest test-double-dude-2x2
  (let [board (-> board-2x2
                  (uncover [[1 0]])
                  (mark [0 0]))]
    (is (= (double-dude board [1 0])
         {[0 0]   :marked
          [0 1]   1
          [1 0]   1
          [1 1]   1
          :width  2
          :height 2
          :state  :game-won
          :num-swines 1
          :remaining-swines 0
          :swines (:swines board-2x2)}))))
