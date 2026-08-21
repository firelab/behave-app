(ns migrations.2026-08-20-add-repeat-group-item-translations
  (:require [behave.schema.group      :refer [repeat-translation-key]]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; BHP1-1526 — Repeat group items read plural ("Resources #1"); they should read
;; singular ("Resource #1").
;;
;; An item's name is a translation keyed "<group translation-key>:repeat" (see
;; behave.schema.group/repeat-translation-key). This adds it for every repeat
;; group — today only Contain > Suppression > Resources — deriving the singular
;; by dropping a trailing "s". Irregular ones can be fixed in the CMS under
;; Group > Translations > Repeat Item Translations.
;;
;; After this runs, re-export layout.msgpack.

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(defn- singular
  "Naive singular of `s`: drops a trailing \"s\"."
  [s]
  (cond-> s
    (re-find #"s$" s) (subs 0 (dec (count s)))))

(defn- repeat-groups
  "Translation keys of every group that repeats."
  [db]
  (d/q '[:find [?k ...]
         :where
         [?g :group/repeat? true]
         [?g :group/translation-key ?k]]
       db))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (->> (repeat-groups db)
       (reduce (fn [acc t-key]
                 (let [translation (d/q '[:find ?t . :in $ ?k
                                          :where
                                          [?e :translation/key ?k]
                                          [?e :translation/translation ?t]]
                                        db t-key)]
                   (cond-> acc
                     translation
                     (assoc (repeat-translation-key t-key) (singular translation)))))
               {})
       (sm/upsert-translations db "en-US")
       (vec)))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server :as cms]
           '[behave-cms.store  :as store])
  (cms/init-db!)

  (def conn (store/default-conn))

  (payload-fn (d/db conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (require '[schema-migrate.interface :as sm])
  (sm/rollback-tx! conn tx-data))
