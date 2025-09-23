(ns migrations.template
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave.schema.core :refer [rules]]

            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def surface-gv-eids-to-proccess
  (d/q '[:find [?gv ...]
         :in $ %
         :where
         [?v :variable/group-variables ?gv]
         [?v :variable/kind :continuous]
         (module-input-vars ?m ?gv)]
       (d/db conn)
       rules))

(def surface-multi-discrtee-gv-eids-to-proccess
  (d/q '[:find [?gv ...]
         :in $ %
         :where
         [?v :variable/group-variables ?gv]
         [?gv :group-variable/discrete-multiple? true]
         [?v :variable/kind :discrete]
         (module-input-vars ?m ?gv)]
       (d/db conn)
       rules))

(def contain-gv-eids-to-process
  [(sm/t-key->eid conn "behaveplus:contain:output:fire:containment:fireline_constructed")
   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:final-production-rate")
   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:time_from_report")
   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:contained_area")
   (sm/t-key->eid conn "behaveplus:contain:output:fire:fire_size___at_resource_arrival_time:fire_area___at_resource_arrival_time")])

(count surface-gv-eids-to-proccess)

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat
   (mapv
    (fn [eid]
      {:db/id                                                          eid
       :group-variable/disable-multi-valued-input-conditional-operator :and
       :group-variable/disable-multi-valued-input-conditionals         [(sm/->conditional conn {:ttype    :group-variable
                                                                                                :operator :equal
                                                                                                :values   "1"
                                                                                                :group-variable-uuid
                                                                                                (:bp/uuid (sm/t-key->entity conn "behaveplus:contain:input:suppression:contain_mode:contain_mode"))})]})
    (concat surface-gv-eids-to-proccess surface-multi-discrtee-gv-eids-to-proccess))

   (mapv
    (fn [eid]
      {:db/id                                                          eid
       :group-variable/disable-multi-valued-input-conditional-operator :and
       :group-variable/disable-multi-valued-input-conditionals         [(sm/->conditional conn {:ttype    :group-variable
                                                                                                :operator :equal
                                                                                                :values   "1"
                                                                                                :group-variable-uuid
                                                                                                (:bp/uuid (sm/t-key->entity conn "behaveplus:contain:input:suppression:contain_mode:contain_mode"))})]
       })
    contain-gv-eids-to-process)
   ))


;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
