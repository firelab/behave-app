(ns map-utils.core)

(defn index-by [k coll]
  (persistent! (reduce
                (fn [acc cur] (assoc! acc (get cur k) cur))
                (transient {})
                coll)))
