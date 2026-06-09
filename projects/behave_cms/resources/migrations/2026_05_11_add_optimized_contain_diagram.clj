(ns ^{:migrate/ignore? true} migrations.2026-05-11-add-optimized-contain-diagram
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
;; 2. Backfills :diagram/title + :diagram/title-translation-key + :diagram/show-quadrant-N?
;;    onto the 3 existing diagram entities (:contain, :fire-shape, :wind-slope-spread-direction).
;;    All four quadrant flags are set to true (full symmetric rendering, matching prior behavior).
;;
;; 3. Seeds English translations for all 6 new translation keys.
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def contain-mode-gv-uuid
  (sm/t-key->bp-uuid conn "behaveplus:contain:input:suppression:contain_mode:contain_mode"))

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
;; Payload — Optimized Contain Diagram group variable (new entity)
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def optimized-contain-diagram-variable
  (sm/->variable conn {:db/id -1
                       :nname "Optimized Contain Diagram"
                       :kind  :continuous}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def optimized-contain-diagram-gv
  (sm/->group-variable
   conn
   {:db/id              -2
    :parent-group-eid   (sm/t-key->eid conn "behaveplus:contain:output:fire:containment")
    :variable-eid       -1
    :order              7
    :translation-key    "behaveplus:contain:output:fire:containment:optimized-contain-diagram"
    :conditionally-set? true
    :hide-result?       true
    :actions            [{:nname        "Set to True when Contain Mode is Compute with Optiomal Resource"
                          :ttype        :select
                          :target-value "true"
                          :conditionals [{:ttype               :group-variable
                                          :operator            :equal
                                          :values              "1"
                                          :group-variable-uuid contain-mode-gv-uuid}]}]}))

;; ===========================================================================================================
;; Payload — Optimized Contain Diagram (new entity)
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def optimized-contain-payload
  [{:db/id           (sm/t-key->eid conn "behaveplus:contain")
    :module/diagrams [{:diagram/type                         :optimized-contain
                       :diagram/group-variable               -2
                       :diagram/title                        "Production Rate vs Containment Area"
                       :diagram/title-translation-key        "behaveplus:contain:diagrams:optimized-contain:title"
                       :diagram/x-axis-title                 "Production Rate"
                       :diagram/x-axis-title-translation-key "behaveplus:contain:diagrams:optimized-contain:x-axis-title"
                       :diagram/y-axis-title                 "Containment Area"
                       :diagram/y-axis-title-translation-key "behaveplus:contain:diagrams:optimized-contain:y-axis-title"
                       :diagram/x-units-uuid                 (short-code->uuid "ch/h")
                       :diagram/y-units-uuid                 (short-code->uuid "ac")
                       :diagram/show-quadrant-1?             true
                       :diagram/show-quadrant-2?             false
                       :diagram/show-quadrant-3?             false
                       :diagram/show-quadrant-4?             false
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
    :diagram/show-quadrant-1?      true
    :diagram/show-quadrant-2?      true
    :diagram/show-quadrant-3?      true
    :diagram/show-quadrant-4?      true
    :diagram/x-units-uuid          (short-code->uuid "ch")
    :diagram/y-units-uuid          (short-code->uuid "ch")}
   {:db/id                         fire-shape-diagram-eid
    :diagram/title                 "Fire Shape"
    :diagram/title-translation-key "behaveplus:surface:diagrams:fire-shape:title"
    :diagram/show-quadrant-1?      true
    :diagram/show-quadrant-2?      true
    :diagram/show-quadrant-3?      true
    :diagram/show-quadrant-4?      true
    :diagram/x-units-uuid          (short-code->uuid "ch")
    :diagram/y-units-uuid          (short-code->uuid "ch")}
   {:db/id                         wind-slope-diagram-eid
    :diagram/title                 "Wind/Slope/Spread Direction"
    :diagram/title-translation-key "behaveplus:surface:diagrams:wind-slope-spread-direction:title"
    :diagram/show-quadrant-1?      true
    :diagram/show-quadrant-2?      true
    :diagram/show-quadrant-3?      true
    :diagram/show-quadrant-4?      true
    :diagram/x-units-uuid          (short-code->uuid "ch/h")
    :diagram/y-units-uuid          (short-code->uuid "ch/h")}])

;; ===========================================================================================================
;; Payload — Seed English translations for all 6 new translation keys
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   {"behaveplus:contain:output:fire:containment:optimized-contain-diagram" "Optimized Contain Diagram"
    "behaveplus:contain:diagrams:optimized-contain:title"                  "Production Rate vs Containment Area"
    "behaveplus:contain:diagrams:optimized-contain:x-axis-title"           "Production Rate"
    "behaveplus:contain:diagrams:optimized-contain:y-axis-title"           "Containment Area"
    "behaveplus:contain:diagrams:contain:title"                            "Containment"
    "behaveplus:surface:diagrams:fire-shape:title"                         "Fire Shape"
    "behaveplus:surface:diagrams:wind-slope-spread-direction:title"        "Wind/Slope/Spread Direction"}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat [optimized-contain-diagram-variable
           optimized-contain-diagram-gv]
          optimized-contain-payload
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
