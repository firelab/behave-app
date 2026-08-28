(ns migrations.2026-08-23-normalize-io-after-module-translation-keys
  (:require [clojure.string           :as str]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Normalize every submodule-scoped translation key and result key so the input/output direction sits
;; IMMEDIATELY AFTER THE MODULE:
;;
;;   translation key : behaveplus:<module>:<submodule>:<io>:...  ->  behaveplus:<module>:<io>:<submodule>:...
;;   submodule key   : behaveplus:surface:spot:input             ->  behaveplus:surface:input:spot
;;   result key      : behaveplus:<module>:result:<submodule>:...->  behaveplus:<module>:<io>:result:<submodule>:...
;;   missing-io keys : behaveplus:crown:topography:aspect        ->  behaveplus:crown:input:topography:aspect
;;                     (result) behaveplus:crown:topography:aspect
;;                                                               ->  behaveplus:crown:input:result:topography:aspect
;;
;; This REVERSES the io-after-submodule reformatting that briefly lived on this branch (io now sits after
;; the module again). The RESULT key deliberately RETAINS io (behaveplus:<module>:<io>:result:<submodule>)
;; so that an input group and an output group under the same submodule never collapse onto the same result
;; key — the "Burning Pile" clash on the UNIQUE `:group/result-translation-key`. This matches the
;; generators (`subgroups/views.cljs`, `schema_migrate/core.clj`), which insert `:result:` after io.
;;
;; The CMS db has been reverted to the dev snapshot, so translation keys are mostly already io-after-module;
;; the real work here is (1) inserting io for the "case-C" keys authored WITHOUT io (crown/mortality
;; topography-style submodules) and (2) rewriting dev result keys (io dropped, `:result:` in io's slot) to
;; the io-retaining form. All branches are idempotent — they no-op on keys already in the target shape.
;;
;; io is derived from the owning submodule (`:submodule/io`), not string parsing. This runs BEFORE
;; 2026_08_24_snake_case_translation_keys (sorted by filename), while keys are still kebab.
;;
;; SCOPE: `:submodule/translation-key`, and `:group/`+`:group-variable/` translation-key and
;; result-translation-key for every group/group-variable that descends from an input/output submodule.
;; HELP KEYS ARE LEFT UNTOUCHED (any key ending in ":help"; they tie to a separate DITA/MadCap system).
;; List-option and subtool keys are NOT submodule descendants and are excluded.
;;
;; DICTIONARY LOCKSTEP: every changed key string has its `:translation/key` dictionary entry renamed to
;; match. Where one dictionary entry served both a translation-key and a result-key that now diverge, the
;; entry is renamed for one and TEXT-COPIED for the other so no translation is dropped.

;; ===========================================================================================================
;; Helpers
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- as-one
  "Reverse-refs on entities here return a single entity (not a set); coerce entity-or-collection to one."
  [x]
  (if (and x (not (:db/id x)) (coll? x)) (first x) x))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- submodule-io
  "Walk `eid` UP to its owning submodule and return its `:submodule/io` (:input/:output), or nil."
  [db eid]
  (loop [e    (d/entity db eid)
         seen #{}]
    (cond
      (nil? e)                        nil
      (as-one (:submodule/_groups e)) (:submodule/io (as-one (:submodule/_groups e)))
      (contains? seen (:db/id e))     nil
      :else                           (recur (as-one (or (:group/_children e)
                                                         (:group/_group-variables e)))
                                             (conj seen (:db/id e))))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- help? [k] (str/ends-with? k ":help"))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- tk-io
  "Parse the io direction (\"input\"/\"output\") out of a translation key, or nil if absent. A group's
   io lives in its OWN key (module:io:submodule at idx 2, or branch-era module:submodule:io at idx 3) and
   is authoritative — an input group and an output group can share one submodule, so the submodule's
   `:submodule/io` cannot tell them apart."
  [tk]
  (when tk
    (let [s (str/split tk #":")]
      (or (#{"input" "output"} (get s 2))
          (#{"input" "output"} (get s 3))))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- insert-at
  "Insert `x` at index `idx` in vector `segs`."
  [segs idx x]
  (vec (concat (take idx segs) [x] (drop idx segs))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn normalize-tk
  "Move io to right after the module for a translation key. `io` is \"input\"/\"output\".
   Keys are `behaveplus:<module>:...` so module/io/submodule sit at indices 1/2/3."
  [k io]
  (let [s (str/split k #":")]
    (cond
      (#{"input" "output"} (get s 2)) k                                    ; already io-after-module
      (#{"input" "output"} (get s 3)) (str/join ":" (assoc s 2 (get s 3)   ; swap submodule <-> io
                                                           3 (get s 2)))
      :else                           (str/join ":" (insert-at s 2 io))))) ; fill in missing io after module

#_{:clj-kondo/ignore [:missing-docstring]}
(defn normalize-rk
  "Produce `behaveplus:<module>:<io>:result:<submodule>:...` for a result key — io RETAINED so input
   and output result keys never collide. `io` is \"input\"/\"output\". Idempotent."
  [k io]
  (let [s (str/split k #":")]
    (if (and (#{"input" "output"} (get s 2)) (= "result" (get s 3)))
      k                                                              ; already <module>:<io>:result:<submodule>
      (let [s1 (if (or (= "result" (get s 2)) (#{"input" "output"} (get s 2)))
                 (vec (concat (subvec s 0 2) (subvec s 3)))          ; strip the leading result/io token at idx 2
                 s)]                                                 ; (case-C: no leading token, keep as-is)
        (str/join ":" (insert-at (insert-at s1 2 io) 3 "result"))))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- dict-eid [db k] (ffirst (d/q '[:find ?e :in $ ?k :where [?e :translation/key ?k]] db k)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn- dict-text [db k]
  (ffirst (d/q '[:find ?t :in $ ?k :where [?e :translation/key ?k] [?e :translation/translation ?t]] db k)))

;; ===========================================================================================================
;; Change set
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn key-changes
  "Every {:eid :attr :old :new} translation/result key change for io submodules and their descendants."
  [db]
  (concat
   ;; submodule translation-keys
   (for [[e io tk] (d/q '[:find ?e ?io ?tk
                          :where [?e :submodule/io ?io] [?e :submodule/translation-key ?tk]] db)
         :when     (#{:input :output} io)
         :when     (not (help? tk))
         :let      [nw (normalize-tk tk (name io))]
         :when     (not= tk nw)]
     {:eid e :attr :submodule/translation-key :old tk :new nw})
   ;; group & group-variable translation-keys + result-translation-keys
   (for [[tattr rattr] [[:group/translation-key          :group/result-translation-key]
                        [:group-variable/translation-key :group-variable/result-translation-key]]
         e             (distinct (concat (map first (d/q '[:find ?e :in $ ?a :where [?e ?a _]] db tattr))
                                         (map first (d/q '[:find ?e :in $ ?a :where [?e ?a _]] db rattr))))
         :let          [ent (d/entity db e)
                        tk  (get ent tattr)
                        rk  (get ent rattr)
                        ;; io from the entity's OWN key (distinct per group); fall back to the owning
                        ;; submodule only for case-C keys that carry no io token.
                        ion (or (tk-io tk) (some-> (submodule-io db e) name))]
         :when         (#{"input" "output"} ion)
         c             (concat (when (and tk (not (help? tk)) (not= tk (normalize-tk tk ion)))
                                 [{:eid e :attr tattr :old tk :new (normalize-tk tk ion)}])
                               (when (and rk (not (help? rk)) (not= rk (normalize-rk rk ion)))
                                 [{:eid e :attr rattr :old rk :new (normalize-rk rk ion)}]))]
     c)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn dictionary-tx
  "Keep the `:translation/key` dictionary in lockstep with the key changes. For each old key with a
   dictionary entry, rename it to the first new key; text-copy for any additional divergent new key
   (the shared translation-key/result-key split)."
  [db changes]
  (let [old->news (reduce (fn [m {:keys [old] nw :new}] (update m old (fnil conj #{}) nw)) {} changes)]
    (vec (mapcat (fn [[old news]]
                   (when-let [de (dict-eid db old)]
                     (let [txt (dict-text db old)
                           nv  (vec news)]
                       (cons {:db/id de :translation/key (first nv)}
                             (map (fn [n] {:translation/key n :translation/translation txt}) (rest nv))))))
                 old->news))))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Single-step migration. The runner calls (payload-fn db) at startup and transacts the result.
;; Guard: validate the change set with a speculative `d/with` so any would-be duplicate on a
;; `:db.unique/identity` translation-key attr throws an informative ex-info instead of a raw
;; transaction failure. On a clean reverted db the only changes are case-C io insertions
;; (input-only submodules), so this is a clean pass.
#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [changes (key-changes db)
        tx      (vec (concat
                      (map (fn [{:keys [eid attr] nw :new}] {:db/id eid attr nw}) changes)
                      (dictionary-tx db changes)))]
    (try
      (d/with db tx)
      (catch Exception e
        (throw (ex-info (str "normalize-io migration: applying the io->after-module change set would "
                             "violate a unique translation-key constraint; resolve by hand first")
                        {:change-count (count changes)
                         :sample       (vec (take 20 changes))}
                        e))))
    tx))

;; ===========================================================================================================
;; Manual REPL usage
;; ===========================================================================================================

#_{:clj-kondo/ignore [:duplicate-require :missing-docstring :unresolved-namespace]}
(comment
  (require '[behave-cms.server        :as cms])
  (cms/init-db!)

  (def conn (behave-cms.store/default-conn))

  ;; Inspect the change set and tx-data.
  (count (key-changes (d/db conn)))
  (payload-fn (d/db conn))

  (try (def tx-data @(d/transact conn (payload-fn (d/db conn))))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; Rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
