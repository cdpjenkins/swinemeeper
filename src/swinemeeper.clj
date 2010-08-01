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
  (:use compojure.core
        clojure.contrib.seq
        hiccup.core
        hiccup.page-helpers
        ring.adapter.jetty
        ring.util.response)

  (:require [compojure.route :as route])
  (:gen-class))

(import
 '(java.awt Color Dimension Graphics GridLayout)
 '(java.awt.event ActionListener MouseAdapter MouseEvent)
 '(java.awt.image BufferedImage)
 '(java.io File)
 '(javax.imageio ImageIO)
 '(javax.swing BoxLayout ButtonGroup
               JButton JDialog JFrame JLabel JPanel JRadioButton
               SwingUtilities))

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
(declare game)

(def quit-fn (atom nil))

(defn make-game [width height square-width square-height num-swines]
  (struct state-struct width height square-width square-height num-swines
          :pregame nil (make-empty-view width height)
          num-swines))

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

(defn squares-are-adjacent [ [x1 y1] [x2 y2] ]
  (and
   (< (Math/abs (- x1 x2)) 3)
   (< (Math/abs (- y1 y2)) 3)))

(defn make-swines [width height num-swines & [exclude-square ] ]
  (set
   (take num-swines
	 (shuffle (for [square (iterate-board)
                        :when (not
                               (squares-are-adjacent square exclude-square))]
                    square)))))

(defn is-swine? [board pos]
  (contains? board pos))

(defn neighbours-fn [x y]
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

(def neighbours (atom (memoize neighbours-fn)))

(defn num-surrounding [x y]
  (count (filter #(is-swine? (board @game) %) (@neighbours x y))))

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
		      (@neighbours x y)))))

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
  (square-str (view-square-at view x y)))

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

; View manipulation functions

(defn uncover [view coords]
  (if (= coords [])
    view
    (let [[x y] (first coords)
	  square (try-square x y)
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

; TODO don't fully reveal the board on lose... just reveal the swines/
; and possible make the one that you just hit be a different colour
(defn fully-reveal-board-on-lose [board view]
  (vec (for [y (iterate-height)]
    (vec (for [x (iterate-width)]
;      (if (= (view-square-at [x y]) :unknown)
;        (try-square x y)
;        (view-square-at [x y]) ))))))
       (condp = (view-square-at view [x y])
         :marked (if (not (is-swine? board [x y]))
                   :incorrectly-marked
                   :marked)
         :swine :exploding_swine
         :unknown (if (is-swine? board [x y])
                    :swine
                    :unknown)
         (try-square x y)))))))

