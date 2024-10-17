(ns migrations.2024-10-17-add-input-group-translations
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave.schema.rules :refer [vms-rules]]
            [behave-cms.server :as cms]
            [cms-import :refer [add-export-file-to-conn]]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(def payload
  (->> (d/q '[:find ?t-key ?name
              :in $ %
              :where
              [?e :group/translation-key ?t-key]
              [?e :group/name ?name]]
            (d/db conn)
            vms-rules)
       (reduce (fn [acc [t-key name]]
                 (assoc acc t-key name))
               {})
       (sm/build-translations-payload conn)))

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
