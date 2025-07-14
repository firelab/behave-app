(ns migrations.2025-06-30-update-mortality-pivot-table
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave.schema.pivot-table :refer [schema]]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; 1. Re-creates :pivot-column/order as type `long`
;; 2. Renames the Pivot Table to "Equation Type"
;; 3. Rename "CVSorCLS" to "CVS or CLS"
;; 4. Change the column order to: Mortality Tree Species, Equation Type, and “CVS or CLS” (with spaces)
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(defn find-eid
  "Finds an entity ID using the attribute (`a`) and
   value (`v`) specified."
  [conn a v]
  (d/q '[:find ?e .
         :in $ ?a ?v
         :where [?e ?a ?v]]
       (d/db conn) a v))

(defn find-eid-by
  "Finds an entity ID using `attr` from map `m`."
  [conn attr m]
  {:pre [(map? m)]}
  (find-eid conn attr (get m attr)))

;;; Re-create :pivot-column/order as `long`
#_{:clj-kondo/ignore [:missing-docstring]}
(def pivot-column-order-attr-eid (find-eid conn :db/ident :pivot-column/order))

#_{:clj-kondo/ignore [:missing-docstring]}
(def pivot-table-eid
  (find-eid conn :pivot-table/title "CVSorCLS"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def cvs-or-cls-variable-eid
  (find-eid conn :variable/name "CVSorCLS"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def cvs-or-cls-group-variable
  (sm/t-key->uuid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:cvsorcls"))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def deprecate-old-pivot-column-order-payload
  [{:db/id    pivot-column-order-attr-eid
    :db/ident :pivot-table/deprecated-order-type-string
    :db/doc   "Depreacted Pivot Column's order (type string)"}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-new-pivot-column-order-payload
  (filter #(= :pivot-column/order (:db/ident %)) schema))

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-pivot-table-payload
  {:db/id             pivot-table-eid  
   :pivot-table/title "Equation Type"})

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-cvs-or-cls-variable-payload
  {:db/id              cvs-or-cls-variable-eid
   :variable/name      "CVS or CLS"
   :variable/bp6-code  "vCVSorCLS"
   :variable/bp6-label "CVS or CLS"})

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-cvs-or-cls-translation-payload
  (sm/update-translations-payload
   conn
   "en-US"
   {"behaveplus:mortality:output:tree_mortality:tree_mortality:cvsorcls" "CSV or CLS"}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def pivot-column-order-payload
  (->> [{:pivot-column/group-variable-uuid (sm/t-key->uuid conn "behaveplus:mortality:input:fuelvegetation_overstory:mortality_tree_species:mortality_tree_species")
         :pivot-column/order               0}
        {:pivot-column/group-variable-uuid (sm/t-key->uuid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:mortality-equation")
         :pivot-column/order               1}
        {:pivot-column/group-variable-uuid (sm/t-key->uuid conn "behaveplus:mortality:output:tree_mortality:tree_mortality:cvsorcls")
         :pivot-column/order               2}]
       (mapv #(assoc % :db/id (find-eid-by conn :pivot-column/group-variable-uuid %)))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat [update-pivot-table-payload update-cvs-or-cls-variable-payload]
                     update-cvs-or-cls-translation-payload
                     pivot-column-order-payload))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try
    (def tx-data-1 @(d/transact conn deprecate-old-pivot-column-order-payload))
    (def tx-data-2 @(d/transact conn add-new-pivot-column-order-payload))
    (def tx-data-3 @(d/transact conn payload))
    (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-data-1)
    (sm/rollback-tx! conn @tx-data-2)))

