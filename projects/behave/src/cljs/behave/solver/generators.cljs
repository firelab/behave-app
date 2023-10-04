(ns behave.solver.generators
  (:require
   [clojure.string :as str]
   [clojure.walk   :as w]))

;;; Helpers

(defn- csv? [s] (< 1 (count (str/split s #","))))

;;; Run Generation

(defn permutations
  "Given collection of `range-inputs` (up to 3),
   generates all permutations of the `range-inputs`
   and combine them with the `single-inputs`."
  [single-inputs range-inputs]
  (case (count range-inputs)
    0 [single-inputs]
    1 (for [x (first range-inputs)]
        (conj single-inputs x))
    2 (for [x (first range-inputs)
            y (second range-inputs)]
        (conj single-inputs x y))
    3 (for [x (first range-inputs)
            y (second range-inputs)
            z (nth range-inputs 2)]
        (conj single-inputs x y z))))

(defn ->run-plan
  "Creates a deeply nested map given an `inputs-vector` of the form:
   `[[group-uuid repeat-id group-variable-uuid value unit] ...]`"
  [inputs-vec]
  (reduce (fn [acc [group-uuid repeat-id group-var-uuid value unit]]
            (let [leaf [value (if (= :none unit) nil unit)]]
              (assoc-in acc [group-uuid repeat-id group-var-uuid] leaf)))
          {}
          inputs-vec))

(defn generate-runs
  "Given a raw `inputs-vector` (typically from the
  `:worksheet/all-inputs+unit-vector` sub), creates permutations of
  range inputs and transforms that collection into a deeply nested map.

  Example:
  `(generate-runs [[:group-uuid :repeat-id :group-variable-uuid \"value1, value2\" :unit] ...])`

  would return:
  ```clojure
  [{:group-uuid
   {:repeat-id
    {:group-variable-uuid [\"value1\" :unit]}}}
  [{:group-uuid
   {:repeat-id
    {:group-variable-uuid [\"value2\" :unit]}}}]
  ```"
  [all-inputs-vector]
  (let [empty-or-csv?          #(or (empty? %) (csv? %))
        single-inputs          (remove #(-> % (reverse) (second) (empty-or-csv?)) all-inputs-vector)
        range-inputs           (filter #(-> % (reverse) (second) (csv?)) all-inputs-vector)
        separated-range-inputs (map #(let [result (vec (drop-last 2 %))
                                           units  (last %)
                                           values (-> % (reverse) (second))
                                           values (map str/trim (str/split values #","))]
                                       (mapv (fn [v] (concat result [v units])) values)) range-inputs)]

    (mapv ->run-plan (permutations (vec single-inputs) separated-range-inputs))))

(defn depth
  ([data] (depth data 0))
  ([node level]
   (cond
     (map? node)
     (apply max (map (fn [[_ v]] (depth v (inc level))) node))

     (vector? node)
     (apply max (map (fn [v] (depth v (inc level))) node))

     :else
     level)))

(defn inputs-map-to-vector
  "Transforms a deeply nested map back into a vector"
  [inputs]
  (let [inputs-depth (depth inputs)]
    (->> inputs
         (w/postwalk (fn [x] (if (map? x) (vec x) x)))
         (mapv (fn [v]
                 (let [l    (flatten v)
                       head (first l)
                       body (partition inputs-depth (rest l))]
                   (mapv #(into [head] %) body))))
         (reduce (fn [acc curr] (concat acc curr)) [])
         (vec))))

(comment
  (def inputs {:G1 {0 {:GV ["30" 0]}}})
  (depth inputs)

  (inputs-map-to-vector
   {:G1 {0 {:GV ["30" 0]}
         1 {:GV ["50" 1]}}
    :G2 {0 {:GV2 ["2" nil]}}}) [[:G1 0 :GV "30" 0] [:G1 1 :GV "50" 1] [:G2 0 :GV2 "2" nil]];

  )
