(ns behave.print.subs
  (:require [clojure.string         :as str]
            [re-frame.core          :as rf]
            [re-posh.core           :as rp]
            [string-utils.interface :refer [split-commas-or-spaces]]))

(rf/reg-sub
 :print/matrix-table-multi-valued-inputs
 (fn [[_ ws-uuid]]
   [(rf/subscribe [:worksheet/multi-value-input-uuid+value ws-uuid])
    (rf/subscribe [:worksheet/result-table-units ws-uuid])])

 (fn [[uuid+values units-lookup] _]
   (->> uuid+values
        (map (fn resolve-gv-uuid [[gv-uuid values]]
               (let [var-name          @(rf/subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])
                     discrte-multiple? @(rf/subscribe [:vms/is-group-variable-discrete-multiple? gv-uuid])]
                 [var-name (get units-lookup gv-uuid) gv-uuid (cond->> (split-commas-or-spaces values)
                                                                discrte-multiple?
                                                                (sort-by #(long %)))]))))))

(rf/reg-sub
 :worksheet/matrix-table-data-single-multi-valued-input
 (fn [[_ ws-uuid row-gv-uuid row-values output-gv-uuids]]
   (rf/subscribe [:query
                  '[:find ?i ?j ?value-j
                    :in $ ?ws-uuid ?row-gv-uuid [?i ... ] [?j ...]
                    :where
                    [?w :worksheet/uuid ?ws-uuid]
                    [?w :worksheet/result-table ?rt]
                    [?rt :result-table/rows ?r]

                    ;;get row
                    [?r :result-row/id ?row]

                    ;;get-header
                    [?r :result-row/cells ?c1]
                    [?c1 :result-cell/header ?h1]
                    [?h1 :result-header/group-variable-uuid ?j]
                    [?c1 :result-cell/value ?value-j]

                    [?r :result-row/cells ?c2]
                    [?c2 :result-cell/header ?h2]
                    [?h2 :result-header/group-variable-uuid ?row-gv-uuid]
                    [?c2 :result-cell/value ?i]]
                  [ws-uuid row-gv-uuid row-values output-gv-uuids]]))

 (fn [table-data _]
   (reduce
    (fn [acc [i j value]]
      (assoc acc [(str i) (str j)] value))
    {}
    table-data)))

(rf/reg-sub
 :print/matrix-table-two-multi-valued-inputs
 (fn [[_ ws-uuid row-gv-uuid row-values col-gv-uuid col-values output-gv-uuid]]
   (rf/subscribe [:query
                  '[:find ?i ?j ?value
                    :in $ ?ws-uuid ?row-gv-uuid [?i ... ] ?col-gv-uuid [?j ...] ?output-gv-uuid
                    :where
                    [?w :worksheet/uuid ?ws-uuid]
                    [?w :worksheet/result-table ?rt]
                    [?rt :result-table/rows ?r]

                    ;;get row
                    [?r :result-row/id ?row]

                    ;; get row index i
                    [?r :result-row/cells ?c1]
                    [?c1 :result-cell/header ?h1]
                    [?h1 :result-header/group-variable-uuid ?row-gv-uuid]
                    [?c1 :result-cell/value ?i]


                    ;; get column index j
                    [?r :result-row/cells ?c2]
                    [?c2 :result-cell/header ?h2]
                    [?h2 :result-header/group-variable-uuid ?col-gv-uuid]
                    [?c2 :result-cell/value ?j]

                    ;; get value from i j
                    [?r :result-row/cells ?c3]
                    [?c3 :result-cell/header ?h3]
                    [?h3 :result-header/group-variable-uuid ?output-gv-uuid]
                    [?c3 :result-cell/value ?value]]
                  [ws-uuid row-gv-uuid row-values col-gv-uuid col-values output-gv-uuid]]))

 (fn [table-data _]
   (reduce
    (fn [acc [i j value]]
      (assoc acc [(str i) (str j)] value))
    {}
    table-data)))

(rp/reg-sub
 :worksheet/first-row-results-gv-uuid->value
 (fn [_ [_ ws-uuid gv-uuid]]
   {:type  :query
    :query '[:find ?value .
             :in $ ?ws-uuid ?gv-uuid
             :where
             [?w :worksheet/uuid ?ws-uuid]
             [?w :worksheet/result-table ?rt]
             [?rt :result-table/rows ?r]

             ;;get row
             [?r :result-row/id 0]

             ;;get-header
             [?r :result-row/cells ?c]
             [?c :result-cell/header ?h]
             [?h :result-header/group-variable-uuid ?gv-uuid]
             [?h :result-header/repeat-id ?repeat-id]

             ;;get value
             [?c :result-cell/value ?value]]
    :variables [ws-uuid gv-uuid]}))

(comment
  (require '[goog.string      :as gstring])

  (def ws-uuid "64f9f2b9-d73d-4240-840e-439f4b3efa6b")

  (rf/subscribe [:print/matrix-table-multi-valued-inputs ws-uuid])

  (let [ws-uuid                                     "64f9f2b9-d73d-4240-840e-439f4b3efa6b"
        multi-valued-inputs                         @(rf/subscribe [:print/matrix-table-multi-valued-inputs ws-uuid])
        [row-name row-units row-gv-uuid row-values] (first multi-valued-inputs)
        [col-name col-units col-gv-uuid col-values] (second multi-valued-inputs)
        output-uuids                                @(rf/subscribe [:worksheet/output-uuids-filtered ws-uuid])]

    #_(rf/subscribe [:print/matrix-table-two-multi-valued-inputs ws-uuid
                     row-gv-uuid
                     (str/split row-values ",")
                     col-gv-uuid
                     (str/split col-values ",")
                     (first output-uuids)])
    row-gv-uuid
    (str/split row-values ",")
    col-gv-uuid
    (str/split col-values ",")
    (first output-uuids)
    )

  (rf/subscribe [:worksheet/output-uuids-filtered ws-uuid])

  )
