(ns swinemeeper.board-test
  (:use clojure.test
        swinemeeper.board))

(def board-2x2
  (let [swines {[0 0] :swine}]
    (-> (make-board 1 2 2 "Custom")
        (assoc :swines swines)
        (assoc :state :game-playing))))

(deftest test-initial-board-2x2
  (is (= (:type board-2x2) "Custom"))
  (is (= (board-2x2 [0 0]) :unknown))
  (is (= (board-2x2 [0 1]) :unknown))
  (is (= (board-2x2 [1 0]) :unknown))
  (is (= (board-2x2 [1 1]) :unknown)))

(deftest test-initial-state-pregame
  (let [width 7
        height 8
        num-swines 10
        board (make-board num-swines width height "Cheese")]
    (is (= (:state board) :pregame))
    (is (= (:type board) "Cheese"))
    (is (= (:num-swines board) num-swines))
    (is (= (:remaining-swines board) num-swines))
    (is (= (:width board) width))
    (is (= (:height board) height))
    (doseq [x (range width)
            y (range height)]
      (is (= (:unknown (board [x y])))))))

(deftest test-uncover-bad-2x2
  (is (= (uncover board-2x2 [[0 0]])
         (-> board-2x2
             (assoc [0 0] :exploding-swine)
             (assoc :state :game-lost)))))

(deftest test-uncover-good-2x2
  (is (= (uncover board-2x2 [[1 1]])
         (-> board-2x2
             (assoc [1 1] 1)))))

(deftest test-mark-good-2x2
  (is (= (mark board-2x2 [0 0])
         (-> board-2x2
             (assoc [0 0] :marked)
             (assoc :remaining-swines 0)))))

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
          :swines (:swines board-2x2)
          :type "Custom"})))

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
          :remaining-swines 1
          :type "Custom"})))

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
          :swines (:swines board-2x2)
          :type "Custom"}))))
