(ns migrations.2026-05-29-add-fire-size-at-report
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1562: Replace Contain's confusing "Elapsed Time" input with the
;; existing-but-hidden "Fire Size at Report" input (acres, 0.1 - 100), and
;; relocate it into the Suppression submodule where Elapsed Time used to
;; live.
;;
;; Currently `behaveplus:contain:input:fire:fire_size_at_report:...` is hidden
;; from the UI because a conditional link on the Contain > Fire submodule
;; routes Surface's "Fire Area" output into it whenever module = "contain".
;; The C++ ContainAdapter does not actually use Elapsed Time as an input — it
;; derives initial elapsed time internally from the report size.
;;
;; Steps:
;;   1. Retract the Surface > Fire Area → Contain > Fire Size at Report
;;      conditional link so Fire Size at Report becomes a user input.
;;   2. Detach the Elapsed Time group from Contain > Suppression.
;;   3. Move the Fire Area at Report group out of Contain > Fire and into
;;      Contain > Suppression at the slot Elapsed Time vacated (order 3).
;;   4. Gate the Fire submodule on `:module := {"contain"}` so it only
;;      appears when Contain is the only active module (its remaining
;;      groups — Surface ROS Max and Length-to-Width Ratio — are otherwise
;;      supplied by the Surface module).
;;
;; Surface > Size > Elapsed Time and Crown > Size > Elapsed Time are left
;; untouched.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def ^:private surface-fire-area-t-key
  "behaveplus:surface:output:size:surface___fire_size:fire_area")

(def ^:private contain-fire-size-t-key
  "behaveplus:contain:input:fire:fire_size_at_report:fire_size_at_report")

(def ^:private fire-submodule-t-key
  "behaveplus:contain:input:fire")

(def ^:private fire-size-at-report-group-t-key
  "behaveplus:contain:input:fire:fire_size_at_report")

(def ^:private suppression-submodule-t-key
  "behaveplus:contain:input:suppression")

(def ^:private elapsed-time-group-t-key
  "behaveplus:contain:input:suppression:elapsed-time")

(def ^:private fire-size-order
  "Slot in Suppression that Elapsed Time used to occupy."
  3)

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [conn]
  (let [auto-link       (sm/find-link conn surface-fire-area-t-key contain-fire-size-t-key)
        fire-submodule  (sm/t-key->eid conn fire-submodule-t-key)
        suppression     (sm/t-key->eid conn suppression-submodule-t-key)
        fire-size-group (sm/t-key->eid conn fire-size-at-report-group-t-key)
        elapsed-group   (sm/t-key->eid conn elapsed-time-group-t-key)]
    (cond-> []
      auto-link
      (conj [:db/retractEntity auto-link])

      (and suppression elapsed-group)
      (conj [:db/retract suppression :submodule/groups elapsed-group])

      (and fire-submodule suppression fire-size-group)
      (into [[:db/retract fire-submodule :submodule/groups fire-size-group]
             [:db/add     suppression    :submodule/groups fire-size-group]
             [:db/add     fire-size-group :group/order     fire-size-order]])

      fire-submodule
      (into (let [contain-only (-> (sm/->module-conditional :equal ["contain"])
                                   (assoc :db/id -1))]
              [contain-only
               [:db/add fire-submodule :submodule/conditionals -1]])))))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

(comment
  (require '[behave-cms.server :as cms]
           '[behave-cms.store  :as store])
  (cms/init-db!)

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def conn (store/default-conn))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn (payload-fn conn)))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
