(ns remove-low-eids 
  (:require
   [datomic-store.main       :as ds]
   [datomic.api              :as d]
   [behave-cms.server        :as cms]
   [behave.schema.core       :refer [all-schemas]]
   [nano-id.core             :refer [nano-id]]
   [clojure.string           :as str]
   [clojure.set              :as set]
   [map-utils.interface      :refer [index-by]]
   [string-utils.interface   :refer [->str]]))

;;; Helpers

(def ^:private rand-uuid (comp str d/squuid))

(defn- remove-tx [e]
  [:db/retractEntity e])

(defn- retract-tx [[e a v]]
  [:db/retract e a v])

(defn- retract-attrs-tx [e]
  (->> e
       (map (fn [[a v]]
              (cond
                (vector? v)
                (mapv (fn [x] [:db/retract (:db/id e) a (get x :db/id x)]) v)

                (not= a :db/id)
                [[:db/retract (:db/id e) a (if (vector? v) (map :db/id v) v)]]

                :else
                [nil])))
       (apply concat)
       (remove nil?)
       (vec)))

(defn- retract-old-entities-tx
  "Retracts attributes of multiple entities, but leaves the `:db/id` in place."
  [old-eid-map]
  (apply concat
         (map (fn [[id e]]
                       (-> e
                           (assoc :db/id id)
                           (dissoc :bp/nid :bp/uuid)
                           (retract-attrs-tx))) old-eid-map)))

(defn- remap-tx
  "Given a map index of `{old-eid {:bp/nid ...}}`,
   create a tx that remaps `old-eid` to the new `:bp/nid`."
  [old-eid-idx [e a v]]
  [:db/add e a [:bp/nid (get-in old-eid-idx [v :bp/nid])]])

(defn- update-children-tx
  "Used to update the `child-attr` for `old-eids`, that the `old-eids` are entities that
   are considered a 'Level 1' attribute."
  [db old-eids child-attr l1-idx l2-idx]
  (->> old-eids
       (d/pull-many db '[*])
       (map (fn [e] [(:db/id e) (map :db/id (get e child-attr))]))
       (map (fn [[id children]]
              [(get-in l1-idx [id :bp/nid])
               (map #(get-in l2-idx [% :bp/nid]) children)]))
       (map (fn [[parent-nid child-nids]]
              (map (fn [child-nid]
                     [:db/add
                      [:bp/nid parent-nid]
                      child-attr
                      [:bp/nid child-nid]]) child-nids)))
       (apply concat)
       (vec)))

(defn- low-eid?
  [eid]
  (and (< 1000 eid) (> 100000 eid)))

;;;; Queries

(defn- q-parent-refs
  "Find all parents of `eids` using `ref-attrs`."
  [db ref-attrs eids]
  (d/q '[:find ?e1 ?a ?e2
         :in $ [?a ...] [?e2 ...]
         :where
         [?e1 ?a ?e2]]
       db ref-attrs eids))

(defn- q-high-refs-low-eids
  "Find all parents of `attrs` of low EID entities that have high EID's."
  [db attrs]
  (d/q '[:find ?e1 ?a ?e2
         :in $ [?a ...]
         :where
         [?e1 ?a ?e2]
         [(< 10000 ?e1)]
         [(< 1000 ?e2)]
         [(> 10000 ?e2)]] db attrs))

(defn- q-high-refs-for-low-eids
  "Find all parents of `attrs` of matching `eids` that have high EID's."
  [db ref-attrs eids]
  (d/q '[:find ?e1 ?a ?e2
         :in $ [?a ...] [?e2 ...]
         :where
         [?e1 ?a ?e2]
         [(< 1000 ?e2)]
         [(> 10000 ?e2)]] db ref-attrs eids))

(defn- q-low-refs-low-eids
  "Find all parents of `attrs` of low EID entities that have low EID's."
  [db attrs]
  (d/q '[:find ?e1 ?a ?e2
         :in $ [?a ...]
         :where
         [?e1 ?a ?e2]
         [(< 1000 ?e1)]
         [(> 10000 ?e1)]
         [(< 1000 ?e2)]
         [(> 10000 ?e2)]] db attrs))

(defn- q-low-eids
  "Find all EIDs that have at least one of `attrs` with a low EID."
  [db attrs]
  (d/q '[:find [?e ...]
         :in $ [?a ...]
         :where
         [?e ?a ?v]
         [(< 1000 ?e)]
         [(> 10000 ?e)]] db attrs))

