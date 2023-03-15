(ns map-utils.interface
  (:require [map-utils.core :as c]))

(def ^{:argslist '([k coll])
       :doc      "Returns a map where `coll` is indexed by key `k`.
                  WARNING: Will not work with multiple entries with the same value for key `k`. For that, use `group-by`."}
  index-by c/index-by)
