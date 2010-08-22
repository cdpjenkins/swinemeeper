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
  (println "in i-=game-won" (.view game))
  (= (count-revealed (.view game))
     (- (* (.width game) (.height game)) (.num-swines game))))

(defn new-game-state [game]
  (cond
    (is-game-won game)  :game-won
    (is-game-lost game) :game-lost
    :else :game-playing))

(defn check-for-endgame [game]
  "Checks for the end of the game and updates game state."
  (println "in check-for-endgame" (.view game))
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
      (println "in game-reveal-square new-view:" new-view)
      (check-for-endgame new-game))
    game))

(defn game-create-board [game coords]
  (let [board (make-board (.width game)
			  (.height game)
			  (.num-swines game)
			  coords)
	new-view (assoc (.view game) :board board)]
    (assoc game :board board
	        :view new-view
		:state :game-playing)))

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
