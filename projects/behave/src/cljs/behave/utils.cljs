(ns behave.utils
  (:require [number-utils.interface :refer [to-precision count-precision]]))

(defn inclusive-range
  ([] (range))
  ([end] (range (inc end)))
  ([start end] (range start (inc end)))
  ([start end step] (when (or (and (pos? step) (< start end))
                              (and (neg? step) (> start end)))
                      (let [step-precision (max (count-precision start)
                                                (count-precision end)
                                                (count-precision step))
                            computed-range (range start (+ end step) step)]
                        (cond->> computed-range
                          (< -1.0 step 1.0)
                          (map #(to-precision % step-precision))

                          (or (and (pos? step) (> (last computed-range) end))
                              (and (neg? step) (< (last computed-range) end)))
                          butlast)))))
