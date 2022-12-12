(ns behave.pubsub
  (:require [clojure.core.async :refer [<! go-loop chan put! sub chan pub]]))

(defn pub-ch [ch topic]
  (pub ch topic))

(defn add-route [pub-ch route sub-ch]
  (sub pub-ch route sub-ch))

(def c (chan 1))
(def router (pub-ch c :route))
(def up (chan 1))
(def down (chan 1))

(add-route router :up-stream up)
(add-route router :down-stream down)

(go-loop []
         (when-let [e (<! up)]
           (println "Got something coming up!" e)
           (recur)))

(go-loop []
         (when-let [e (<! down)]
           (println "Got something going down!" e)
           (recur)))

(put! c {:route :up-stream :data 23423})

(put! c {:route :down-stream :data 345})
