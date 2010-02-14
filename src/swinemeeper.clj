;; TODO configure dimensions + number of swines by popping up some kind of
;;      dialog or something
;; TODO timer
;; TODO high score table
;; TODO turn it into an applet
;; TODO push all the state into game state-struct object

(set! *warn-on-reflection* true)

(ns swinemeeper
  (:use clojure.contrib.seq-utils)
  (:gen-class))

(import
 '(java.awt Color Dimension Graphics GridLayout)
 '(java.awt.event ActionListener MouseAdapter MouseEvent)
 '(java.awt.image BufferedImage)
 '(java.io File)
 '(javax.imageio ImageIO)
 '(javax.swing BoxLayout JButton JFrame JLabel JPanel SwingUtilities))

;
(defstruct state-struct
  :width
  :height
  :square-width
  :square-height
  :num-swines
  :state)
; TODO add the following to state-struct
; - the board
; - the view
; - the GUI? Maybe.

(def width (accessor state-struct :width))
(def height (accessor state-struct :height))
(def square-width (accessor state-struct :square-width))
(def square-height (accessor state-struct :square-height))
(def num-swines (accessor state-struct :num-swines))
(def state (accessor state-struct :state))

(def game (struct state-struct 36               ; width
                               24               ; height
                               32               ; square-width
                               32               ; square-height
                               120              ; num-swines
                               (ref :pregame))) ; game-state
(declare board-ref)
(declare remaining-swines-ref)

(declare square-str)

;; Mappings screen-coords <--> board-coords
(defn screen-to-board [ [x y] ]
  [ (int (/ x (square-width game))) (int (/ y (square-height game))) ] )

(defn board-to-screen [ [x y] ]
  [ (* x (square-width game)) (* y (square-height game)) ])

;; Board functions

(defn iterate-width []
  (range (width game)))

(defn iterate-height []
  (range (height game)))

(defn iterate-board []
  (for [y (iterate-height)
        x (iterate-width)]
    [x y]))

(defn make-swines [width height num-swines & [exclude-square ] ]
  (set
   (take num-swines
	 (shuffle (for [square (iterate-board)
                        :when (not (= square exclude-square))]
                    square)))))

(defn is-swine? [pos]
  (contains? @board-ref pos))

(defn neighbours [x y]
  (filter
   (fn [ [x y] ] (and (>= x 0)
		      (< x (width game))
		      (>= y 0)
		      (< y (height game))))
   [[(- x 1) (- y 1)]
    [ x (- y 1)]
    [ (+ x 1) (- y 1)]
    [ (- x 1) y]
    [ (+ x 1) y]
    [ (- x 1) (+ y 1)]
    [ x (+ y 1)]
    [ (+ x 1) (+ y 1) ]]))

(def neighbours (memoize neighbours))

(defn num-surrounding [x y]
  (count (filter is-swine? (neighbours x y))))


(defn try-square [x y]
  (if (is-swine? [x y])
    :swine
    (num-surrounding x y)))

(defn print-board []
  (doseq [y (iterate-height)]
    (doseq [x (iterate-width)]
      (print (square-str (try-square x y))))
    (println)))

;; View functions

(defn make-empty-view []
  (vec (for [y (iterate-height)]
    (vec (for [x (iterate-width)]
      :unknown)))))

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
  (doseq [y (iterate-height)]
    (doseq [x (iterate-width)]
      (print (view-square-str view x y)))
    (println)))

(defn countp [view p]
  "Count the number of view squares that match a predicate"
  (count
   (for [square (iterate-board)
         :when (p (view-square-at view square))]
     nil)))

