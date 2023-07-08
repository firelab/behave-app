(ns map-utils.interface
  (:require [map-utils.core :as c]))

(def ^{:argslist '([k coll])
       :doc      "Returns a map where `coll` is indexed by key `k`.
                  WARNING: Will not work with multiple entries with the same value for key `k`. For that, use `group-by`."}
  index-by c/index-by)

(def ^{:argslist '([map key val])
       :doc      "Associate a key with a value in a map. If the key already exists in the map,
                 a vector of values is associated with the key."}
  assoc-conj c/assoc-conj)