(defn fully-reveal-board-on-win [board view]
  (vec (for [y (iterate-height)]
    (vec (for [x (iterate-width)]
      (let [square (try-square x y)]
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
  ; TODO slight hack here but not sure what to do... if the game is not
  ;      in progress then need to start the game
  (when (= (state @game) :pregame)
    (swap! game game-create-board coords))
  (when (= (state @game) :game-playing)
    (swap! game game-reveal-square coords)))

(defn double-click [coords]
  (swap! game game-double-click coords))

(defn right-click [coords]
  (swap! game game-flag coords))

(defn load-image [filename]
  (ImageIO/read (ClassLoader/getSystemResource filename)))

(defmacro make-action-listener [ [e] & body]
  `(proxy [ActionListener] []
     (actionPerformed [~e]
       ~@body)))
(def images
  {:unknown "unknown.png"
   :swine "swine.png"
   :exploding_swine"exploding_swine.png"
   :marked "marked.png"
   :incorrectly-marked "incorrectly_marked.png"
   0 "0.png"
   1 "1.png"
   2 "2.png"
   3 "3.png"
   4 "4.png"
   5 "5.png"
   6 "6.png"
   7 "7.png"
   8 "8.png"})

(defn load-images []
  {:unknown (load-image "unknown.png")
   :swine (load-image "swine.png")
   :exploding_swine (load-image "exploding_swine.png")
   :marked (load-image "marked.png")
   :incorrectly-marked (load-image "incorrectly_marked.png")
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
	square (view-square-at view [x y])]
    (.drawImage g (images square) sx sy 
                (square-width @game) (square-height @game)
		Color/BLACK nil)))

;; HACK TODO sort out
(declare frame)
(declare new-game)

; TODO HACK
(defn make-radio-button [name]
  (let [button (JRadioButton. name)]
    (doto button
      (.setActionCommand name))))

(defn make-choose-game-dialog []
  (let [dialog (JDialog. @frame "Husston-skank" true)
        pane (JPanel.)
        button-group (ButtonGroup.)
        small-button (make-radio-button "Small")
        medium-button (make-radio-button "Medium")
        large-button (make-radio-button "Large")
        buttons-panel (JPanel.)
        start-game-button (JButton. "Start Game")
        quit-button (JButton. "Quit")]

    (.addActionListener start-game-button
      (make-action-listener [e]
        (condp = (.. button-group getSelection getActionCommand)
          "Small" (new-game (make-game 10 10 32 32 10))
          "Medium" (new-game (make-game 16 16 32 32 40))
          "Large" (new-game (make-game 30 16 32 32 99))
          nil)
          (.setVisible dialog false)))
    (.addActionListener quit-button
      (make-action-listener [e]
        (@quit-fn)))
            
    (doto buttons-panel
      (.setLayout (BoxLayout. buttons-panel BoxLayout/X_AXIS))
      (.add start-game-button)
      (.add quit-button))

    (.setLayout pane (BoxLayout. pane BoxLayout/Y_AXIS))
    (doto button-group
      (.add small-button)
      (.add medium-button)
      (.add large-button))      
    (doto pane
      (.add small-button)
      (.add medium-button)
      (.add large-button)
      (.add buttons-panel))
    (.setContentPane dialog pane)
    (.pack dialog)
    (.setVisible dialog true)))

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
                      (fn []
                        (.setText label "You lose, sucker!")
                        (make-choose-game-dialog)))
          :game-won  (SwingUtilities/invokeLater
                      (fn []
                        (.setText label "You win. Hoo-ray!")
                        (make-choose-game-dialog)))
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
    (add-watch game "view updated" 
               (fn [k r o n]
                 (if (not= o n)
                   (doto panel
                     (.invalidate)
                     (.repaint)))))
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

(def game (atom nil))
(def frame (atom nil))

(defn quit-jvm []
  (System/exit 0))

(defn quit-swank []
  (.setVisible @frame false)
  (.dispose @frame)
  (reset! frame nil))

(defn -main []
  ;; TODO hack
  ;; TODO move that into a "make easy game function"
  (reset! game (make-game 12 12 32 32 12))
  (reset! frame (make-frame JFrame/EXIT_ON_CLOSE))
  (reset! quit-fn quit-jvm)
  (make-choose-game-dialog))

(defn swank-main []
  ;; TODO hack
  ;; TODO move that into a "make easy game function"
  (reset! game (make-game 12 12 32 32 12))
  (reset! frame (make-frame JFrame/DISPOSE_ON_CLOSE))
  (reset! quit-fn quit-swank)
  (make-choose-game-dialog))

; TODO figure out an appropriate place to put this and then move it there
(defn new-game [the-game]
  (reset! game the-game)
  ; TODO write our own pack function that checks if not null n that
  (when (not (nil? @frame))
    (.pack @frame))
  (when (not (nil? @frame))
    (.repaint @frame))
  ; TODO sort this out a bit better
  (reset! neighbours (memoize neighbours-fn)))

;;;;; web esque stuff
(defn draw-board []
  (html
   [:html
    [:head [:title "Swine Meeper"]]
    [:body
     [:table
      (let [view (:view @game)]
        (for [ [y row] (indexed view)]
          [:tr
           (for [ [x square] (indexed row)]
             (let [form-id (str x "_" y)]
               [:td
;              [:a {:href (str "/swinemeeper/click/" x "/" y)}
                [:form {:action (str "/swinemeeper/click")
                        :method "get"
                        :id form-id}
                 [:input {:type "hidden"
                          :name "bx"
                          :value (str x)}]
                 [:input {:type "hidden"
                          :name "by"
                          :value (str y)}]
                 [:a {:href (str "javascript: document.forms['"
                                 form-id "'].submit()") }
                  [:img {:src (str "/images/" (images square))   }]]
;                 [:input {:type "image"
;                          :src (str "/images/" (images square))
;                          :border "0"}]]
                ]]))]))]]]))

(defn husston [name]
  (let [path (str "src/" name ".png")]
    (println path)
    (file-response path)))

(defn hoss []
  (println "hoss")
  "<h1>Mooing bovine cow!</h1>")

(defn web-left-click [coords]
  (left-click coords)
  (draw-board))

(defroutes example
  (GET "/" [] (hoss))

  (GET "/huss" [kiwi snake] (println kiwi snake))

  (GET "/images/:name.png" [name] (husston name))
 
  (GET "/view-cookies" {cookies :cookies} (str cookies))

  (GET "/cuss-ton" [x y]
      (str x y))

  (GET "/swinemeeper" [] (draw-board))
  (GET "/swinemeeper/click" [bx by]
    (do
      (println "huss" bx)
      (println "hoss" by)
      (web-left-click 
       [(Integer/parseInt bx)
        (Integer/parseInt by)])
      (redirect "/swinemeeper")))
  (GET "/other" [] "poo")
  (route/not-found "Page not found"))

(defonce server (run-jetty (var example) {:port 8080 :join? false}))
