;; TODO configure dimensions + number of swines by popping up some kind of
;;      dialog or something - still need to make the dialog less rubbish 
;; TODO timer
;; TODO high score table
;; TODO turn it into an applet
;; TODO do all thar gui calls on the awt thread


;; TODO create a "quit game" function which can be different depending on
;;      whether we launched from swank or not

;(set! *warn-on-reflection* true)

(ns swinemeeper

  (:use board)
  (:gen-class))

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
;(declare game)
(def game (atom nil))

(def quit-fn (atom nil))

(defn make-game [width height square-width square-height num-swines]
  (struct state-struct width height square-width square-height num-swines
          :pregame nil (make-empty-view width height)
          num-swines))

;(declare square-str)

;; Mappings screen-coords <--> board-coords
(defn screen-to-board [ [x y] ]
  [ (int (/ x (square-width @game))) (int (/ y (square-height @game))) ] )

(defn board-to-screen [ [x y] ]
  [ (* x (square-width @game)) (* y (square-height @game)) ])

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
		      (@neighbours x y)))))

(defn num-marked-neighbours [view [x y]]
  (num-neighbours= view [x y] :marked))

(defn num-unknown-neighbours [view [x y]]
  (num-neighbours= view [x y] :unknown))

(defn view-square-str [view x y]
  (square-str (view-square-at view x y)))

(defn print-view [view]
  (doseq [y (iterate-height)]
    (doseq [x (iterate-width)]
      (print (view-square-str view x y)))
    (println)))

(defn countp [view p]
  "Count the number of view squares that match a predicate"
  (count
   (for [square (iterate-board (board @game))
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

; View manipulation functions

(defn uncover [view coords]
  (if (= coords [])
    view
    (let [[x y] (first coords)
	  square (try-square (board @game) [x y])
	  new-view (assoc-in view [y x] square)
	  new-coords (if (and
			  (= (view-square-at view [x y]) :unknown)
			  (= square 0))
		       (concat (rest coords) (@neighbours x y))
		       (rest coords))]
      (recur new-view new-coords))))

(defn mark [view [x y]]
  (condp = (view-square-at view [x y])
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
                         (@neighbours x y)))
        view)
      view)))

(defn #^String format-remaining-swines []
  (str "Remaining Swines: " (remaining-swines @game)))

(defn fully-reveal-board-on-lose [board view]
  (vec (for [y (iterate-height board)]
    (vec (for [x (iterate-width board)]
       (condp = (view-square-at view [x y])
         :marked (if (not (is-swine? board [x y]))
                   :incorrectly-marked
                   :marked)
         :swine :exploding_swine
         :unknown (if (is-swine? board [x y])
                    :swine
                    :unknown)
         (try-square board [x y])))))))

(defn fully-reveal-board-on-win [board view]
  (vec (for [y (iterate-height board)]
    (vec (for [x (iterate-width board)]
      (let [square (try-square board [x y])]
        (if (= square :swine) :marked square)))))))

; Game manpulation functions
(defn new-game-state [game]
  (cond
    (is-game-won (view game))  :game-won
    (is-game-lost (view game)) :game-lost
    :else :game-playing))

(defn check-for-endgame [game]
  "Checks for the end of the game and updates game state."
  (let [new-state (new-game-state game)]
    (condp = new-state
      :game-won (assoc game :view (fully-reveal-board-on-win (board game)
                                                             (view game))
                            :state new-state)
      :game-lost (assoc game :view (fully-reveal-board-on-lose (board game)
                                                               (view game))
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
  (assoc game :board (make-board (width game)
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

