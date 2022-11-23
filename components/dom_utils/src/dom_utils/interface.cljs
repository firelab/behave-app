(ns dom-utils.interface
  (:require [dom-utils.core :as c]))

(def ^{:argslist '([event])
       :doc      "Returns the value property of the target property of an event."}
  input-value c/input-value)

(def ^{:argslist '([event])
       :doc      "Given an event, returns the value as an integer."}
  input-int-value c/input-int-value)

(def ^{:argslist '([event])
       :doc      "Given an event, returns the value as a float."}
  input-float-value
  c/input-float-value)

(def ^{:argslist '([event])
       :doc      "Given an event, returns the value as a sequence of floats."}
  input-float-values
  c/input-float-values)

(def ^{:argslist '([event])
       :doc      "Given an event, returns the value as a Clojure keyword."}
  input-keyword c/input-keyword)

(def ^{:argslist '([event])
       :doc      "Returns the file of the target property of an event."}
  input-file c/input-file)

(def ^{:argslist
       '([element-id])

       :doc
       "Copies the contents of `element-id` into the user's clipboard. `element-id` must
       be the ID of an HTML element in the document."}
  copy-input-clipboard! c/copy-input-clipboard!)
