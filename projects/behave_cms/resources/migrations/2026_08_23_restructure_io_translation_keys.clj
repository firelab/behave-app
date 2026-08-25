(ns migrations.2026-08-23-restructure-io-translation-keys
  (:require [clojure.string           :as str]
            [datomic.api              :as d]
            [schema-migrate.interface :as sm]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Restructure every submodule-scoped translation key and result key so the input/output direction
;; sits IMMEDIATELY AFTER THE SUBMODULE:
;;
;;   translation key : behaveplus:<module>:<io>:<submodule>:...  ->  behaveplus:<module>:<submodule>:<io>:...
;;   submodule key   : behaveplus:surface:input:spot             ->  behaveplus:surface:spot:input
;;   result key      : behaveplus:surface:result:spot:burning_pile
;;                                                               ->  behaveplus:surface:spot:input:result:burning_pile
;;   missing-io keys : behaveplus:crown:topography:aspect        ->  behaveplus:crown:topography:input:aspect
;;                     (result) behaveplus:crown:topography:aspect
;;                                                               ->  behaveplus:crown:topography:input:result:aspect
;;
;; io is derived from the owning submodule (`:submodule/io`), not string parsing. This runs BEFORE
;; 2026_08_24_snake_case_translation_keys (sorted by filename) and removes the whole class of
;; input/output result-key collisions (e.g. the Surface "Burning Pile" input+output groups that would
;; otherwise both collapse to `behaveplus:surface:result:spot:burning_pile` on the UNIQUE
;; `:group/result-translation-key`).
;;
;; SCOPE: `:submodule/translation-key`, and `:group/`+`:group-variable/` translation-key and
;; result-translation-key for every group/group-variable that descends from an input/output submodule.
;; HELP KEYS ARE LEFT UNTOUCHED (any key ending in ":help"; they tie to a separate DITA/MadCap system).
;; List-option and subtool keys are NOT submodule descendants and are excluded.
;;
;; DICTIONARY LOCKSTEP: every changed key string has its `:translation/key` dictionary entry renamed to
;; match. Where one dictionary entry served both a translation-key and a result-key that now diverge, the
;; entry is renamed for one and TEXT-COPIED for the other so no translation is dropped.
;;
;; Verified on the live VMS db (speculative d/with, then chained through the snake migration): 888 entity
;; key changes + dictionary lockstep, 0 new-key collisions, 0 unresolvable/dictionary conflicts, and 0
;; hyphenated keys remaining after the snake migration.

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
(defn- insert-at
  "Insert `x` at index `idx` in vector `segs`."
  [segs idx x]
  (vec (concat (take idx segs) [x] (drop idx segs))))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn restructure-tk
  "Move io to right after the submodule for a translation key. `io` is \"input\"/\"output\".
   Keys are `behaveplus:<module>:...` so io/submodule sit at indices 2/3."
  [k io]
  (let [s (str/split k #":")]
    (if (#{"input" "output"} (get s 2))
      (str/join ":" (assoc s 2 (get s 3) 3 (get s 2)))   ; swap io <-> submodule
      (str/join ":" (insert-at s 3 io)))))               ; fill in missing io after the submodule

#_{:clj-kondo/ignore [:missing-docstring]}
(defn restructure-rk
  "Produce `behaveplus:<module>:<submodule>:<io>:result:<rest>` for a result key."
  [k io]
  (let [s0 (str/split k #":")
        ;; normalize to [bp module submodule ...rest] by dropping a leading result/io at index 2
        s1 (if (or (= "result" (get s0 2)) (#{"input" "output"} (get s0 2)))
             (vec (concat (subvec s0 0 2) (subvec s0 3)))
             s0)]
    (str/join ":" (insert-at (insert-at s1 3 io) 4 "result"))))

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
         :let      [nw (restructure-tk tk (name io))]
         :when     (not= tk nw)]
     {:eid e :attr :submodule/translation-key :old tk :new nw})
   ;; group & group-variable translation-keys + result-translation-keys
   (for [[tattr rattr] [[:group/translation-key          :group/result-translation-key]
                        [:group-variable/translation-key :group-variable/result-translation-key]]
         e             (distinct (concat (map first (d/q '[:find ?e :in $ ?a :where [?e ?a _]] db tattr))
                                         (map first (d/q '[:find ?e :in $ ?a :where [?e ?a _]] db rattr))))
         :let          [io (submodule-io db e)]
         :when         (#{:input :output} io)
         :let          [ent (d/entity db e)
                        ion (name io)
                        tk  (get ent tattr)
                        rk  (get ent rattr)]
         c             (concat (when (and tk (not (help? tk)) (not= tk (restructure-tk tk ion)))
                                 [{:eid e :attr tattr :old tk :new (restructure-tk tk ion)}])
                               (when (and rk (not (help? rk)) (not= rk (restructure-rk rk ion)))
                                 [{:eid e :attr rattr :old rk :new (restructure-rk rk ion)}]))]
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
#_{:clj-kondo/ignore [:missing-docstring]}
(defn payload-fn [db]
  (let [changes (key-changes db)]
    (vec (concat
          (map (fn [{:keys [eid attr] nw :new}] {:db/id eid attr nw}) changes)
          (dictionary-tx db changes)))))

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
