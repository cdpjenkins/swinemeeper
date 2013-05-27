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

(defn is-swine [board pos]
  (= (board pos) :swine))

(defn num-surrounding [board pos]
  (count (filter #(is-swine board %) (@neighbours pos))))

(defn try-square [board pos]
  (if (is-swine board pos)
    :swine
    (num-surrounding board pos)))

;; A view is a map from coord pair to either
;; :swine
;; :unknown
;; number - of swine neighbours
;;
;; also has the keys :width :height
;; :swinefield <-- that's a reference to the swinefield... dunno if we will need this
;; ultimately... alternative is to pass in a function that we call when we want to
;; see if something is a swine
(defn make-view [swines]
  (into
   (apply hash-map
          (interleave
           (for [y (range (:height swines))
                 x (range (:width swines))]
             [x y])
           (repeat :unknown)))
   {:swines swines}))

(defn print-view [view]
  (doseq [y (range (:height (:swines view)))]
    (doseq [x (range (:width (:swines view)))]
      (print (square-str ( view [x y]))))
    (println)))

(defn num-neighbours= [view [x y] value]
  (count (filter #(= % value) (map view (@neighbours [x y])))))

(defn num-marked-neighbours [view [x y]]
  (num-neighbours= view [x y] :marked))

(defn num-unknown-neighbours [view [x y]]
  (num-neighbours= view [x y] :unknown))

(defn countp [view p]
  "Count the number of view squares that match a predicate"
  (count
   (for [square (iterate-board (:swines view))
         :when (p (view square))]
     nil)))

(defn count-marked [view]
  (countp view #(= % :marked)))

(defn count-revealed [view]
  (countp view #(number? %)))

(defn count-swines [view]
  (countp view #(= % :swine)))

;; View manipulation functions
(defn uncover [view coords]
  (let [ [x y] (first coords)

         ]
    ;; TODO
    )
  )

(defn uncover [view poses]
  (if (empty? poses)
    view
    (let [pos (first poses)
          square (try-square (:swines view) pos)
          new-view (assoc view pos square)
          ; TODO recursively check squares if this is a zero
          new-poses (if (and
                         (= (view pos) :unknown)
                         (= square 0))
                      (concat (rest poses) (@neighbours pos))
                      (rest poses))]
      (recur new-view new-poses))))

(defn mark [view [x y]]
  (condp = (view [x y])
    :unknown (assoc view [x y] :marked)
    :marked  (assoc view [x y] :unknown)
    view))

(defn double-dude [view [x y]]
  ; TODO rename to something sensible
  (let [square (view [x y])]
    (if (number? square)
      (if (= square (num-marked-neighbours view [x y]))
        (uncover view
                 (filter #(= (view %) :unknown)
                         (@neighbours [x y])))
        view)
      view)))

(defn fully-reveal-board-on-lose [view]
  ;; TODO wrongly placed flags
  ;; TODO exploding swine on the place you just clicked
  (let [board (:swines view)]
    (into view
          (for [[x y] (iterate-board board)]
            [[x y] (try-square board [x y])]))

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

(defn fully-reveal-board-on-win [view]
(let [board (:swines view)]
    (into view
          (for [[x y] (iterate-board board)]
            [[x y] (try-square board [x y])]))))
