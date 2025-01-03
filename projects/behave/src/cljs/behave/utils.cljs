(ns behave.utils
  (:require [number-utils.interface :refer [to-precision count-precision]]))

(defn inclusive-range
  ([] (range))
  ([end] (range (inc end)))
  ([start end] (range start (inc end)))
  ([start end step] (when (pos? step)
                      (let [step-precision (max (count-precision start)
                                                (count-precision end)
                                                (count-precision step))
                            computed-range (range start (+ end step) step)]
                        (cond->> computed-range
                          (< 0 step 1.0)                (map #(to-precision % step-precision))
                          (> (last computed-range) end) butlast)))))
