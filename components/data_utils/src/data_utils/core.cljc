(ns data-utils.core
  (:require [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Macros
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro vmap
  "Creates a hash-map using just variable names.

  Example:
  ```
  (let [a 1
        b 2
        c 3]
    (hmap a b c)) ; => {:a 1 :b 2 :c 3}
  ```
  "
  [& vars]
  `(hash-map ~@(mapcat (fn [v] [(keyword (name v)) v]) vars)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Functions - Data Utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-digit?
  "Checks is every character is digit in `s`."
  [s]
  #?(:cljs (every? (comp not js/isNaN js/parseInt) s)
     :clj  (try
             (number? (Integer/parseInt s))
             (catch Exception _ false))))

(defn is-int?
  "Checks if `s` is an integer."
  [s]
  #?(:cljs (every? (comp not js/isNaN js/parseInt) s)
     :clj  (try
             (number? (Integer/parseInt s))
             (catch Exception _ false))))

(defn is-float?
  "Checks if `s` is a float."
  [s]
  #?(:cljs ((comp not js/isNaN js/parseFloat) s)
     :clj  (try
             (float? (Float/parseFloat s))
             (catch Exception _ false))))

(defn parse-int
  "Parse string `s` into an integer."
  [s]
  #?(:clj  (Integer/parseInt s)
     :cljs (js/parseInt s)))

(defn parse-float
  "Parse string `s` into a float."
  [s]
  #?(:clj  (Float/parseFloat s)
     :cljs (js/parseFloat s)))

(defn- no-data? [x]
  (or
   (and (number? x) (.isNaN #?(:clj (Double. x) :cljs (js/Number x))))
   (and (string? x)
        (re-matches #"\d{4,}-\d{2}-\d{2}" x)
        (not (< 1990 (parse-int (first (str/split x #"-"))) 2200)))
   (and (string? x) (str/blank? x))
   (and (coll?   x) (empty? x))
   (nil? x)))

(defn has-data?
  "Checks if an input of any type has data."
  [x]
  (not (no-data? x)))

(defn missing-data?
  "Checks if an input of any type is missing specific data."
  [& args]
  (some no-data? args))

(defn remove-from
  "Removes any 'k' from the provided `m` that matches `v`."
  [m k v]
  (remove #(= v (get % k)) m))

(defn replace-with-nil
  "Replaces each entry in `coll` with nil whose `k` kv-pair matches any entry in
  `to-replace` set."
  [coll k to-replace & [xform]]
  (mapv (fn [entry]
         (let [v (get entry k)]
           (assoc entry k (if (contains? to-replace (if (fn? xform) (xform v) v))
                                nil
                                v))))
        coll))

(defn filterm
  "A version of `filter` that uses transients."
  [f coll]
  (persistent!
   (reduce (fn [acc cur]
             (if (f cur)
               (conj! acc cur)
               acc))
           (transient {})
           coll)))

(defn mapm
  "A version of `map` that uses transients."
  [f coll]
  (persistent!
   (reduce (fn [acc cur]
             (conj! acc (f cur)))
           (transient {})
           coll)))

(defn reverse-sorted-map
  "Creates a sorted-map where the keys are sorted in reverse order."
  []
  (sorted-map-by (fn [a b] (* -1 (compare a b)))))

(defn get-changed-keys
  "Takes in two maps with the same keys and (potentially) different values.
   Determines which values are different between the two maps and returns a set
   containing the keys associated with the changed values."
  [old-map new-map]
  (reduce (fn [acc k]
             (if (not= (get old-map k) (get new-map k))
               (conj acc k)
               acc))
          #{}
          (keys old-map)))

(defn find-boundary-values
  "Returns the two values from a sorted collection that bound v."
  [v coll]
  (loop [coll coll]
    (let [s (second coll)]
      (and s
           (if (< v s)
             (take 2 coll)
             (recur (next coll)))))))

(defn find-key-by-id
  "Finds the value of a key by id if one exists."
  ([coll id]
   (find-key-by-id coll id :opt-label))
  ([coll id k]
   (some #(when (= (:opt-id %) id) (get % k)) coll)))

(defn find-by-id
  "Finds the value of a specific id if one exists."
  [coll id]
  (some #(when (= (:opt-id %) id) %) coll))

(defn remove-nth
  "Removes the nth element from a collection"
  [coll n]
  (if (and (<= 0 n) (coll? coll) (> (count coll) n))
    (cond
      (list? coll)
      (concat (take n coll) (drop (inc n) coll))

      (vector? coll)
      (vec (concat (subvec coll 0 n) (subvec coll (inc n))))

      :else
      coll)
    coll))
