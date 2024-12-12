(ns migrations.2024-11-05-fix-min-max
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [csv-parser.interface :refer [fetch-csv]]
            [map-utils.interface :refer [index-by]]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(d/q '[:find ?e
       :where [?e :db/ident :variable/bp6-code]]
     (d/db conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def bp6-code->eids
  (index-by :key 
            (d/q '[:find ?e ?v
                   :keys :eid :key
                   :where [?e :variable/bp6-code ?v]]
                 (d/db conn))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def vars-min-max (fetch-csv "cms-exports/vars_min_max.csv"))

#_{:clj-kondo/ignore [:missing-docstring :shadowed-var]}
(def payload (map (fn [{:keys [key min max]}]
               (-> {:db/id (get-in bp6-code->eids [key :eid])}        
                   (assoc :variable/maximum (-> max (str/replace #"," "") parse-double))
                   (assoc :variable/minimum (-> max (str/replace #"," "") parse-double))))
             vars-min-max))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
