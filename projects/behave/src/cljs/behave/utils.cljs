(ns behave.utils)

(defn inclusive-range
  ([] (range))
  ([end] (range (inc end)))
  ([start end] (range start (inc end)))
  ([start end step] (range start (+ end step) step)))
