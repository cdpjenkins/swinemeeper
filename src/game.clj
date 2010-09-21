(ns game
  (:use board view))

(defprotocol IGame)

(defrecord CGame [width
		  height
		  square-width
		  square-height
		  num-swines
		  state
		  board
		  view
		  remaining-swines])


(defn make-game [width height square-width square-height num-swines]
  (CGame. width height square-width square-height num-swines
          :pregame nil (make-empty-view width height nil)
          num-swines))

(defn num-swines-unmarked [view num-swines]
  (- num-swines (count-marked view)))

(defn is-game-lost [game]
  (> (count-swines (.view game)) 0))

(defn is-game-won [game]
  (= (count-revealed (.view game))
     (- (* (.width game) (.height game)) (.num-swines game))))

(defn new-game-state [game]
  (cond
    (is-game-won game)  :game-won
    (is-game-lost game) :game-lost
    :else :game-playing))

(defn check-for-endgame [game]
  "Checks for the end of the game and updates game state."
  (let [new-state (new-game-state game)]
    (condp = new-state
      :game-won (assoc game :view (fully-reveal-board-on-win (.view game))
                            :state new-state)
      :game-lost (assoc game :view (fully-reveal-board-on-lose (.view game))
                             :state new-state)
      (assoc game :state new-state))))

(defn game-reveal-square [game coords]
  (condp = (.state game)
    :game-playing
    (let [new-view (uncover (.view game) [coords])
          new-game (assoc game :view new-view)]
      (check-for-endgame new-game))
    game))

(defn game-create-board [game coords]
  (println "hoss")
  (let [board (make-board (.width game)
			  (.height game)
			  (.num-swines game)
			  coords)
	new-view (assoc (.view game) :board board)]
    (assoc game :board board
	        :view new-view
		:state :game-playing)))

(defn create-board-if-pregame [game coords]
  (println (.state game))
  (if (= (.state game) :pregame)
    (game-create-board game coords)
    game))

;; Functions that support GUI interaction
(defn game-left-click [game coords]
  (let [new-game (create-board-if-pregame game coords)]
    (game-reveal-square new-game coords)))

(defn game-double-click [game coords]
  (if (= (.state game) :game-playing)
    (let [new-game (assoc game :view (double-dude (.view game) coords))]
      (check-for-endgame new-game))
    game))

(defn game-flag [game coords]
  (let [new-view (mark (.view game) coords)]
    (assoc game :view new-view
	        :remaining-swines
		  (num-swines-unmarked new-view (.num-swines game)))))
