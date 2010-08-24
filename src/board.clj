(ns board)

(declare neighbours square-str)

(defprotocol IBoardModel
  "A Swinemeeper Board"
  (is-swine? [ this [x y] ])
  )

(defrecord CBoardModel [width height swines]
  IBoardModel
  (is-swine? [this [x y]]
    (contains? swines [x y]))
  Object
  (toString [this] (str "width: " width " height: " height)))


(defn iterate-width [board]
  (range (.width board)))  
(defn iterate-height [board]
  (range (.height board)))
(defn iterate-board [board]
  (for [y (iterate-height board)
	x (iterate-width board)]
    [x y]))
(defn num-surrounding [board [x y]]
  (count (filter #(.is-swine? board %) (@neighbours x y))))

(defn try-square [board [x y]]
  (if (.is-swine? board [x y])
    :swine
    (num-surrounding board [x y])))

(defn print-board [board]
  (doseq [y (iterate-height board)]
    (doseq [x (iterate-width board)]
      (print (square-str (try-square board [x y]))))
    (println)))

(defn squares-are-adjacent [ [x1 y1] [x2 y2] ]
  (and
   (< (Math/abs (- x1 x2)) 3)
   (< (Math/abs (- y1 y2)) 3)))

(defn make-swines [width height num-swines exclude-square ]
  (set
   (take num-swines
	 (shuffle (for [square (for [x (range width)
				     y (range height)]
				 [x y])
                        :when (not
                               (squares-are-adjacent square exclude-square))]
                    square)))))

(defn make-board [width height num-swines exclude-square]
  (CBoardModel. width height (make-swines width height num-swines exclude-square)))

(defn make-neighbours [width height]
  (memoize
   (fn [x y]
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

; TODO start off with width, height == 10, 10. Maybe need to sort out a better
; default
(def neighbours (atom (make-neighbours 10 10)))

(defn square-str [sq]
  (str (condp = sq
	 :swine   "X"
	 :unknown "."
	 sq)))
