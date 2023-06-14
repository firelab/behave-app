(ns data-utils.interface
  (:require [data-utils.core :as c]))

(def ^{:argslist '([s])
       :doc      "Parses a string into an integer (CLJ/CLJS)"}
  parse-int c/parse-int)

(def ^{:argslist '([s])
       :doc      "Parses a string into a float (CLJ/CLJS)"}
  parse-float c/parse-float)

(def ^{:argslist '([x])
       :doc      "Checks if a value no data. Can check strings, numbers, and dates as strings."}
  has-data? c/has-data?)

(def ^{:argslist '([x])
       :doc      "Checks if an input of any type is missing specific data."}
  missing-data? c/missing-data?)

(def ^{:argslist '([m k v])
       :doc      "Removes any 'k' from the provided `m` that matches `v`."}
  remove-from c/remove-from)

(def ^{:argslist '([coll k to-replace] [coll k to-replace xform])
       :doc      "Replaces each entry in `coll` with nil whose `k` kv-pair matches any entry in
  `to-replace` set. Can also apply `xform` to the value."}
  replace-with-nil c/replace-with-nil)

(def ^{:argslist '([f coll])
       :doc      "A version of `filter` that uses transients."}
  filterm c/filterm)

(def ^{:argslist '([f coll])
       :doc      "A version of `map` that uses transients."}
  mapm c/mapm)

(def ^{:argslist '([])
       :doc      "Creates a sorted-map where the keys are sorted in reverse order."}
  reverse-sorted-map c/reverse-sorted-map)

(def ^{:argslist '([old-map new-map])
       :doc      "Takes in two maps with the same keys and (potentially) different values. Determines which values are different between the two maps and returns a set containing the keys associated with the changed values."}
  get-changed-keys c/get-changed-keys)

(def ^{:argslist '([v coll])
       :doc      "Returns the two values from a sorted collection that bound v."}
  find-boundary-values c/find-boundary-values)
