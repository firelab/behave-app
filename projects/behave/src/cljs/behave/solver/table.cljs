(ns behave.solver.table
  (:require [re-frame.core :as rf]
            [behave.logger :refer [log]]
            [behave.solver.queries :as q]))

;;; Results Table Helpers

(defn- add-table [ws-uuid]
  (rf/dispatch [:worksheet/delete-existing-result-table ws-uuid])
  (rf/dispatch [:worksheet/add-result-table ws-uuid]))

(defn- add-row [ws-uuid row-id]
  (rf/dispatch [:worksheet/add-result-table-row ws-uuid row-id]))

(defn- add-header [ws-uuid gv-id repeat-id units]
  (rf/dispatch [:worksheet/add-result-table-header ws-uuid gv-id repeat-id units]))

(defn- add-cell [ws-uuid row-id gv-id repeat-id value]
  (rf/dispatch [:worksheet/add-result-table-cell ws-uuid row-id gv-id repeat-id (str value)]))

(defn- unit-label [unit-uuid]
  (or (:unit/short-code (q/unit unit-uuid)) ""))

(defn- add-inputs-to-results-table [ws-uuid row-id inputs]
  (doseq [[_ repeats] inputs]
    (cond
      ;; Single Group w/ Single Variable
      (and (= 1 (count repeats)) (= 1 (count (first (vals repeats)))))
      (let [[gv-id [value unit-uuid]] (ffirst (vals repeats))
            units                     (unit-label unit-uuid)]
        (log [:ADDING-INPUT ws-uuid row-id gv-id value units])
        (add-header ws-uuid gv-id 0 units)
        (add-cell ws-uuid row-id gv-id 0 value))

      ;; Multiple Groups w/ Single Variable
      (every? #(= 1 (count %)) (vals repeats))
      (doseq [[repeat-id [_ repeat-group]] (map list repeats (range (count repeats)))]
        (let [[gv-id [value unit-uuid]] (first repeat-group)
            units                     (unit-label unit-uuid)]
          (log [:ADDING-INPUT ws-uuid row-id gv-id value units])
          (add-header ws-uuid gv-id repeat-id units)
          (add-cell ws-uuid row-id gv-id repeat-id value)))

      ;; Multiple Groups w/ Multiple Variables
      :else
      (doseq [[[_ repeat-group] repeat-id] (map list repeats (range (count repeats)))]
        (doseq [[gv-id [value unit-uuid]] repeat-group]
          (let [units (unit-label unit-uuid)]
            (log [:ADDING-INPUT ws-uuid row-id gv-id value units])
            (add-header ws-uuid gv-id repeat-id units)
            (add-cell ws-uuid row-id gv-id repeat-id value)))))))

(defn add-outputs-to-results-table [ws-uuid row-id outputs]
  (doseq [[gv-id [value unit-uuid]] outputs]
    (let [units (unit-label unit-uuid)]
      (log [:ADDING-OUTPUT ws-uuid row-id gv-id value units])
      (add-header ws-uuid gv-id 0 units)
      (add-cell ws-uuid row-id gv-id 0 value))))

(defn add-to-results-table [results ws-uuid]
  (add-table ws-uuid)
  (doseq [{:keys [row-id inputs outputs]} results]
    (add-row ws-uuid row-id)
    (add-inputs-to-results-table ws-uuid row-id inputs)
    (add-outputs-to-results-table ws-uuid row-id outputs)))
