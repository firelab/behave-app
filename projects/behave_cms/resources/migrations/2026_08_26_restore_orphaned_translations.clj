(ns migrations.2026-08-26-restore-orphaned-translations
  (:require [clojure.string           :as str]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; The 2026-08-23 (io-normalize) + 2026-08-24 (snake) migrations deleted some `:translation/key`
;; dictionary entries, leaving these reference keys orphaned so the app renders their label BLANK.
;;
;; Cause: for each of these group / group-variables the `translation-key` and `result-translation-key`
;; were the SAME string (one shared dictionary entry). io-normalize renames that shared entry onto the
;; `:result:` key.

;; ===========================================================================================================
;; Affected-key detection
;; ===========================================================================================================

;; The dev DB state just before the io/snake migrations ran, used to tell a genuine loss (the key HAD a
;; translation then) from a key that was never translated (blank then, blank now — a false report).
(def ^:private old-db-instant #inst "2026-08-27")

(defn affected-translation-keys
  "The translation keys orphaned by the io/snake migrations because the entity's `translation-key` and
   `result-translation-key` were the SAME string pre-migration (one shared dictionary entry that
   io-normalize renamed onto the `:result:` key). Run on the dev DB.

   A key is reported only when it is a genuine migration-caused loss:
   1. its result key equals its translation key with `:result:` inserted (the shared-key signature),
   2. its `translation-key` currently has NO `:translation/key` entry (blank now), and
   3. the entity's translation-key DID resolve in the pre-migration DB — checked via
      `(d/as-of db old-db-instant)`, looking up the entity's translation-key value AS IT WAS THEN.
  Only groups and group-variables carry result keys, so those two cover every case."
  [db]
  (let [dict     (into #{} (map first) (d/q '[:find ?k :where [_ :translation/key ?k]] db))
        old-db   (d/as-of db old-db-instant)
        old-dict (into #{} (map first) (d/q '[:find ?k :where [_ :translation/key ?k]] old-db))]
    (->> (for [[tattr rattr] [[:group/translation-key          :group/result-translation-key]
                              [:group-variable/translation-key :group-variable/result-translation-key]]
               [e tk rk]     (d/q '[:find ?e ?tk ?rk :in $ ?ta ?ra
                                    :where [?e ?ta ?tk] [?e ?ra ?rk]] db tattr rattr)
               :when         (and (string? tk) (string? rk)
                                  (str/includes? rk ":result:")
                                  (= tk (str/replace rk ":result:" ":"))               ; tk & rk were the same pre-migration
                                  (not (contains? dict tk))                             ; blank now
                                  (contains? old-dict (get (d/entity old-db e) tattr)))] ; but had a value in the old DB
           tk)
         distinct
         sort
         vec)))
;;=>
;; ["behaveplus:contain:input:suppression:contain_mode"
;;  "behaveplus:contain:input:suppression:resource"
;;  "behaveplus:contain:input:suppression:resource:resource_arrival_time"
;;  "behaveplus:contain:input:suppression:resource:resource_arrival_time:resource_arrival_time"
;;  "behaveplus:contain:input:suppression:resource:resource_duration"
;;  "behaveplus:crown:input:fuel_moisture:moisture_scenario:moisture_scenario"]

(def ^:private orphan-key->translation
  {"behaveplus:contain:input:suppression:contain_mode"                                         "Contain Mode"
   "behaveplus:contain:input:suppression:resource"                                             "Estimated Resource and Arrival Duration"
   "behaveplus:contain:input:suppression:resource:resource_arrival_time"                       "Resource Arrival Time"
   "behaveplus:contain:input:suppression:resource:resource_arrival_time:resource_arrival_time" "Resource Arrival Time"
   "behaveplus:contain:input:suppression:resource:resource_duration"                           "Resource Duration"
   "behaveplus:crown:input:fuel_moisture:moisture_scenario:moisture_scenario"                  "Moisture Scenario"})

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration. Creates a dictionary entry (linked to English) for each key above that has no
;; `:translation/key` entry yet. The runner transacts the result plus a :bp/migration-id marker.
#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [existing (into #{} (map first) (d/q '[:find ?k :where [_ :translation/key ?k]] db))
        missing  (into {} (remove (fn [[k _]] (contains? existing k)) orphan-key->translation))]
    (println (format "[restore-orphaned-translations] %d/%d keys missing — restoring: %s"
                     (count missing) (count orphan-key->translation)
                     (pr-str (sort (keys missing)))))
    (vec (sm/build-translations-payload db missing))))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms])

  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  (payload-fn (d/db conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
