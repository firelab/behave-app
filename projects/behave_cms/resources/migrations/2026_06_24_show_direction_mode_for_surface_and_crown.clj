(ns migrations.2026-06-24-show-direction-mode-for-surface-and-crown
  (:require [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1354 — Show a pre-selected "Heading" Direction Mode under Surface & Crown.
;;
;; The "Fire Behavior" output submodule is gated by module conditionals (:or over
;; ["surface"], ["contain" "surface"], ["mortality" "surface"]) and is missing
;; the ["crown" "surface"] combo, so in Surface & Crown the whole submodule —
;; including the "Direction Mode" group — never appears.
;;
;; We want ONLY the "Direction Mode" group to appear under Surface & Crown (not
;; "Surface Fire" nor "Ignition"). The direction options already carry actions
;; that select Heading and disable HBF / Direction of Interest for ["crown"
;; "surface"], so making the group visible is all that's needed.
;;
;; Module conditionals match the selected module set exactly, so this migration:
;;   1. adds ["crown" "surface"] to the Fire Behavior submodule conditionals, and
;;   2. pins the "Surface Fire" and "Ignition" groups to the original combos
;;      (["surface"], ["contain" "surface"], ["mortality" "surface"]) so they do
;;      NOT show in Surface & Crown.
;; "Direction Mode" has no group conditionals, so it shows whenever the submodule
;; does — which now includes Surface & Crown.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def ^:private fire-behavior  "behaveplus:surface:output:fire_behavior")
(def ^:private surface-fire   "behaveplus:surface:output:fire_behavior:surface_fire")
(def ^:private ignition       "behaveplus:surface:output:fire_behavior:ignition")

;; Combos the Fire Behavior submodule served before this change — i.e. every
;; combo EXCEPT Surface & Crown.
(def ^:private non-crown-module-sets
  [["surface"] ["contain" "surface"] ["mortality" "surface"]])

(defn- module-conditional [db modules]
  (sm/->conditional db {:ttype :module :operator :equal :values modules}))

#_{:clj-kondo/ignore [:missing-docstring :unused-binding]}
(defn payload-fn [db]
  [;; Let the submodule appear for Surface & Crown (cardinality-many: appends).
   {:db/id                  (sm/t-key->eid db fire-behavior)
    :submodule/conditionals [(module-conditional db ["crown" "surface"])]}

   ;; Keep "Surface Fire" and "Ignition" out of Surface & Crown by pinning them
   ;; to the original (non-crown) module combos.
   {:db/id                       (sm/t-key->eid db surface-fire)
    :group/conditionals-operator :or
    :group/conditionals          (mapv #(module-conditional db %) non-crown-module-sets)}

   {:db/id                       (sm/t-key->eid db ignition)
    :group/conditionals-operator :or
    :group/conditionals          (mapv #(module-conditional db %) non-crown-module-sets)}])

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server :as cms]
           '[behave-cms.store  :as store])
  (cms/init-db!)

  (def conn (store/default-conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
