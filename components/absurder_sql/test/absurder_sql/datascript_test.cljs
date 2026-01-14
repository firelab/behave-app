(ns absurder-sql.datascript-test
  (:require
   [absurder-sql.datascript :as ds]
   [me.tonsky.persistent-sorted-set :as set :refer [Node]]
   [cljs.test :as t :include-macros true :refer [async deftest is use-fixtures]]))

(def iters 5)

;; confirm that clj's use of sorted set works as intended.
;; allow for [:foo nil] to glob [:foo *]; data will never be inserted
;; w/ nil, but slice/subseq elements will.

(defn cmp [x y]
  (if (and x y)
    (compare x y)
    0))

(defn cmp-s [[x0 x1] [y0 y1]]
  (let [c0 (cmp x0 y0)
        c1 (cmp x1 y1)]
    (cond
      (= c0 0) c1
      (< c0 0) -1
      (> c0 0)  1)))

(deftest semantic-test-btset-by
  (let [e0 (set/sorted-set-by cmp-s)
        ds [[:a :b] [:b :x] [:b :q] [:a :d]]
        e1 (reduce conj e0 ds)]
    (is (= (count ds)        (count (seq e1))))
    (is (= (vec (seq e1))    (vec (set/slice e1 [nil nil] [nil nil])))) ; * *
    (is (= [[:a :b] [:a :d]] (vec (set/slice e1 [:a nil]  [:a nil] )))) ; :a *
    (is (= [[:b :q]]         (vec (set/slice e1 [:b :q]   [:b :q]  )))) ; :b :q (specific)
    (is (= [[:a :d] [:b :q]] (vec (set/slice e1 [:a :d]   [:b :q]  )))) ; matching subrange
    (is (= [[:a :d] [:b :q]] (vec (set/slice e1 [:a :c]   [:b :r]  )))) ; non-matching subrange
    (is (= [[:b :x]]         (vec (set/slice e1 [:b :r]   [:c nil] )))) ; non-matching -> out of range
    (is (= []                (vec (set/slice e1 [:c nil]  [:c nil] )))) ; totally out of range
    ))

;;; State

(comment 
  (require '[me.tonsky.persistent_sorted_set ANode])
)