;;;; Indexes

(defn- old-eids->new-entities
  "Creates an index of old EID to 'new' entity
   that only `keep-attrs` (with optional `new-ids?`)."
  [db eids keep-attrs & {:keys [new-ids? ref-attrs]}]
  (let [ref-attrs        (set/intersection (set ref-attrs) (set keep-attrs))
        get-id-from-refs (if (seq ref-attrs)
                           (apply comp (map (fn [attr] #(if (get % attr) (update % attr (partial mapv :db/id)) %)) ref-attrs))
                           identity)]
    (->> eids
         (d/pull-many db '[*])
         (map #(select-keys % (apply conj keep-attrs :db/id (when (not new-ids?) '(:bp/uuid :bp/nid)))))
         (map #(if new-ids? (assoc % :bp/uuid (rand-uuid) :bp/nid (nano-id)) %))
         (index-by :db/id)
         (map (fn [[k v]] [k (-> v (dissoc :db/id) (get-id-from-refs))]))
         (into {}))))

;;; Common Schemas

(def ^:private all-attrs 
  (->> all-schemas
       (map :db/ident)))

(def ^:private ref-attrs 
  (->> all-schemas
       (filter #(= :db.type/ref (:db/valueType %)))
       (map :db/ident)))

(def ^:private cond-ref-attrs 
  (->> all-schemas
       (map :db/ident)
       (filter #(str/ends-with? (str %) "conditionals"))))

(def ^:private cond-attrs
  (->> all-schemas
       (map :db/ident)
       (filter #(str/starts-with? (->str %) "conditional"))))

(def ^:private action-attrs
  (->> all-schemas
       (map :db/ident)
       (filter #(str/starts-with? (->str %) "action"))))

(def ^:private translation-attrs
  (->> all-schemas
       (map :db/ident)
       (filter #(str/starts-with? (->str %) "translation"))))

(def ^:private list-option-attrs
  (->> all-schemas
       (map :db/ident)
       (filter #(str/starts-with? (->str %) "list-option"))))

(def ^:private group-attrs
  (->> all-schemas
       (map :db/ident)
       (filter #(str/starts-with? (->str %) "group"))))

(def ^:private group-variable-attrs
  (->> all-schemas
       (map :db/ident)
       (filter #(str/starts-with? (->str %) "group-variable"))))

(def ^:private group-variable-ref-attrs
  (->> ref-attrs
       (filter #(or
                 (str/starts-with? (->str %) "group")
                 (str/starts-with? (->str %) "variable")))))

;;; Drivers

(defn- refresh-entities!

  [conn parent-eav-refs old-eid-map]
  ;; Remove parent relations
  (d/transact conn (map retract-tx parent-eav-refs))

  ;; Remove old attributes
  (d/transact conn (retract-old-entities-tx old-eid-map))

  ;; TX new entities
  (d/transact conn (vals old-eid-map))

  ;; Remap parents
  (d/transact conn (map (partial remap-tx old-eid-map) parent-eav-refs)))

#_{:clj-kondo/ignore [:missing-docstring]}
(do

  ;;; 0. Setup (get DB)

  (cms/init-db!)

  (def conn @ds/datomic-conn)
  (def db (d/db conn))

  ;;;; 1. Conditionals
  ;;; 1A. High Entities -> Low Entities
  (def db-1a db)
  (def conds-w-low-eids-high-refs (q-high-refs-low-eids db-1a cond-ref-attrs))
  (def old-eid->new-cond (old-eids->new-entities db-1a (map last conds-w-low-eids-high-refs) cond-attrs {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn conds-w-low-eids-high-refs old-eid->new-cond)

  ;;; 1B. Conditionals of Low Entities -> Low Entities
  (def db-1b (d/db conn))
  (def conds-low-eids-low-refs (q-low-refs-low-eids db-1b cond-ref-attrs))
  (def l2-eid->new-cond (old-eids->new-entities db-1b (map last conds-low-eids-low-refs) cond-attrs {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn conds-low-eids-low-refs l2-eid->new-cond)

  ;;;; 2. Actions
  ;;; 2A. Actions with Low EIDs (Level 1)

  (def db-2 (d/db conn))
  (def l1-actions (q-high-refs-for-low-eids db-2 [:group-variable/actions] (map first conds-low-eids-low-refs)))

  ;; New entities
  (def old-eid->new-l1-action (old-eids->new-entities db-2 (map last l1-actions) action-attrs {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn l1-actions old-eid->new-l1-action)

  ;;;; 3. Translations

  (def db-3 (d/db @ds/datomic-conn))

  (def english
    (d/q '[:find ?e . :where [?e :language/shortcode "en-US"]] db-3))

  (def low-eid-translations (q-high-refs-low-eids db-3 [:language/translation]))
  (def low-eid-english-translations (remove #(not= (first %) english) low-eid-translations))

  ;; New entities
  (def old-eid->new-translation (old-eids->new-entities db-3 (map last low-eid-english-translations) translation-attrs {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn low-eid-english-translations old-eid->new-translation)

  ;;;; 4. List Options

  (def db-4 (d/db @ds/datomic-conn))

  (def low-eid-list-options (q-high-refs-low-eids db [:list/options]))

  ;; New entities
  (def old-eid->new-list-options (old-eids->new-entities db-4 (map last low-eid-list-options) list-option-attrs {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn low-eid-list-options old-eid->new-list-options)

  ;;;; 5. Group Variables

  (def db-5a (d/db @ds/datomic-conn))

  ;;; 5A. Group Variables w/ High Group EID's

  (def h2l-group-variables (q-high-refs-low-eids db-5a group-variable-ref-attrs))

  ;; New entities
  (def old-eid->h2l-group-variables
    (old-eids->new-entities db-5a
                            (->> h2l-group-variables
                                 (filter #(= :variable/group-variables (second %)))
                                 (map last))
                            group-variable-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (def h2l-gv-parents (q-parent-refs db-5a ref-attrs (keys old-eid->h2l-group-variables)))

  (refresh-entities! conn h2l-gv-parents old-eid->h2l-group-variables)

  ;;; 5B. Group Variable's Actions

  ;;; 5B. Subgroups w/ High references

  (def db-5b (d/db conn))

  (def subgroups-w-high-refs (q-high-refs-low-eids db-5b group-variable-ref-attrs))

  ;; New entities
  (def old-eid->subgroups-w-high-refs
     (old-eids->new-entities db-5b
                             (map last subgroups-w-high-refs) 
                             group-attrs
                             {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn subgroups-w-high-refs old-eid->subgroups-w-high-refs)

  ;;; 5C. Subgroups w/ Low EID references

  (def db-5c (d/db conn))

  (def l2-subgroups (q-low-refs-low-eids db-5c group-variable-ref-attrs))

  ;; New entities
  (def old-eid->l2-subgroups
    (old-eids->new-entities db-5c
                            (map last l2-subgroups)
                            group-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn l2-subgroups old-eid->l2-subgroups)

  ;;; 5D. Groups w/ Low EID references

  (def db-5d (d/db @ds/datomic-conn))

  (def l1-groups
    (->> l2-subgroups
         (filter #(low-eid? (first %)))
         (map first)
         (q-parent-refs db-5d ref-attrs)))

  ;; New entities
  (def old-eid->l1-groups
    (old-eids->new-entities db-5d
                            (map last l1-groups)
                            group-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))


  (refresh-entities! conn l1-groups old-eid->l1-groups)

  ;;; 5E. Groups w/ High references

  (def db-5e (d/db @ds/datomic-conn))

  (def l1-groups-w-high-refs (q-high-refs-low-eids db-5e [:submodule/groups]))

  ;; New entities
  (def old-eid->l1-groups-w-high-refs
    (old-eids->new-entities db-5e
                            (map last l1-groups-w-high-refs)
                            group-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn l1-groups-w-high-refs old-eid->l1-groups-w-high-refs)

  ;;;; 6. Remove Low EIDs w/ dangling `:group-variable/*?` attributes

  (def db-6 (d/db @ds/datomic-conn))

  (def low-eids-dangling-gvs (q-low-eids db [:group-variable/conditionally-set? :group-variable/discrete-multiple?]))

  (d/transact @ds/datomic-conn (map remove-tx low-eids-dangling-gvs))

  ;;;; 7. Verify
  (def db-7 (d/db @ds/datomic-conn))

  (empty? (concat (q-high-refs-low-eids db-7 ref-attrs)
                  (q-low-refs-low-eids db-7 ref-attrs)
                  (q-low-eids db-7 all-attrs)))

  (d/pull-many db-7 '[*] (q-low-eids db-7 all-attrs))

  )
