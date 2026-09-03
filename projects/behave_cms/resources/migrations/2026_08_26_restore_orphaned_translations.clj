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

(defn affected-translations
  "Map of orphaned translation-key -> the translation it had in the pre-migration DB. Run on the dev DB.

   A key is included only when it is a genuine migration-caused loss:
   1. its result key equals its translation key with `:result:` inserted (the shared-key signature),
   2. its `translation-key` currently has NO `:translation/key` entry (blank now), and
   3. the entity's translation-key DID resolve in the pre-migration DB — `(d/as-of db old-db-instant)` —
      and that pre-migration translation becomes the restored value.
   Condition 2 drops keys whose translation-key merely got io inserted but kept an entry (e.g.
   `crown:input:size:elapsed_time`); condition 3 drops keys that were never translated (blank then too).
   Only groups and group-variables carry result keys, so those two cover every case."
  [db]
  (let [dict     (into #{} (map first) (d/q '[:find ?k :where [_ :translation/key ?k]] db))
        old-db   (d/as-of db old-db-instant)
        old-tmap (into {} (d/q '[:find ?k ?t
                                 :where [?e :translation/key ?k] [?e :translation/translation ?t]] old-db))]
    (into (sorted-map)
          (for [[tattr rattr] [[:group/translation-key          :group/result-translation-key]
                               [:group-variable/translation-key :group-variable/result-translation-key]]
                [e tk rk]     (d/q '[:find ?e ?tk ?rk :in $ ?ta ?ra
                                     :where [?e ?ta ?tk] [?e ?ra ?rk]] db tattr rattr)
                :let          [old-text (get old-tmap (get (d/entity old-db e) tattr))]
                :when         (and (string? tk) (string? rk)
                                   (str/includes? rk ":result:")
                                   (= tk (str/replace rk ":result:" ":")) ; tk & rk were the same pre-migration
                                   (not (contains? dict tk))              ; blank now
                                   old-text)]                             ; but had a value in the old DB
            [tk old-text]))))

;;=>
#_{"behaveplus:contain:input:suppression:contain_mode"                                         "Contain Mode",
   "behaveplus:contain:input:suppression:resource"                                             "Estimated Resource Arrival Time and Duration",
   "behaveplus:contain:input:suppression:resource:resource_arrival_time"                       "Resource Arrival Time",
   "behaveplus:contain:input:suppression:resource:resource_arrival_time:resource_arrival_time" "Resource Arrival Time",
   "behaveplus:contain:input:suppression:resource:resource_duration"                           "Resource Duration",
   "behaveplus:crown:input:fuel_moisture:moisture_scenario:moisture_scenario"                  "Moisture Scenario"}

(defn affected-translation-keys
  "Just the orphaned translation keys (the keys of `affected-translations`)."
  [db]
  (vec (keys (affected-translations db))))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration. Recomputes the orphaned keys + their pre-migration translations from the db
;; (`affected-translations`) and creates a dictionary entry (linked to English) for each. The runner
;; transacts the result plus a :bp/migration-id marker. Idempotent: once created the keys resolve, so a
;; re-run finds nothing.
#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [restore (affected-translations db)]
    (println (format "[restore-orphaned-translations] restoring %d orphaned translation(s): %s"
                     (count restore) (pr-str (keys restore))))
    (vec (sm/build-translations-payload db restore))))

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
