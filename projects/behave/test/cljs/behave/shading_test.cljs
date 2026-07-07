(ns behave.shading-test
  (:require [behave.results.shading :as shading]
            [cljs.test              :refer [deftest is testing]]))

;; A spread-rate output with an enabled filter range of [5 10].
(def filters
  (shading/filters-by-uuid
   [["spread-rate"   5   10  true]
    ["flame-length"  2   4   false]]))  ;; present but disabled

(deftest out-of-range?-test
  (testing "value inside an enabled range is in-range"
    (is (false? (shading/out-of-range? filters {} "spread-rate" 7))))

  (testing "value outside an enabled range is out-of-range"
    (is (true? (shading/out-of-range? filters {} "spread-rate" 12)))
    (is (true? (shading/out-of-range? filters {} "spread-rate" 1))))

  (testing "boundary values are inclusive"
    (is (false? (shading/out-of-range? filters {} "spread-rate" 5)))
    (is (false? (shading/out-of-range? filters {} "spread-rate" 10))))

  (testing "disabled filter never shades"
    (is (false? (shading/out-of-range? filters {} "flame-length" 99))))

  (testing "output with no filter never shades"
    (is (false? (shading/out-of-range? filters {} "reaction-intensity" 99))))

  (testing "rounding to significant digits honors the displayed value"
    ;; 10.4 displays as "10" at 0 sig digits, so it should read as in-range.
    (is (false? (shading/out-of-range? filters {"spread-rate" 0} "spread-rate" 10.4)))
    ;; ...but as out-of-range when shown to one decimal place.
    (is (true? (shading/out-of-range? filters {"spread-rate" 1} "spread-rate" 10.4))))

  (testing "numeric strings are accepted (matrix data arrives as strings)"
    (is (true? (shading/out-of-range? filters {} "spread-rate" "12")))))

(deftest shade-set-1d-test
  (testing "collects row positions whose output value is out of range"
    (let [cells [["row-a" "spread-rate" 7]    ;; in range
                 ["row-b" "spread-rate" 12]   ;; out
                 ["row-c" "spread-rate" 3]]]  ;; out
      (is (= #{"row-b" "row-c"}
             (shading/shade-set filters {} cells))))))

(deftest shade-set-2d-test
  (testing "collects [row col] positions and unions across outputs"
    (let [cells [[[0 0] "spread-rate"  7]    ;; in
                 [[0 1] "spread-rate"  20]   ;; out
                 [[1 0] "flame-length" 99]]] ;; disabled filter -> in
      (is (= #{[0 1]}
             (shading/shade-set filters {} cells))))))

(deftest filters-by-uuid-test
  (testing "indexes a table-settings-filters seq by group-variable uuid"
    (is (= {"a" ["a" 1 2 true] "b" ["b" 3 4 false]}
           (shading/filters-by-uuid [["a" 1 2 true] ["b" 3 4 false]])))))

;; A directional output whose Heading/Backing children share the same [5 10]
;; filter (filters fan out from the parent to its children identically).
(def directional-filters
  (shading/filters-by-uuid
   [["heading-sr" 5 10 true]
    ["backing-sr" 5 10 true]]))

(deftest shade-set-respects-scope-test
  (testing "each direction's shade-set reflects only that direction's cells"
    (let [heading-cells [["r1" "heading-sr" 7]     ;; in
                         ["r2" "heading-sr" 99]]   ;; out
          backing-cells [["r1" "backing-sr" 99]    ;; out
                         ["r2" "backing-sr" 7]]]   ;; in
      ;; Heading scope shades r2 only; Backing's out-of-range r1 does not leak in.
      (is (= #{"r2"} (shading/shade-set directional-filters {} heading-cells)))
      ;; Backing scope shades r1 only; independent of Heading.
      (is (= #{"r1"} (shading/shade-set directional-filters {} backing-cells)))
      ;; The old "across all runs" behavior (one combined set) shaded both — this
      ;; is exactly what direction-scoping replaces.
      (is (= #{"r1" "r2"}
             (shading/shade-set directional-filters {} (concat heading-cells backing-cells)))))))

(deftest shade-set-non-directional-relies-on-heading-test
  ;; The non-directional table scopes its cells to its own outputs + the Heading
  ;; children (done in the view/sub). These cases show why that scope is correct:
  ;; Heading drives shading and Backing — if it were passed — would not.
  (testing "non-directional scope = own outputs + Heading children"
    (let [f       (shading/filters-by-uuid
                   [["rxn-intensity" 5 10 true]
                    ["heading-sr"    5 10 true]
                    ["backing-sr"    5 10 true]])
          non-dir [["r1" "rxn-intensity" 7]]    ;; in range
          heading [["r1" "heading-sr"    99]]   ;; out of range
          backing [["r1" "backing-sr"    99]]]  ;; out of range
      ;; Non-directional + Heading: the Heading out-of-range value shades the row.
      (is (= #{"r1"} (shading/shade-set f {} (concat non-dir heading))))
      ;; Non-directional outputs alone are all in range — nothing shaded.
      (is (= #{} (shading/shade-set f {} non-dir)))
      ;; Backing WOULD shade if scoped in — which is exactly why the view excludes
      ;; it from the non-directional table.
      (is (= #{"r1"} (shading/shade-set f {} backing))))))
