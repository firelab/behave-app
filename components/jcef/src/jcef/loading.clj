(ns jcef.loading
  (:require [clojure.java.io :as io])
  (:import [javax.swing JFrame JLabel ImageIcon JProgressBar]
           [java.awt BorderLayout Color Image Toolkit]
           [javax.swing.border EmptyBorder]
           [javax.swing WindowConstants SwingConstants]))

(defn- create-loader [project image-resource]
  (let [frame         (JFrame. (str "Loading " project))
        icon          (ImageIcon. (.getScaledInstance (.getImage (ImageIcon. image-resource)) 256 256 Image/SCALE_DEFAULT))
        progress-bar  (JProgressBar.)
        image-label   (JLabel. icon)
        loading-label (JLabel. (str "Loading " project "..."))
        toolkit       (Toolkit/getDefaultToolkit)
        screen-size   (.getScreenSize toolkit)]

    #_(doto progress-bar
      (.setOpaque true)
      (.setBackground Color/WHITE)
      (.setMaximum 100)
      (.setBorder (EmptyBorder. 0 30 0 30)))

    (doto loading-label
      (.setOpaque true)
      (.setBackground Color/WHITE)
      (.setBorder (EmptyBorder. 10 10 10 10))
      (.setHorizontalAlignment SwingConstants/CENTER)
      (.setHorizontalTextPosition SwingConstants/CENTER))

    (doto frame
      (.setResizable false)
      (.setLayout (BorderLayout.))
      (.add image-label BorderLayout/NORTH)
      #_(.add progress-bar BorderLayout/CENTER)
      (.add loading-label BorderLayout/SOUTH)
      (.setUndecorated true) ; Remove window controls
      (.setDefaultCloseOperation WindowConstants/EXIT_ON_CLOSE)
      (.pack)
      (.setLocation (- (/ (.getWidth screen-size) 2) (/ (.getWidth frame) 2))
                    (- (/ (.getHeight screen-size) 2) (/ (.getHeight frame) 2))) ; Center the frame
      (.setVisible true))
    {:frame frame
     :progress progress-bar}))

(defn show-loader!
  "Displays a loader centered on the screen. Takes:
  - `project`  - Project Name to display on the loading screen
  - `icon-url` - Icon display on the loading screen, at least 256x256 px.
                 This can use a local resource URL (e.g. `(io/resource <path-to-icon>)`).

  Returns a map with keys:
   - `:frame`    - JFrame, which should be disposed with `(.dispose frame)`
   - `:progress` - JProgressBar, which can be updated with
                   `(.setValue progress-bar <value>)`"
  [project icon-url]
  (create-loader project icon-url))

(comment
  (def result (show-loader! "Behave7" (io/resource "public/images/icon.png")))
  (.setMaximum (:progress result) 100)
  (.setValue (:progress result) 30)
  (.dispose (:frame result))
  )
