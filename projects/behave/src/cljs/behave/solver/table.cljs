(ns behave.solver.table
  (:require [re-frame.core :as rf]
            [behave.logger :refer [log]]
            [behave.solver.queries :as q]))

;;; Results Table Helpers

(defn- ->table [tempid ws-uuid]
  {:db/id                   tempid
   :worksheet/_result-table [:worksheet/uuid ws-uuid]})

(defn- ->row [tempid table-eid row-id cells]
  {:db/id              tempid
   :result-table/_rows table-eid
   :result-row/id      row-id
   :result-row/cells   cells})

(defn- ->header [tempid table-eid gv-uuid repeat-id units]
  {:db/id                             tempid
   :result-table/_headers             table-eid
   :result-header/group-variable-uuid gv-uuid
   :result-header/repeat-id           repeat-id
   :result-header/units               units})

(defn- ->cell [header-eid value]
  {:result-cell/header header-eid
   :result-cell/value  value})

(defn- unit-label [unit-uuid]
  (or (:unit/short-code (q/unit unit-uuid)) ""))

(defn- map-result-headers [{:keys [inputs outputs]}]
  (let [headers (atom [])]
    (doseq [[_ repeats] inputs]
      (cond
        ;; Single Group w/ Single Variable
        (and (= 1 (count repeats)) (= 1 (count (first (vals repeats)))))
        (let [[gv-id [_value unit-uuid]] (ffirst (vals repeats))
              units                     (unit-label unit-uuid)]
          (swap! headers conj [gv-id 0 units]))

        ;; Multiple Groups w/ Single Variable
        (every? #(= 1 (count %)) (vals repeats))
        (for [[repeat-id [_ repeat-group]] (map list repeats (range (count repeats)))]
          (let [[gv-id [_value unit-uuid]] (first repeat-group)
                units                     (unit-label unit-uuid)]
            (swap! headers conj [gv-id repeat-id units])))

        ;; Multiple Groups w/ Multiple Variables
        :else
        (for [[[_ repeat-group] repeat-id] (map list repeats (range (count repeats)))]
          (doseq [[gv-id [_value unit-uuid]] repeat-group]
            (let [units (unit-label unit-uuid)]
              (swap! headers conj [gv-id repeat-id units]))))))
    (concat @headers
            (mapv (fn [[gv-id [_ unit-uuid]]]
                    (let [units (unit-label unit-uuid)]
                      [gv-id 0 units]))
                  outputs))))

(defn- map-outputs-to-cells [header-map outputs]
  (map (fn [[gv-id [value unit-uuid]] ]
         (let [units      (unit-label unit-uuid)
               header-eid (get header-map [gv-id 0 units])]
           (->cell header-eid value)))
       outputs))

(defn- map-inputs-to-cells [header-map inputs]
  (let [cells (atom [])]
    (doseq [[_ repeats] inputs]
      (cond
        ;; Single Group w/ Single Variable
        (and (= 1 (count repeats)) (= 1 (count (first (vals repeats)))))
        (let [[gv-id [value unit-uuid]] (ffirst (vals repeats))
              units                     (unit-label unit-uuid)
              header-eid                (get header-map [gv-id 0 units])]
          (swap! cells conj (->cell header-eid value)))

        ;; Multiple Groups w/ Single Variable
        (every? #(= 1 (count %)) (vals repeats))
        (doseq [[repeat-id [_ repeat-group]] (map list repeats (range (count repeats)))]
          (let [[gv-id [value unit-uuid]] (first repeat-group)
                units                     (unit-label unit-uuid)
                header-eid                (get header-map [gv-id repeat-id units])]
            (swap! cells conj (->cell header-eid value))))

        ;; Multiple Groups w/ Multiple Variables
        :else
        (doseq [[[_ repeat-group] repeat-id] (map list repeats (range (count repeats)))]
          (doseq [[gv-id [value unit-uuid]] repeat-group]
            (let [units      (unit-label unit-uuid)
                  header-eid (get header-map [gv-id repeat-id units])]
              (swap! cells conj (->cell header-eid value)))))))
    @cells))

(defn add-to-results-table
  "Adds results to worksheet as a table."
  [results ws-uuid]
  (let [tempid      (atom -1)
        table       (->table (swap! tempid dec) ws-uuid)
        table-eid   (:db/id table)
        headers-map (reduce (fn [m header-key]
                              (assoc m header-key (swap! tempid dec)))
                            {}
                            (map-result-headers (first results)))
        headers     (map (fn [[[gv-uuid repeat-id units] id]]
                           (->header id table-eid gv-uuid repeat-id units)) headers-map)
        rows        (map 
                     (fn [{:keys [row-id inputs outputs]}]
                       (let [row-eid      (swap! tempid dec)
                             input-cells  (map-inputs-to-cells headers-map inputs)
                             output-cells (map-outputs-to-cells headers-map outputs)]
                         (->row row-eid table-eid row-id (concat input-cells output-cells)))) results)]

    ;; Transact all at once
    (rf/dispatch [:ds/transact-many (concat [table] headers rows)])))
