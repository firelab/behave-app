(ns migrations.2025-06-23-rename-tools-to-calculators
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; 1. Rename "Tools" to "Calculators":
;;   - Main menu should say Calculators & Settings
;;   - Button should say Calculators (plural)
;;   - Header in the selection window should say Select a Calculator
;;   - Close button says Close Calculator
;;
;; 2. Rename tool names for clarity:
;;   - Ignite → Probability of Ignition
;;   - 1-HR Fuel Moisture → 1-Hour Fuel Moisture
;;   - Slope Tool → Slope from Map Measurements
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

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-translations
  (sm/build-translations-payload
   conn
   100
   {"behaveplus:calculators"              "Calculators"
    "behaveplus:calculators_and_settings" "Calculators & Settings"
    "behaveplus:close_calculator"         "Close Calculator"
    "behaveplus:select_calculator"        "Select a Calculator"
    "behaveplus:select"                   "Select..."}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def rename-tools-payload
  [{:bp/nid    "8F_7EmeSVZ5LWVW5D37XQ"
    :tool/name "Probability of Ignition"}
   {:bp/nid    "SuQVp1yhgrpTme2PZ8yxr"
    :tool/name "1-Hour Fuel Moisture"}
   {:bp/nid    "0M1P3z24kkvsTBYrSgsRf"
    :tool/name "Slope from Map Measurements"}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat rename-tools-payload new-translations))

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
  (sm/rollback-tx! conn @tx-data))

