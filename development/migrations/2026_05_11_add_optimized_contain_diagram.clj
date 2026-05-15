(ns migrations.2026-05-11-add-optimized-contain-diagram
  (:require [behave-cms.server        :as cms]
            [behave-cms.store         :refer [default-conn]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;;
;; 1. Adds a new :optimized-contain diagram type to the contain module.
;;    This diagram displays production rate vs containment area as a scatter plot,
;;    populated by the new C++ sweep in doContainRunWithOptimalResource.
;;
;; 2. Backfills :diagram/title + :diagram/title-translation-key onto the 3 existing
;;    diagram entities (:contain, :fire-shape, :wind-slope-spread-direction) — these
;;    attrs are new; the titles were previously hardcoded in the app.
;;
;; 3. Seeds English translations for all 6 new translation keys.
;; ===========================================================================================================

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
  "Given a unit's short-code (e.g. \"ch\", \"ch/h\", \"ac\"), returns the :bp/uuid for that unit."
  [short-code]
  (d/q '[:find ?uuid .
         :in $ ?sc
         :where
         [?e :unit/short-code ?sc]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       short-code))

;; ===========================================================================================================
;; Payload — Optimized Contain Diagram (new entity)
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def optimized-contain-payload
  [{:db/id           (sm/t-key->eid conn "behaveplus:contain")
    :module/diagrams [{:diagram/type                         :optimized-contain
                       :diagram/group-variable               (sm/t-key->eid conn "behaveplus:contain:output:fire:containment:autocomputed_resource_production_rate")
                       :diagram/title                        "Production Rate vs Containment Area"
                       :diagram/title-translation-key        "behaveplus:contain:diagrams:optimized-contain:title"
                       :diagram/x-axis-title                 "Production Rate"
                       :diagram/x-axis-title-translation-key "behaveplus:contain:diagrams:optimized-contain:x-axis-title"
                       :diagram/y-axis-title                 "Containment Area"
                       :diagram/y-axis-title-translation-key "behaveplus:contain:diagrams:optimized-contain:y-axis-title"
                       :diagram/x-units-uuid                 (short-code->uuid "ch/h")
                       :diagram/y-units-uuid                 (short-code->uuid "ac")
                       :diagram/symmetric-axes?              false
                       :diagram/mirror-y?                    false
                       :diagram/connect-points?              true}]}])

;; ===========================================================================================================
;; Payload — Backfill existing diagrams with title + title-translation-key
;;
;; NOTE: verify parent module t-key namespaces before transacting.
;;   - :contain lives under behaveplus:contain  → use "behaveplus:contain:diagrams:..."
;;   - :fire-shape lives under behaveplus:surface → use "behaveplus:surface:diagrams:..."
;;   - :wind-slope-spread-direction lives under behaveplus:surface → same
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def contain-diagram-eid
  (d/q '[:find ?e . :in $ :where [?e :diagram/type :contain]] (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def fire-shape-diagram-eid
  (d/q '[:find ?e . :in $ :where [?e :diagram/type :fire-shape]] (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-slope-diagram-eid
  (d/q '[:find ?e . :in $ :where [?e :diagram/type :wind-slope-spread-direction]] (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def legacy-diagram-payload
  [{:db/id                         contain-diagram-eid
    :diagram/title                 "Containment"
    :diagram/title-translation-key "behaveplus:contain:diagrams:contain:title"
    :diagram/symmetric-axes?       true
    :diagram/mirror-y?             true
    :diagram/x-units-uuid          (short-code->uuid "ch")
    :diagram/y-units-uuid          (short-code->uuid "ch")}
   {:db/id                         fire-shape-diagram-eid
    :diagram/title                 "Fire Shape"
    :diagram/title-translation-key "behaveplus:surface:diagrams:fire-shape:title"
    :diagram/symmetric-axes?       true
    :diagram/mirror-y?             true
    :diagram/x-units-uuid          (short-code->uuid "ch")
    :diagram/y-units-uuid          (short-code->uuid "ch")}
   {:db/id                         wind-slope-diagram-eid
    :diagram/title                 "Wind/Slope/Spread Direction"
    :diagram/title-translation-key "behaveplus:surface:diagrams:wind-slope-spread-direction:title"
    :diagram/symmetric-axes?       true
    :diagram/mirror-y?             true
    :diagram/x-units-uuid          (short-code->uuid "ch/h")
    :diagram/y-units-uuid          (short-code->uuid "ch/h")}])

;; ===========================================================================================================
;; Payload — Seed English translations for all 6 new translation keys
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   {"behaveplus:contain:diagrams:optimized-contain:title"           "Production Rate vs Containment Area"
    "behaveplus:contain:diagrams:optimized-contain:x-axis-title"    "Production Rate"
    "behaveplus:contain:diagrams:optimized-contain:y-axis-title"    "Containment Area"
    "behaveplus:contain:diagrams:contain:title"                     "Containment"
    "behaveplus:surface:diagrams:fire-shape:title"                  "Fire Shape"
    "behaveplus:surface:diagrams:wind-slope-spread-direction:title" "Wind/Slope/Spread Direction"}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat optimized-contain-payload
          legacy-diagram-payload
          translations-payload))

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
