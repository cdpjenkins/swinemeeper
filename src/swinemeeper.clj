;; TODO number of swines remaining
;; TODO let you know when game ends, either in success or failure
;; TODO don't let you hit a swine on first go
;; TODO configure dimensions + number of swines
;; TODO move click callback functions into top level functions of their own

;; TODO turn the view into a defstruct that includes the view itself and
;;      also board, num-swines, game-state (:pre-game, :playing, :lost, :won)
;; TODO timer
;; TODO high score table
;; TODO turn it into an applet

(ns swinemeeper
  (:use clojure.contrib.seq-utils)
  (:gen-class))

(import
 '(java.awt Color Dimension GridLayout)
 '(java.awt.event ActionListener MouseAdapter MouseEvent)
 '(java.awt.image BufferedImage)
 '(java.io File)
 '(javax.imageio ImageIO)
 '(javax.swing JFrame JButton JPanel))

;; Constants

(def width 12)
(def height 12)
(def square-width 36)
(def square-height 36)
(def num-swines 15)

;; Mappings screen-coords <--> board-coords
(defn screen-to-board [ [x y] ]
  [ (int (/ x square-width)) (int (/ y square-height)) ] )

(defn board-to-screen [ [x y] ]
  [ (* x square-width) (* y square-height) ])

;; Board functions

(defn make-swines [width height num-swines & [ [ex ey]] ]
  (set
   (take num-swines
	 (shuffle (for [x (range width) y (range height)] [x y])))))

(def swines (make-swines width height num-swines))

(defn is-swine? [pos]
  (contains? swines pos))

(defn neighbours [x y]
  (filter
   (fn [ [x y] ] (and (>= x 0)
		      (< x width)
		      (>= y 0)
		      (< y height)))
   [[(- x 1) (- y 1)]
    [ x (- y 1)]
    [ (+ x 1) (- y 1)]
    [ (- x 1) y]
    [ (+ x 1) y]
    [ (- x 1) (+ y 1)]
    [ x (+ y 1)]
    [ (+ x 1) (+ y 1) ]]))

(defn num-surrounding [x y]
  (count (filter is-swine? (neighbours x y))))

(defn try-square [x y]
  (if (is-swine? [x y])
    :swine
    (num-surrounding x y)))

(declare square-str)

(defn print-board []
  (doseq [y (range height)]
    (doseq [x (range width)]
      (print (square-str (try-square x y))))
    (println)))

;; View functions

(defn make-empty-view []
  (vec (for [y (range height)]
    (vec (for [x (range width)]
      :unknown)))))

(def view-atom (atom (make-empty-view)))

(defn view-square-at [view [x y]]
  ; TODO I really want to implement view as a map instead in the future
  ((view y) x))

(defn num-neighbours= [view [x y] value]
  (count (filter #(= value %)
		 (map #(view-square-at view %)
		      (neighbours x y)))))

(defn num-marked-neighbours [view [x y]]
  (num-neighbours= view [x y] :marked))

(defn num-unknown-neighbours [view [x y]]
  (num-neighbours= view [x y] :unknown))

(defn square-str [sq]
  (str (condp = sq
	 :swine   "X"
	 :unknown "."
	 sq)))

(defn view-square-str [view x y]
  (square-str ((view y) x)))

(defn print-view [view]
  (doseq [y (range height)]
    (doseq [x (range width)]
      (print (view-square-str view x y)))
    (println)))

; View (atom) manipulation functions

(defn uncover [view coords]
  (if (= coords [])
    view
    (let [[x y] (first coords)
	  square (try-square x y)
	  new-view (assoc-in view [y x] square)
	  new-coords (if (and
			  (= ((view y) x) :unknown)
			  (= square 0))
		       (concat (rest coords) (neighbours x y))
		       (rest coords))]
      (recur new-view new-coords))))

(defn mark [view [x y]]
  (condp = ((view y) x)
    :unknown (assoc-in view [y x] :marked)
    :marked  (assoc-in view [y x] :unknown)
    view))

(defn mark-list [view coords]
  (if (= coords [])
    view
    (recur (mark view (first coords)) (rest coords))))
  

(defn double-dude [view [x y]]
  ; TODO rename to something sensible
  (let [square (view-square-at view [x y])]
    (if (number? square)
      (if (= square (num-marked-neighbours view [x y]))
	(uncover view
		 (filter #(= (view-square-at view %) :unknown)
			 (neighbours x y)))
	(if (= square
	       (+ (num-unknown-neighbours view [x y])
		  (num-marked-neighbours view [x y])))
	  (mark-list view (filter #(= (view-square-at view %) :unknown)
				  (neighbours x y)))
	  view))
      view)))

;; GUI stuff

;  GUI event handlers

(defn left-click [coords]
  (swap! view-atom uncover [coords]))

(defn double-click [coords]
  (swap! view-atom double-dude coords))

(defn right-click [coords]
  (swap! view-atom mark coords))

(defn make-action-listener [f]
  (proxy [ActionListener] []
    (actionPerformed [e] (f e))))

(defn load-image [filename]
;  (ImageIO/read(File. filename)))
  (ImageIO/read (ClassLoader/getSystemResource filename)))

(defn load-images []
  {:unknown (load-image "unknown.png")
   :swine (load-image "swine.png")
   :marked (load-image "marked.png")
   0 (load-image "0.png")
   1 (load-image "1.png")
   2 (load-image "2.png")
   3 (load-image "3.png")
   4 (load-image "4.png")
   5 (load-image "5.png")
   6 (load-image "6.png")
   7 (load-image "7.png")
   8 (load-image "8.png")})

(defn paint-square [g x y panel view images]
  (let [sx (* x square-width)
	sy (* y square-height)
	square ((view y) x)]
    (.drawImage g (images square) sx sy square-width square-height 
		Color/BLACK nil)))

(defn make-board-panel []
  (let [pointless-panel (JPanel.)
        images (load-images)
	panel (proxy [JPanel] []
		(getPreferredSize [] (Dimension. (* width square-width)
						 (* height square-height)))
		(paintComponent [g]
		  (doseq [y (range height)
			  x (range width)]
		    (paint-square g x y pointless-panel @view-atom images))))]
    (doto panel
      (.addMouseListener
       (proxy [MouseAdapter] []
	 (mouseClicked [e]
	   (let [coords (screen-to-board [ (.getX e)
					   (.getY e)]) 
		 button (.getButton e)]
	     (condp = button
	       MouseEvent/BUTTON1
	         (condp = (.getClickCount e)
		   1 (left-click coords)
		   2 (double-click coords)
		   nil)
	       MouseEvent/BUTTON3 (right-click coords))
	     ; TODO get rid of the repain and replace with add-watch
	     (.repaint panel))))))

    panel))

(defn make-frame []
  (let [frame (JFrame. "Swine Meeper")]
    (doto (.getContentPane frame)
      (.add (make-board-panel)))
    (doto frame
      (.pack)
      (.show))))

(defn -main []
  (println "Totaly in the main method")
  (make-frame))

;(-main)
