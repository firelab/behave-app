(ns migrations.2026-10-15-heading-fireline-intensity-result-label
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; The results-page label for the Surface Heading Fireline Intensity output reads "Fireline Intensity",
;; while every other directional output carries its direction prefix (e.g. "Heading Rate of Spread",
;; "Heading Flame Length", "Flanking Fireline Intensity"). Rename it to "Heading Fireline Intensity".

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (sm/update-translations-payload
   db
   "en-US"
   {"behaveplus:surface:output:result:fire_behavior:surface_fire:heading_fireline_intensity" "Heading Fireline Intensity"}))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms])
  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
