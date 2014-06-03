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

(defn- iterate-board [board]
  (for [x (range (:width board))
        y (range (:height board))]
    [x y]))

(defn- print-swinefield [swinefield]
  (doseq [y (range (:height swinefield))]
    (doseq [x (range (:width swinefield))]
      (print (square-str (swinefield [x y]))))
    (println)))

(defn- adjacent? [ [x1 y1] [x2 y2]]
  (and
   (<= (Math/abs (- x1 x2)) 1)
   (<= (Math/abs (- y1 y2)) 1)))

(defn make-swines [width height num-swines exclude-square]
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
          (repeat :swine))))

(defn- make-neighbours [width height]
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
;; TODO - cos - this limits the board to being no more than 100x100
(def neighbours (atom (make-neighbours 100 100)))

(defn- is-swine [swines pos]
  (= (swines pos) :swine))

(defn- num-surrounding [swines pos]
  (count (filter #(is-swine swines %) (@neighbours pos))))

(defn- try-square [swines pos]
  (if (is-swine swines pos)
    :exploding-swine
    (num-surrounding swines pos)))

(defn- reveal-square-on-win [swines pos]
  (if (is-swine swines pos)
    :marked
    (num-surrounding swines pos)))

(defn- reveal-square-on-lose [swines pos board]
  (if (is-swine swines pos)
    (condp = (board pos)
      :exploding-swine :exploding-swine
      :marked :marked
      :swine)
    (if (= (board pos) :marked)
      :incorrectly-marked
      (board pos))))

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
   {:swines swines
    :width width
    :height height
    :num-swines (count swines)
    :state :game-playing
    :remaining-swines (count swines)}))

(defn- print-board [board]
  (doseq [y (range (:height board))]
    (doseq [x (range (:width board))]
      (print (square-str ( board [x y]))))
    (println)))

(defn- num-neighbours= [board [x y] value]
  "Returns the number of squares adjacent to [x y] that have the
  specified value"
  (count (filter #(= % value) (map board (@neighbours [x y])))))

(defn- num-marked-neighbours [board [x y]]
  (num-neighbours= board [x y] :marked))

(defn- num-unknown-neighbours [board [x y]]
  (num-neighbours= board [x y] :unknown))

(defn- countp [board p]
  "Count the number of board squares that match a predicate"
  (count
   (for [square (iterate-board board)
         :when (p (board square))]
     nil)))

(defn- count-marked [board]
  (countp board #(= % :marked)))

(defn- count-revealed [board]
  (countp board #(number? %)))

(defn- count-swines [board]
  (countp board #(or (= % :swine)
                    (= % :exploding-swine))))

;; Board manipulation functions
(defn- fully-reveal-board-on-lose [board]
  ;; TODO wrongly placed flags
  ;; TODO exploding swine on the place you just clickged
  (let [swines (:swines board)]
    (into board
          (for [[x y] (iterate-board board)]
            [[x y] (reveal-square-on-lose swines [x y] board)]))))

(defn- fully-reveal-board-on-win [board]
  (let [swines (:swines board)]
    (assoc 
        (into board
              (for [[x y] (iterate-board board)]
                [[x y] (reveal-square-on-win swines [x y])]))
      :remaining-swines 0)))

(defn- num-swines-unmarked [board]
  (- (:num-swines board) (count-marked board)))

(defn- is-game-lost [board]
  (> (count-swines board) 0))

(defn- is-game-won [board]
  (= (count-revealed board)
     (- (* (:width board) (:height board)) (:num-swines board))))

(defn- new-game-state [board]
  (cond
    (is-game-won board)  :game-won
    (is-game-lost board) :game-lost
    :else :game-playing))

(defn- check-for-endgame [board]
  "Checks for the end of the game and updates game state."
  (let [new-state (new-game-state board)]
    (condp = new-state
      :game-won (assoc (fully-reveal-board-on-win board)
                  :state new-state)
      :game-lost (assoc (fully-reveal-board-on-lose board)
                   :state new-state)
      (assoc board :state new-state))))

(defn sanitise-board
  "Removes :swines from the board - so the client can't cheat!"
  [board]
  (dissoc board :swines))

(defn uncover [board poses]
  (when (= (:state board) :game-playing)
    (if (empty? poses)
      (check-for-endgame board)
      (let [pos (first poses)
            square (try-square (:swines board) pos)
            new-board (assoc board pos square)
            new-poses (if (and
                           (= (board pos) :unknown)
                           (= square 0))
                        (concat (rest poses) (@neighbours pos))
                        (rest poses))]
        (recur new-board new-poses)))))

(defn double-dude [board [x y]]
  (when (= (:state board) :game-playing)
    (let [square (board [x y])]
      (if (number? square)
        (if (= square (num-marked-neighbours board [x y]))
          (uncover board
                   (filter #(= (board %) :unknown)
                           (@neighbours [x y])))
          board)
        board))))

(defn mark [board [x y]]
  (when (= (:state board) :game-playing)
    (cond 
     (= (board [x y]) :unknown) (let [board (assoc board [x y] :marked)]
                                  (assoc board :remaining-swines (num-swines-unmarked board)))

     (= (board [x y]) :marked) (let [board (assoc board [x y] :unknown)]
                                 (assoc board :remaining-swines (num-swines-unmarked board)))
     (number? (board [x y])) (double-dude board [x y])
     true board)))
