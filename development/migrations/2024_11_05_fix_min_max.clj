(ns migrations.2024-11-05-fix-min-max
  (:require [schema-migrate.interface :as sm]
            [clojure.string :as str]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [csv-parser.interface :refer [fetch-csv]]
            [map-utils.interface :refer [index-by]]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================


#_{:clj-kondo/ignore [:missing-docstring :shadowed-var]}
(do

  (cms/init-db!)

  (def conn (default-conn))

  ;;; Remove existing min/max/default_value
  (def vars
    (d/q '[:find [?e ...]
           :where [?e :variable/maximum ?n]]
         (d/db conn)))

  (def retract-existing-payload
    (apply concat
           (map (fn [eid]
                  [[:db/retract eid :variable/default-value]
                   [:db/retract eid :variable/maximum]
                   [:db/retract eid :variable/minimum]]) vars)))

  ;;; Add new variable min/max
  (def bp6-code->eids
    (index-by :key
              (d/q '[:find ?e ?v
                     :keys :eid :key
                     :where [?e :variable/bp6-code ?v]]
                   (d/db conn))))

  (def vars-min-max (fetch-csv "cms-exports/vars_min_max.csv"))

  (def new-min-max-payload
    (map (fn [{:keys [key min max]}]
           (-> {:db/id (get-in bp6-code->eids [key :eid])}
               (assoc :variable/maximum (-> max (str/replace #"," "") parse-double))
               (assoc :variable/minimum (-> min (str/replace #"," "") parse-double))))
         vars-min-max)))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (do
    (def tx-1 (d/transact conn retract-existing-payload))
    (def tx-2 (d/transact conn new-min-max-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-1)
    (sm/rollback-tx! conn @tx-2)))
