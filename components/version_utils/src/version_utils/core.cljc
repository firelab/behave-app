(ns version-utils.core
  (:require [clojure.string :as str]))

(defn parse
  "Parse a dotted version string like \"7.1.4\" into [7 1 4].
  Returns nil for nil, blank, or non-numeric input."
  [s]
  (when (and (string? s) (not (str/blank? s)))
    (let [parts (str/split s #"\.")]
      (when (every? #(re-matches #"\d+" %) parts)
        (mapv #?(:clj  #(Integer/parseInt %)
                 :cljs js/parseInt) parts)))))

(defn- ->vec [v]
  (if (vector? v) v (parse v)))

(defn compare-versions
  "Compare two versions (strings or pre-parsed vectors). Returns -1, 0, or 1.
  nil sorts before any concrete version (treated as ancient/unknown).
  Shorter vectors are padded with zeros: \"7.1\" == \"7.1.0\"."
  [a b]
  (let [av (->vec a)
        bv (->vec b)]
    (cond
      (and (nil? av) (nil? bv)) 0
      (nil? av)                 -1
      (nil? bv)                 1
      :else (let [n   (max (count av) (count bv))
                  pad #(into (vec %) (repeat (- n (count %)) 0))]
              (compare (pad av) (pad bv))))))
