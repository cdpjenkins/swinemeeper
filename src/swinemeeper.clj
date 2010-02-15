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
  :state
  :board
  :view
  :remaining-swines)

(def width (accessor state-struct :width))
(def height (accessor state-struct :height))
(def square-width (accessor state-struct :square-width))
(def square-height (accessor state-struct :square-height))
(def num-swines (accessor state-struct :num-swines))
(def state (accessor state-struct :state))
(def board (accessor state-struct :board))
(def view (accessor state-struct :view))
(def remaining-swines (accessor state-struct :remaining-swines))

(declare make-empty-view)

(defn make-game [width height square-width square-height num-swines]
  (struct state-struct width height square-width square-height num-swines
          :pregame nil (make-empty-view width height)
          num-swines))

(def game (ref (make-game 12 12
                          32 32
                          15)))

;(declare remaining-swines-ref)

(declare square-str)

;; Mappings screen-coords <--> board-coords
(defn screen-to-board [ [x y] ]
  [ (int (/ x (square-width @game))) (int (/ y (square-height @game))) ] )

(defn board-to-screen [ [x y] ]
  [ (* x (square-width @game)) (* y (square-height @game)) ])

;; Board functions

(defn iterate-width []
  (range (width @game)))

(defn iterate-height []
  (range (height @game)))

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

(defn is-swine? [board pos]
  (contains? board pos))

(defn neighbours [x y]
  (filter
   (fn [ [x y] ] (and (>= x 0)
		      (< x (width @game))
		      (>= y 0)
		      (< y (height @game))))
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
  (count (filter #(is-swine? (board @game) %) (neighbours x y))))

(defn try-square [x y]
  (if (is-swine? (board @game) [x y])
    :swine
    (num-surrounding x y)))

(defn print-board []
  (doseq [y (iterate-height)]
    (doseq [x (iterate-width)]
      (print (square-str (try-square x y))))
    (println)))

;; View functions

(defn make-empty-view [width height]
  (vec (for [y (range height)]
    (vec (for [x (range width)]
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
  (- (num-swines @game) (count-marked view)))

(defn is-game-lost [view]
  (> (count-swines view) 0))

(defn is-game-won [view]
  (= (count-revealed view)
     (- (* (width @game) (height @game)) (num-swines @game))))

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
  (str "Remaining Swines: " (remaining-swines @game)))

; TODO don't fully reveal the board on lose... just reveal the swines/
; and possible make the one that you just hit be a different colour
(defn fully-reveal-board [board]
  (vec (for [y (iterate-height)]
    (vec (for [x (iterate-width)]
      (try-square x y))))))

(defn fully-reveal-board-on-win [board]
  (vec (for [y (iterate-height)]
    (vec (for [x (iterate-width)]
      (let [square (try-square x y)]
        (if (= square :swine) :marked square)))))))

; Game manipulation functions
(defn new-game-state [game]
  (cond
    (is-game-won (view game))  :game-won
    (is-game-lost (view game)) :game-lost
    :else :game-playing))

(defn check-for-endgame [game]
  "Checks for the end of the game and updates game state. Must be called from
  within a transaction"
  (let [new-state (new-game-state game)]
    (condp = new-state
      :game-won (assoc game :view (fully-reveal-board-on-win (board game))
                            :state new-state)
      :game-lost (assoc game :view (fully-reveal-board (board game))
                             :state new-state)
      (assoc game :state new-state))))

(defn game-reveal-square [game coords]
  (condp = (state game)
    :game-playing
    (let [new-view (uncover (view game) [coords])
          new-game (assoc game :view new-view)]
      (check-for-endgame new-game))
    game))

(defn game-create-board [game coords]
  (assoc game :board (make-swines (width game)
                                  (height game)
                                  (num-swines game)
                                  coords)
              :state :game-playing))

; TODO better name
(defn game-double-click [game coords]
  (if (= (state game) :game-playing)
    (let [new-game (assoc game :view (double-dude (view game) coords))]
      (check-for-endgame new-game))
    game))

(defn game-flag [game coords]
  (let [new-view (mark (view game) coords)]
    (assoc game :view new-view
                :remaining-swines (num-swines-unmarked new-view))))

;; GUI stuff

;  GUI event handlers

(defn left-click [coords]
  (dosync
   ; TODO slight hack here but not sure what to do... if the game is not
   ;      in progress then need to start the game
   (when (= (state @game) :pregame)
     (alter game game-create-board coords))
   (when (= (state @game) :game-playing)
     (alter game game-reveal-square coords))))

(defn double-click [coords]
  (dosync
   (alter game game-double-click coords)))

(defn right-click [coords]
  (dosync
   (alter game game-flag coords)))

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
  (let [sx (* x (square-width @game))
	sy (* y (square-height @game))
	square ((view y) x)]
    (.drawImage g (images square) sx sy 
                (square-width @game) (square-height @game)
		Color/BLACK nil)))

(defn make-remaining-swines-panel []
  (let [label (JLabel. (format-remaining-swines))
        panel (JPanel.)]
    (.add panel label)
    (add-watch game
               "remaining-swines"
               (fn [k r o n]
                 (when (= (state @game) :game-playing)
                   (.setText label (format-remaining-swines)))))
    (add-watch game
               "game state"
      (fn [k r o n]
        (condp = (state n)
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
                  (Dimension. (* (width @game ) (square-width @game))
                              (* (height @game) (square-height @game))))
		(paintComponent [g]
		  (doseq [y (iterate-height)
			  x (iterate-width)]
		    (paint-square g x y pointless-panel (view @game) images))))]
    (add-watch game "view updated" (fn [k r o n]
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
	       MouseEvent/BUTTON3 (right-click coords)))))))

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
