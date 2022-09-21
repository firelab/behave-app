(ns dom-utils.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Functions - Browser DOM and Event Utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn input-value
  "Returns the value property of the target property of an event."
  [event]
  (-> event .-target .-value))

(defn input-int-value
  "Given an event, returns the value as an integer."
  [event]
  (js/parseInt (input-value event)))

(defn input-float-value
  "Given an event, returns the value as a float."
  [event]
  (js/parseFloat (input-value event)))

(defn input-keyword
  "Given an event, returns the value as a Clojure keyword."
  [event]
  (keyword (input-value event)))

(defn input-file
  "Returns the file of the target property of an event."
  [event]
  (-> event .-target .-files (aget 0)))

(defn copy-input-clipboard!
  "Copies the contents of `element-id` into the user's clipboard. `element-id` must
   be the ID of an HTML element in the document."
  [element-id]
  {:pre [(string? element-id)]}
  (doto (js/document.getElementById element-id)
    (.select))
  (js/document.execCommand "copy"))
