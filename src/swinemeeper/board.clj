(ns swinemeeper.board)

;; A swinefield is a map from coor pair to boolean "is it a swine or not?"
;; :width
;; :height


(defn- square-str [sq]
  (str (condp = sq
	 :swine   "X"
         :unknown "?"
         :marked  "!"
	 nil      "."
	 sq)))

(defn iterate-board [board]
  (for [x (range (:width board))
        y (range (:height board))]
    [x y]))

(defn print-swinefield [swinefield]
  (doseq [y (range (:height swinefield))]
    (doseq [x (range (:width swinefield))]
      (print (square-str (swinefield [x y]))))
    (println)))

(defn adjacent? [ [x1 y1] [x2 y2]]
  (and
   (<= (Math/abs (- x1 x2)) 1)
   (<= (Math/abs (- y1 y2)) 1)))

(defn make-swines [width height num-swines exclude-square]
  (into
   (apply hash-map
          (interleave
           (take num-swines
                 (shuffle (for [square
                                (for [x (range width)
                                      y (range height)]
                                  [x y])
                                :when (not
                                       (adjacent? square exclude-square))]
                            square)))
           (repeat :swine)))
   {:width width :height height}))

(defn make-neighbours [width height]
  (memoize
   (fn [[ x y]]
     (filter
       (fn [ [x y] ] (and (>= x 0)
			  (< x width)
			  (>= y 0)
			  (< y height )))
       [[(- x 1) (- y 1)]
	[ x (- y 1)]
	[ (+ x 1) (- y 1)]
	[ (- x 1) y]
	[ (+ x 1) y]
	[ (- x 1) (+ y 1)]
	[ x (+ y 1)]
	[ (+ x 1) (+ y 1) ]]))))

;; TODO sort this out
(def neighbours (atom (make-neighbours 15 10)))

(defn is-swine [swines pos]
  (= (swines pos) :swine))

(defn num-surrounding [swines pos]
  (count (filter #(is-swine swines %) (@neighbours pos))))

(defn try-square [swines pos]
  (if (is-swine swines pos)
    :swine
    (num-surrounding swines pos)))

;; A board is a map from coord pair to either
;; :swine
;; :unknown
;; number - of swine neighbours
;;
;; also has the keys :width :height
;; :board <-- that's a reference to the swinefield... dunno if we will need this
;; ultimately... alternative is to pass in a function that we call when we want to
;; see if something is a swine
;; :width
;; :height
;; :num-swines
;; :state           <-- {:game-playing, :game-won, :game-lost}
;; :remaining-swines

(defn make-board [swines width height]
  (into
   (apply hash-map
          (interleave
           (for [y (range height)
                 x (range width)]
             [x y])
           (repeat :unknown)))
   {
    :swines swines
    :width width
    :height height
    :num-swines (- (count swines) 2) ; HACK
    :state :game-playing
    :remaining-swines (- (count swines) 2)})) ; HACK

(defn print-board [board]
  (doseq [y (range (:height board))]
    (doseq [x (range (:width board))]
      (print (square-str ( board [x y]))))
    (println)))

(defn num-neighbours= [board [x y] value]
  (count (filter #(= % value) (map board (@neighbours [x y])))))

(defn num-marked-neighbours [board [x y]]
  (num-neighbours= board [x y] :marked))

(defn num-unknown-neighbours [board [x y]]
  (num-neighbours= board [x y] :unknown))

(defn countp [board p]
  "Count the number of board squares that match a predicate"
  (count
   (for [square (iterate-board board)
         :when (p (board square))]
     nil)))

(defn count-marked [board]
  (countp board #(= % :marked)))

(defn count-revealed [board]
  (countp board #(number? %)))

(defn count-swines [board]
  (countp board #(or (= % :swine)
                    (= % :exploding-swine))))

;; Board manipulation functions
(defn uncover [board coords]
  (let [ [x y] (first coords)

         ]
    ;; TODO
    )
  )

(defn uncover [board poses]
  (if (empty? poses)
    board
    (let [pos (first poses)
          square (try-square (:swines board) pos)
          new-board (assoc board pos square)
          ; TODO recursively check squares if this is a zero
          new-poses (if (and
                         (= (board pos) :unknown)
                         (= square 0))
                      (concat (rest poses) (@neighbours pos))
                      (rest poses))]
      (recur new-board new-poses))))

(defn mark [board [x y]]
  (condp = (board [x y])
    :unknown (assoc board [x y] :marked)
    :marked  (assoc board [x y] :unknown)
    board))

(defn double-dude [board [x y]]
  ; TODO rename to something sensible
  (let [square (board [x y])]
    (if (number? square)
      (if (= square (num-marked-neighbours board [x y]))
        (uncover board
                 (filter #(= (board %) :unknown)
                         (@neighbours [x y])))
        board)
      board)))

(defn fully-reveal-board-on-lose [board]
  ;; TODO wrongly placed flags
  ;; TODO exploding swine on the place you just clickged
  (let [swines (:swines board)]
    (into board
          (for [[x y] (iterate-board board)]
            [[x y] (try-square swines [x y])]))

    ;; (assoc view :grid (vec (for [y (iterate-height board)]
    ;;     	        (vec (for [x (iterate-width board)]
    ;;     	          (condp = (.square-at view [x y])
    ;;     		      :marked (if (not (is-swine? board [x y]))
    ;;     				:incorrectly-marked
    ;;     				:marked)
    ;;     		      :swine :exploding_swine
    ;;     		      :unknown (if (is-swine? board [x y])
    ;;     				 :swine
    ;;     				 :unknown)
    ;;     		      (try-square board [x y])))))))
    ))

(defn fully-reveal-board-on-win [board]
(let [swines (:swines board)]
    (into board
          (for [[x y] (iterate-board board)]
            [[x y] (try-square swines [x y])]))))

(defn num-swines-unmarked [board]
  (- (:num-swines board) (count-marked board)))

(defn is-game-lost [board]
  (> (count-swines board) 0))

(defn is-game-won [board]
  (= (count-revealed board)
     (- (* (:width board) (:height board)) (:num-swines board))))

(defn new-game-state [board]
  (cond
    (is-game-won board)  :game-won
    (is-game-lost board) :game-lost
    :else :game-playing))

(defn check-for-endgame [board]
  "Checks for the end of the game and updates game state."
  (let [new-state (new-game-state board)]
    (condp = new-state
      :game-won (assoc (fully-reveal-board-on-win board)
                  :state new-state)
      :game-lost (assoc (fully-reveal-board-on-lose board)
                   :state new-state)
      (assoc board :state new-state))))
