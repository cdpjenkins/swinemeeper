(ns gui
  (:use swinemeeper board)
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
		  (doseq [y (range (height @game))
			  x (range (width @game))]
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

;(def game (atom nil))
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
  (reset! neighbours (make-neighbours (width @game) (height @game))))
