(ns behave.helpers)

(defn within?
  "Returns true if `a` and `b` are within `precision` of one another."
  [precision a b]
  (> precision (Math/abs (- a b))))

(def within-one-percent? (partial within? 0.1))
(def within-hundredth?   (partial within? 1e-02))
(def within-thousandth?  (partial within? 1e-03))
(def within-millionth?   (partial within? 1e-06))
