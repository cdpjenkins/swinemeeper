(ns view
  (:use board))

(defprotocol IViewModel
  "A View onto a Swinemeeper Board"
  (square-at [this [x y] ])
  (assoc-square [this [x y] value]))

(defrecord CViewModel [width height grid board]
  IViewModel
  (square-at [ this [x y] ]
    (( grid y) x))
  (assoc-square [this [x y] value]
    (assoc this :width width
	        :height height
		:grid (assoc-in grid [y x] value)
		:board board))

  Object
  (toString [this] (str "width: " width " height: " height)))

(defn make-empty-view [width height board]
  (CViewModel. width
	  height
	  (vec (for [y (range height)]
	    (vec (for [x (range width)]
              :unknown))))
	  board))

(defn num-neighbours= [view [x y] value]
  (count (filter #(= value %)
		 (map #(.square-at view %)
		      (@neighbours x y)))))

(defn num-marked-neighbours [view [x y]]
  (num-neighbours= view [x y] :marked))

(defn num-unknown-neighbours [view [x y]]
  (num-neighbours= view [x y] :unknown))

(defn view-square-str [view x y]
  (square-str (.square-at view [x y])))

(defn print-view [view]
  (doseq [y (iterate-height (.board view))]
    (doseq [x (iterate-width (.board view))]
      (print (view-square-str view x y)))
    (println)))

(defn countp [view p]
  "Count the number of view squares that match a predicate"
  (count
   (for [square (iterate-board (.board view))
         :when (p (.square-at view square))]
     nil)))

(defn count-marked [view]
  (countp view #(= % :marked)))

(defn count-revealed [view]
  (countp view #(number? %)))

(defn count-swines [view]
  (countp view #(= % :swine)))

;; View manipulation fns

(defn uncover [view coords]
  (if (= coords [])
    view
    (let [[x y] (first coords)
 	  square (try-square (.board view) [x y])
	  new-view (.assoc-square view [x y] square)
	  new-coords (if (and
			  (= (.square-at view [x y]) :unknown)
			  (= square 0))
		       (concat (rest coords) (@neighbours x y))
		       (rest coords))]
      (recur new-view new-coords))))

(defn mark [view [x y]]
  (condp = (.square-at view [x y])
    :unknown (assoc-square view [x y] :marked)
    :marked  (assoc-square view [x y] :unknown)
    view))

(defn double-dude [view [x y]]
  ; TODO rename to something sensible
  (let [square (.square-at view [x y])]
    (if (number? square)
      (if (= square (num-marked-neighbours view [x y]))
        (uncover view
                 (filter #(= (.square-at view %) :unknown)
                         (@neighbours x y)))
        view)
      view)))

(defn fully-reveal-board-on-lose [view]
  (let [board (.board view)]
    (assoc view :grid (vec (for [y (iterate-height board)]
  		        (vec (for [x (iterate-width board)]
		          (condp = (.square-at view [x y])
			      :marked (if (not (is-swine? board [x y]))
					:incorrectly-marked
					:marked)
			      :swine :exploding_swine
			      :unknown (if (is-swine? board [x y])
					 :swine
					 :unknown)
			      (try-square board [x y])))))))))

(defn fully-reveal-board-on-win [view]
  (let [board (.board view)]
    (assoc view :grid (vec (for [y (iterate-height board)]
	                (vec (for [x (iterate-width board)]
			  (let [square (try-square board [x y])]
			    (if (= square :swine) :marked square)))))))))
