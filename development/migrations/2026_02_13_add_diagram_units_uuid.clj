(ns ^{:migrate/ignore? true} migrations.2026-02-13-add-diagram-units-uuid
  (:require [schema-migrate.interface :as sm]
            [datomic.api              :as d]
            [behave-cms.store         :refer [default-conn]]
            [behave-cms.server        :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Adds :diagram/units-uuid to existing diagram entities.

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

(defn short-code->uuid
  "Given a unit's short-code (e.g. \"ch\", \"ft\"), returns the :bp/uuid for that unit."
  [short-code]
  (d/q '[:find ?uuid .
         :in $ ?sc
         :where
         [?e :unit/short-code ?sc]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       short-code))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Map of diagram type -> unit short-code to assign.
;; Update these short-codes as needed.
(def diagram-type->unit-short-code
  {:contain                     "ch"
   :fire-shape                  "ch"
   :wind-slope-spread-direction "ch/h"})

(def diagram-entities
  (d/q '[:find ?e ?type
         :in $
         :where
         [?e :diagram/type ?type]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (vec (for [[eid dtype] diagram-entities
             :let        [short-code (get diagram-type->unit-short-code dtype)
                          uuid (when short-code (short-code->uuid short-code))]
             :when       uuid]
         {:db/id              eid
          :diagram/units-uuid uuid})))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