(defn count-marked [view]
  (countp view #(= % :marked)))

(defn count-revealed [view]
  (countp view #(number? %)))

(defn count-swines [view]
  (countp view #(= % :swine)))

(defn num-swines-unmarked [view]
  (- (num-swines game) (count-marked view)))

(defn is-game-lost [view]
  (> (count-swines view) 0))

(defn is-game-won [view]
  (= (count-revealed view)
     (- (* (width game) (height game)) (num-swines game))))

; View (ref) manipulation functions

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
        view)
      view)))

(defn #^String format-remaining-swines []
  (str "Remaining Swines: " @remaining-swines-ref))

; TODO don't fully reveal the board on lose... just reveal the swines/
; and possible make the one that you just hit be a different colour
(defn fully-reveal-board []
  (vec (for [y (iterate-height)]
    (vec (for [x (iterate-width)]
      (try-square x y))))))

(defn fully-reveal-board-on-win []
  (vec (for [y (iterate-height)]
    (vec (for [x (iterate-width)]
      (let [square (try-square x y)]
        (if (= square :swine) :marked square)))))))
          

;; Refs
(def board-ref (ref nil))
(def view-ref (ref (make-empty-view)))
(def remaining-swines-ref (ref (num-swines game)))

; Misc ref functions
(defn new-game-state []
  (cond
    (is-game-won @view-ref)  :game-won
    (is-game-lost @view-ref) :game-lost
    :else :game-playing))

(defn check-for-endgame []
  "Checks for the end of the game and updates game state. Must be called from
  within a transaction"
  (let [new-state (new-game-state)]
    (when (= new-state :game-won)
      (ref-set view-ref (fully-reveal-board-on-win)))
    (when (= new-state :game-lost)
      (ref-set view-ref (fully-reveal-board)))
;    (condp = new-state
;      :game-won (ref-set view-ref (fully-reveal-board))
;      :game-lost (ref-set view-ref (fully-reveal-board))
;      nil)
    (ref-set (state game) new-state)))

;; GUI stuff

;  GUI event handlers

(defn left-click [coords]
  (dosync
   (when (= @(state game) :pregame)
     (ref-set board-ref (make-swines (width game)
                                     (height game)
                                     (num-swines game)
                                     coords))
     (ref-set (state game) :game-playing))

   (when (= @(state game) :game-playing)
     (alter view-ref uncover [coords])
     (check-for-endgame))))

(defn double-click [coords]
  (dosync
   (when (= @(state game) :game-playing)
     (alter view-ref double-dude coords)
     (check-for-endgame))))

(defn right-click [coords]
  (dosync
   (when (= @(state game) :game-playing)
     (alter view-ref mark coords)
     (ref-set remaining-swines-ref (num-swines-unmarked @view-ref)))))

(defn make-action-listener [f]
  (proxy [ActionListener] []
    (actionPerformed [e] (f e))))

(defn load-image [filename]
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

(defn paint-square [#^Graphics g x y panel view images]
  (let [sx (* x (square-width game))
	sy (* y (square-height game))
	square ((view y) x)]
    (.drawImage g (images square) sx sy 
                (square-width game) (square-height game)
		Color/BLACK nil)))

(defn make-remaining-swines-panel []
  (let [label (JLabel. (format-remaining-swines))
        panel (JPanel.)]
    (.add panel label)
    (add-watch remaining-swines-ref
               "remaining-swines"
               (fn [k r o n]
                 (when (= @(state game))
                   (.setText label (format-remaining-swines)))))
    (add-watch (state game)
               "game state"
      (fn [k r o n]
        (condp = n
          :game-lost (SwingUtilities/invokeLater
                       #(.setText label "You lose, sucker!"))
          :game-won  (SwingUtilities/invokeLater
                       #(.setText label "You win. Hoo-ray!"))
          nil)))
    panel))
       

(defn make-board-panel []
  (let [pointless-panel (JPanel.)
        images (load-images)
	panel (proxy [JPanel] []
		(getPreferredSize []
                  (Dimension. (* (width game ) (square-width game))
                              (* (height game) (square-height game))))
		(paintComponent [g]
		  (doseq [y (iterate-height)
			  x (iterate-width)]
		    (paint-square g x y pointless-panel @view-ref images))))]
    (add-watch view-ref "view updated" (fn [k r o n]
                                         (.repaint panel)))
    (doto panel
      (.addMouseListener
       (proxy [MouseAdapter] []
	 (mouseClicked [#^MouseEvent e]
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
	     )))))

    panel))

(defn make-main-panel []
  (let [#^JPanel panel (JPanel.)]
    (doto panel
      (.setLayout (BoxLayout. panel BoxLayout/Y_AXIS))
      (.add (make-board-panel))
      (.add (make-remaining-swines-panel)))))

(defn make-frame [close-action]
  (let [frame (JFrame. "Swine Meeper")]
    (doto (.getContentPane frame)
      (.add (make-main-panel)))
    (doto frame
      (.setDefaultCloseOperation close-action)
      (.pack)
      (.show))))

(defn -main []
  (make-frame JFrame/EXIT_ON_CLOSE))

(defn swank-main []
  (make-frame JFrame/DISPOSE_ON_CLOSE))

;(swank-main)
