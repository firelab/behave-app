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

(defn- q-refs-low-eids
  "Find all parents of `attrs` of matching `eids` that have high EID's."
  [db ref-attrs]
  (d/q '[:find ?e1 ?a ?e2
         :in $ [?a ...]
         :where
         [?e1 ?a ?e2]
         [(< 1000 ?e2)]
         [(> 10000 ?e2)]] db ref-attrs))

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
  (def db-1 db)
  (def cond-refs (q-refs-low-eids db-1 cond-ref-attrs))
  (def new-conds
    (old-eids->new-entities db-1
                            (map last cond-refs)
                            cond-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn cond-refs new-conds)

  ;;;; 2. Actions

  (def db-2 (d/db conn))
  (def action-refs (q-high-refs-low-eids db-2 [:group-variable/actions]))
  (def new-actions
    (old-eids->new-entities db-2
                            (map last action-refs)
                            action-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn action-refs new-actions)

  ;;;; 3. Translations

  (def db-3 (d/db @ds/datomic-conn))

  (def english (d/q '[:find ?e . :where [?e :language/shortcode "en-US"]] db-3))

  (def translation-refs (q-high-refs-low-eids db-3 [:language/translation]))
  (def english-translations (remove #(not= (first %) english) translation-refs))
  (def new-translations
    (old-eids->new-entities db-3
                            (map last english-translations)
                            translation-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn english-translations new-translations)

  (def non-english-translations (filter #(not= (first %) english) translation-refs))
  (d/transact conn (map remove-tx (map last non-english-translations)))

  ;;;; 4. List Options

  (def db-4 (d/db @ds/datomic-conn))

  (def list-option-refs (q-refs-low-eids db [:list/options]))
  (def new-list-options
    (old-eids->new-entities db-4
                            (map last list-option-refs)
                            list-option-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn list-option-refs new-list-options)

  ;;; 5. Group Variables, Subgroups, Groups
  ;;; 5A. Group Variables

  (def db-5a (d/db @ds/datomic-conn))

  (def gv-refs (q-refs-low-eids db-5a group-variable-ref-attrs))

  ;; New entities
  (def new-group-variables
    (old-eids->new-entities db-5a
                            (->> gv-refs
                                 (filter #(= :variable/group-variables (second %)))
                                 (map last))
                            group-variable-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (def gv-parents (q-parent-refs db-5a ref-attrs (keys new-group-variables)))

  (refresh-entities! conn gv-parents new-group-variables)

  ;;; 5B. Subgroups

  (def db-5b (d/db conn))

  (def subgroup-refs (q-refs-low-eids db-5b [:group/children]))

  ;; New entities
  (def new-subgroups
     (old-eids->new-entities db-5b
                             (->> subgroup-refs
                                  (filter #(= :group/children (second %)))
                                  (map last))
                             group-attrs
                             {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn subgroup-refs new-subgroups)

  ;;; 5C. Groups

  (def db-5c (d/db @ds/datomic-conn))
  (def group-refs (q-refs-low-eids db-5c [:submodule/groups]))

  (def new-groups
    (old-eids->new-entities db-5c
                            (->> group-refs
                                 (filter #(= :submodule/groups (second %)))
                                 (map last))
                            group-attrs
                            {:new-ids? true :ref-attrs ref-attrs}))

  (refresh-entities! conn group-refs new-groups)

  ;;;; 6. Remove Low EIDs w/ dangling `:group-variable/*?` attributes

  (def db-6 (d/db @ds/datomic-conn))

  (def low-eids-dangling-gvs
    (q-low-eids db-6
                [:group-variable/conditionally-set?
                 :group-variable/discrete-multiple?]))

  (d/transact @ds/datomic-conn (map remove-tx low-eids-dangling-gvs))

  ;;;; 7. Remove remaining entities
  (def db-7 (d/db conn))

  (def remaining-keys #{:db/id :bp/nid :bp/uuid})

  (def remaining-eids
    (->> (q-low-eids db-7 all-attrs)
         (d/pull-many db-7 '[*])
         (filter #(= remaining-keys (set (keys %))))
         (map :db/id)))

  (d/transact conn (map remove-tx remaining-eids))

  ;;;; 8. Verify
  (def db-8 (d/db conn))

  (empty? (concat (q-high-refs-low-eids db-8 ref-attrs)
                  (q-low-refs-low-eids db-8 ref-attrs)
                  (q-low-eids db-8 all-attrs)))

  )
