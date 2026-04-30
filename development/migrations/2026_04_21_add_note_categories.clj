(ns migrations.2026-04-21-add-note-categories
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================
;; [BHP1-1480] Note Categories Editor — seed initial note categories and
;; translations for the new note-form Cancel button and Delete-note confirm.
;;
;; Categories with `:note-category/modules` set to a specific module combination
;; are shown only on worksheets whose module set matches exactly (see
;; `projects/behave/src/cljs/behave/wizard/subs.cljs :wizard/note-categories`).
;; Shared categories (Fuel Model, Fuel Moisture, Weather, Topography, Fire
;; Behavior) omit `:note-category/modules` so they appear on every worksheet.
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Lookups
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def bp-app-eid
  (d/q '[:find ?e .
         :where [?e :application/name "BehavePlus"]]
       (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def surface-eid   (sm/t-key->eid conn "behaveplus:surface"))
#_{:clj-kondo/ignore [:missing-docstring]}
(def crown-eid     (sm/t-key->eid conn "behaveplus:crown"))
#_{:clj-kondo/ignore [:missing-docstring]}
(def contain-eid   (sm/t-key->eid conn "behaveplus:contain"))
#_{:clj-kondo/ignore [:missing-docstring]}
(def mortality-eid (sm/t-key->eid conn "behaveplus:mortality"))

;; ===========================================================================================================
;; Note Categories Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def note-categories-payload
  [;; Shared across all SURFACE-based worksheets.
   {:db/id                         -1
    :application/_note-categories  bp-app-eid
    :note-category/name            "Fuel Model"
    :note-category/order           0
    :note-category/translation-key "behaveplus:note-categories:fuel-model"}

   {:db/id                         -2
    :application/_note-categories  bp-app-eid
    :note-category/name            "Fuel Moisture"
    :note-category/order           1
    :note-category/translation-key "behaveplus:note-categories:fuel-moisture"}

   {:db/id                         -3
    :application/_note-categories  bp-app-eid
    :note-category/name            "Weather"
    :note-category/order           2
    :note-category/translation-key "behaveplus:note-categories:weather"}

   {:db/id                         -4
    :application/_note-categories  bp-app-eid
    :note-category/name            "Topography"
    :note-category/order           3
    :note-category/translation-key "behaveplus:note-categories:topography"}

   ;; SURFACE + CROWN only.
   {:db/id                         -5
    :application/_note-categories  bp-app-eid
    :note-category/name            "Canopy Fuel"
    :note-category/order           4
    :note-category/modules         [surface-eid crown-eid]
    :note-category/translation-key "behaveplus:note-categories:canopy-fuel"}

   ;; SURFACE + CONTAIN only.
   {:db/id                         -6
    :application/_note-categories  bp-app-eid
    :note-category/name            "Resources"
    :note-category/order           5
    :note-category/modules         [surface-eid contain-eid]
    :note-category/translation-key "behaveplus:note-categories:resources"}

   ;; SURFACE + MORTALITY only.
   {:db/id                         -7
    :application/_note-categories  bp-app-eid
    :note-category/name            "Mortality"
    :note-category/order           6
    :note-category/modules         [surface-eid mortality-eid]
    :note-category/translation-key "behaveplus:note-categories:mortality"}

   ;; Results stage — shared.
   {:db/id                         -8
    :application/_note-categories  bp-app-eid
    :note-category/name            "Fire Behavior"
    :note-category/order           7
    :note-category/translation-key "behaveplus:note-categories:fire-behavior"}])

;; ===========================================================================================================
;; Translations Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   100
   {;; Note category names.
    "behaveplus:note-categories:fuel-model"    "Fuel Model"
    "behaveplus:note-categories:fuel-moisture" "Fuel Moisture"
    "behaveplus:note-categories:weather"       "Weather"
    "behaveplus:note-categories:topography"    "Topography"
    "behaveplus:note-categories:canopy-fuel"   "Canopy Fuel"
    "behaveplus:note-categories:resources"     "Resources"
    "behaveplus:note-categories:mortality"     "Mortality"
    "behaveplus:note-categories:fire-behavior" "Fire Behavior"

    ;; Note editor / list UI strings.
    "behaveplus:cancel"                "Cancel"
    "behaveplus:confirm_discard_note"  "Discard unsaved changes to this note?"
    "behaveplus:confirm_delete_note"   "Delete this note? This cannot be undone."}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (into note-categories-payload translations-payload))

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
