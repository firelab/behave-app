(ns map-utils.core)

(defn index-by
  "Indexes collection by k."
  [k coll]
  (persistent! (reduce
                (fn [acc cur] (assoc! acc (get cur k) cur))
                (transient {})
                coll)))

(defn assoc-conj
  "Assoc k onto m with v. If k already exists, conj v into a vector."
  [m k v]
  (assoc m k
    (if-let [cur (get m k)]
      (if (vector? cur)
        (conj cur v)
        [cur v])
      v)))
