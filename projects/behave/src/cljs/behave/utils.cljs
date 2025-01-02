(ns behave.utils
  (:require [number-utils.interface :refer [to-precision count-precision]]))

(defn inclusive-range
  ([] (range))
  ([end] (range (inc end)))
  ([start end] (range start (inc end)))
  ([start end step] (when (pos? step)
                      (if (< 0 step 1.0)
                        (let [step-precision (count-precision step)]
                          (map #(to-precision % step-precision)
                               (range start (+ end step) step)))
                        (range start (+ end step) step)))))
