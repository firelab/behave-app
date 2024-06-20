(ns remove-low-eids 
  (:require
   [datomic-store.main       :as ds]
   [datomic.api              :as d]
   [behave-cms.server        :as cms]
   [behave.schema.core       :refer [all-schemas]]
   [nano-id.core             :refer [nano-id]]
   [clojure.string           :as str]
   [map-utils.interface      :refer [index-by]]
   [string-utils.interface   :refer [->str]]))

;;; Helpers

(def ^:private rand-uuid (comp str d/squuid))

(defn- remove-tx [e]
  [:db/retractEntity e])

(defn- retract-attrs-tx [e]
  (->> e
       (map (fn [[a v]]
              (cond
                (vector? v)
                (mapv (fn [x] [:db/retract (:db/id e) a (:db/id x)]) v)

                (not= a :db/id)
                [[:db/retract (:db/id e) a (if (vector? v) (map :db/id v) v)]]

                :else
                [nil])))
       (apply concat)
       (remove nil?)
       (vec)))

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
   that only `keep-attrs` (with optional `new-ids?`, and `removed-attrs`)."
  [db eids keep-attrs & {:keys [new-ids? remove-attrs]}]
  (->> eids
       (d/pull-many db '[*])
       (map #(select-keys % (apply conj keep-attrs :db/id (when (not new-ids?) '(:bp/uuid :bp/nid)))))
       (map #(if new-ids? (assoc % :bp/uuid (rand-uuid) :bp/nid (nano-id)) %))
       (index-by :db/id)
       (map (fn [[k v]] [k (apply dissoc v :db/id remove-attrs)]))
       (into {})))

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


#_{:clj-kondo/ignore [:missing-docstring]}
(do

  ;;; 0. Setup (get DB)

  (cms/init-db!)

  (def db (d/db @ds/datomic-conn))

  ;;;; 1. Conditionals
  ;;; 1A. High Entities -> Low Entities
  (def db-1 db)
  (def conds-w-low-eids-high-refs (q-high-refs-low-eids db-1 cond-ref-attrs))
  (def old-eid->new-cond (old-eids->new-entities db-1 (map last conds-w-low-eids-high-refs) cond-attrs {:new-ids? true}))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (map remove-tx (map last conds-w-low-eids-high-refs)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->new-cond))

  ;; Remap parents
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->new-cond) conds-w-low-eids-high-refs))

  ;;; 1B. Low Entities -> Low Entities
  (def conds-low-eids-low-refs (q-low-refs-low-eids db-1 cond-ref-attrs))
  (def l2-eid->new-cond (old-eids->new-entities db-1 (map last conds-low-eids-low-refs) cond-attrs {:new-ids? true}))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (map remove-tx (map last conds-low-eids-low-refs)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals l2-eid->new-cond))

  ;;;; 2. Actions
  ;;; 2A. Actions with Low EIDs (Level 1)

  (def db-2 (d/db @ds/datomic-conn))
  (def l1-low-eid-w-high-refs (q-high-refs-for-low-eids db-2 ref-attrs (map first conds-low-eids-low-refs)))
  (def l1-actions (filter #(= :group-variable/actions (second %)) l1-low-eid-w-high-refs))

  ;; New entities
  (def old-eid->new-l1-action (old-eids->new-entities db-2
                                                      (map last l1-actions)
                                                      action-attrs
                                                      {:new-ids? true :remove-attrs [:action/conditionals]}))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (map remove-tx (map last l1-actions)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->new-l1-action))

  ;; Update new Level 1 Actions with new Level 2 Conditionals
  (def l1-actions->conds-tx (update-children-tx db-2
                                                (map last l1-actions)
                                                :action/conditionals
                                                old-eid->new-l1-action
                                                l2-eid->new-cond))

  ;; TX update
  (d/transact @ds/datomic-conn l1-actions->conds-tx)

  ;; Remap parents of L1 Actions
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->new-l1-action) l1-actions))

  ;;;; 3. Translations

  (def db-3 (d/db @ds/datomic-conn))

  (def english
    (d/q '[:find ?e . :where [?e :language/shortcode "en-US"]] db-3))

  (def low-eid-translations (q-high-refs-low-eids db-3 [:language/translation]))
  (def low-eid-english-translations (remove #(not= (first %) english) low-eid-translations))

  ;; New entities
  (def old-eid->new-translation (old-eids->new-entities db-3 (map last low-eid-english-translations) translation-attrs {:new-ids? true}))

  ;; Remove refs from parents to current Entities
  (d/transact @ds/datomic-conn (map remove-tx (map last low-eid-translations)))

  ;; Remove translations that are unrelated to English
  (d/transact @ds/datomic-conn (mapv remove-tx (map last (filter #(not= (first %) english) low-eid-translations))))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->new-translation))

  ;; TX refs from parents to new Entities
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->new-translation) low-eid-english-translations))

  ;;;; 4. List Options

  (def db-4 (d/db @ds/datomic-conn))

  (def low-eid-list-options (q-high-refs-low-eids db [:list/options]))

  ;; New entities
  (def old-eid->new-list-options (old-eids->new-entities db-4 (map last low-eid-list-options) list-option-attrs {:new-ids? true}))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (mapv remove-tx (map last low-eid-list-options)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->new-list-options))

  ;; TX refs from parents to new Entities
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->new-list-options) low-eid-list-options))

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
                            {:new-ids? true}))

  (def h2l-gv-parents (q-parent-refs db-5a ref-attrs (keys old-eid->h2l-group-variables)))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (map remove-tx (keys old-eid->h2l-group-variables)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->h2l-group-variables))

  ;; TX refs from parents to new Entities
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->h2l-group-variables) h2l-gv-parents))

  ;;; Submodules
  (d/pull db-5b '[* {:group/_children [*]}] 6022)

  ;;; 5B. Subgroups w/ High references

  (def db-5b (d/db @ds/datomic-conn))

  (def subgroups-w-high-refs (q-high-refs-low-eids db-5b group-variable-ref-attrs))

  ;; New entities
  (def old-eid->subgroups-w-high-refs
    (->> 
     (old-eids->new-entities db-5b
                             (map last subgroups-w-high-refs)
                             group-attrs
                             {:new-ids? true})
     (map (fn [[k v]] [k (cond-> v
                           (:group/conditionals v)
                           (update :group/conditionals (partial mapv :db/id))

                           (:group/group-variables v)
                           (update :group/group-variables (partial mapv :db/id)))]))
     (into {})))

  ;; Remove old attributes - necessary to avoid retracting subcomponents
  (d/transact @ds/datomic-conn
              (apply concat (map retract-attrs-tx
                                 (d/pull-many db-5b
                                              '[* {:group/conditionals [:db/id] :group/group-variables [:db/id]}]
                                              (keys old-eid->subgroups-w-high-refs)))))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (map remove-tx (keys old-eid->subgroups-w-high-refs)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->subgroups-w-high-refs))

  ;; TX refs from parents to new Entities
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->subgroups-w-high-refs) subgroups-w-high-refs))


  ;;; 5C. Subgroups w/ Low EID references

  (def db-5c (d/db @ds/datomic-conn))

  (def l2-subgroups (q-low-refs-low-eids db-5c group-variable-ref-attrs))

  ;; New entities
  (def old-eid->l2-subgroups
    (->> 
     (old-eids->new-entities db-5c
                             (map last l2-subgroups)
                             group-attrs
                             {:new-ids? true})
     (map (fn [[k v]] [k (cond-> v
                           (:group/conditionals v)
                           (update :group/conditionals (partial mapv :db/id))

                           (:group/group-variables v)
                           (update :group/group-variables (partial mapv :db/id)))]))
     (into {})))

  ;; Remove old attributes - necessary to avoid retracting subcomponents
  (d/transact @ds/datomic-conn
              (apply concat (map retract-attrs-tx
                                 (d/pull-many db-5c
                                              '[* {:group/conditionals [:db/id] :group/group-variables [:db/id]}]
                                              (keys old-eid->l2-subgroups)))))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (map remove-tx (keys old-eid->l2-subgroups)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->l2-subgroups))

  ;; TX refs from parents to new Entities
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->l2-subgroups) l2-subgroups))

  ;;; 5D. Groups w/ Low EID references

  (def db-5d (d/db @ds/datomic-conn))

  (def l1-groups
    (->> l2-subgroups
         (filter #(low-eid? (first %)))
         (map first)
         (q-parent-refs db-5d ref-attrs)))

  ;; New entities
  (def old-eid->l1-groups
    (->>
     (old-eids->new-entities db-5d
                             (map last l1-groups)
                             group-attrs
                             {:new-ids? true})
     (map (fn [[k v]] [k (cond-> v
                           (:group/conditionals v)
                           (update :group/conditionals (partial mapv :db/id))

                           (:group/children v)
                           (update :group/children (partial mapv :db/id)))]))
     (into {})))

  ;; Remove old attributes - necessary to avoid retracting subcomponents
  (d/transact @ds/datomic-conn
              (apply concat (map retract-attrs-tx
                                 (d/pull-many db-5d
                                              '[* {:group/conditionals [:db/id] :group/children [:db/id]}]
                                              (keys old-eid->l1-groups)))))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (map remove-tx (keys old-eid->l1-groups)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->l1-groups))

  ;; TX refs from parents to new Entities
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->l1-groups) l1-groups))

  ;;; 5E. Groups w/ High references

  (def db-5e (d/db @ds/datomic-conn))

  (def l1-groups-w-high-refs (q-high-refs-low-eids db-5e [:submodule/groups]))

  ;; New entities
  (def old-eid->l1-groups-w-high-refs
    (->>
     (old-eids->new-entities db-5e
                             (map last l1-groups-w-high-refs)
                             group-attrs
                             {:new-ids? true})
     (map (fn [[k v]] [k (cond-> v
                           (:group/conditionals v)
                           (update :group/conditionals (partial mapv :db/id))

                           (:group/children v)
                           (update :group/children (partial mapv :db/id)))]))
     (into {})))

  ;; Remove old attributes - necessary to avoid retracting subcomponents
  (d/transact @ds/datomic-conn
              (apply concat (map retract-attrs-tx
                                 (d/pull-many db-5e
                                              '[* {:group/conditionals [:db/id] :group/children [:db/id]}]
                                              (keys old-eid->l1-groups-w-high-refs)))))

  ;; Remove old entities
  (d/transact @ds/datomic-conn (map remove-tx (keys old-eid->l1-groups-w-high-refs)))

  ;; TX new entities
  (d/transact @ds/datomic-conn (vals old-eid->l1-groups-w-high-refs))

  ;; TX refs from parents to new Entities
  (d/transact @ds/datomic-conn (map (partial remap-tx old-eid->l1-groups-w-high-refs) l1-groups))

  ;;;; 6. Remove Low EIDs w/ dangling `:group-variable/*?` attributes

  (def db-6 (d/db @ds/datomic-conn))

  (def low-eids-dangling-gvs (q-low-eids db [:group-variable/conditionally-set? :group-variable/discrete-multiple?]))

  (d/transact @ds/datomic-conn (map remove-tx low-eids-dangling-gvs))

  ;;;; 7. Verify
  (def db-7 (d/db @ds/datomic-conn))

  (empty? (concat (q-high-refs-low-eids db-7 ref-attrs)
                  (q-low-refs-low-eids db-7 ref-attrs)
                  (q-low-eids db-7 all-attrs)))

  )
