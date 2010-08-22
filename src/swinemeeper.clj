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

  (:use board view game)
  (:gen-class))

(def game (atom nil))

(def quit-fn (atom nil))

;; Mappings screen-coords <--> board-coords
; TODO move to GUI
(defn screen-to-board [ [x y] ]
  [ (int (/ x (.square-width @game))) (int (/ y (.square-height @game))) ] )

(defn board-to-screen [ [x y] ]
  [ (* x (.square-width @game)) (* y (.square-height @game)) ])

