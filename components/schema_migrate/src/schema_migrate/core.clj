(ns schema-migrate.core
  (:require
   [clojure.walk :as walk]
   [datascript.core :refer [squuid]]
   [datomic.api :as d]
   [datomic-store.main :as ds]
   [nano-id.core :refer [nano-id]]))

(def
  ^{:doc "Random UUID in string format."}
  rand-uuid (comp str squuid))

(defn name->uuid
  "Get the :bp/uuid using the name for the specified name attribute"
  [conn attr nname]
  (d/q '[:find ?uuid .
         :in $ ?attr ?name
         :where
         [?e ?attr ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       attr
       nname))

(defn name->nid
  "Get the :bp/nid using the name for the specified name attribute"
  [conn attr nname]
  (d/q '[:find ?nid .
         :in $ ?attr ?name
         :where
         [?e ?attr ?name]
         [?e :bp/nid ?nid]]
       (d/db conn)
       attr
       nname))

(defn name->eid
  "Get the datomic entity id given the attribute and name. May not work as expected if attr and name
  is not unique."
  [conn attr nname]
  (d/q '[:find ?e .
         :in $ ?attr ?name
         :where [?e ?attr ?name]]
       (d/db conn)
       attr
       nname))

(defn cpp-ns->uuid
  "Get the :bp/uuid using the cpp namepsace name"
  [conn nname]
  (d/q '[:find ?uuid .
         :in $ ?name
         :where
         [?e :cpp.namespace/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       nname))

(defn cpp-class->uuid
  "Get the :bp/uuid using the cpp class name"
  [conn nname]
  (d/q '[:find ?uuid .
         :in $ ?name
         :where
         [?e :cpp.class/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       nname))

(defn cpp-fn->uuid
  "Get the :bp/uuid using the cpp function name"
  [conn nname]
  (d/q '[:find ?uuid .
         :in $ ?name
         :where
         [?e :cpp.function/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       nname))

(defn- submodule [conn t]
  (->> t
       (d/q '[:find ?e .
              :in $ ?t
              :where [?e :submodule/translation-key ?t]]
            (ds/unwrap-db conn)
            t)
       (d/entity (ds/unwrap-db conn))))

(defn t-key->uuid
  "Get the :bp/uuid using translation-key"
  [conn t]
  (when-let [e (or (d/entity (ds/unwrap-db conn) [:group-variable/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:group-variable/result-translation-key t])
                   (d/entity (ds/unwrap-db conn) [:group/translation-key t])
                   (d/entity (ds/unwrap-db conn) [:module/translation-key t])
                   (submodule conn t))]
    (:bp/uuid (d/touch e))))

(defn t-key->entity
  "Get the datomic entity given a translation-key"
  [conn t]
  (d/entity (ds/unwrap-db conn) [:bp/uuid (t-key->uuid conn t)]))

(defn t-key->eid
  "Get the db/id using translation-key"
  [conn t]
  (:db/id (t-key->entity conn t)))

(defn make-attr-is-component-payload
  "Returns a payload for updating a given attribute to include :db/isComponent true"
  [conn attr]
  (when-let [eid (d/q '[:find ?e .
                        :in $ ?attr
                        :where [?e :db/ident ?attr]]
                      (ds/unwrap-db conn)
                      attr)]
    {:db/id          eid
     :db/isComponent true}))

(defn make-attr-is-component!
  [conn attr]
  (ds/transact conn [(make-attr-is-component-payload conn attr)]))

(defn rollback-tx!
  "Given a transaction ID or a transaction result (return from datomic.api/transact), Reassert
  retracted datoms and retract asserted datoms in a transaction, effectively \"undoing\" the
  transaction."
  [conn tx]
  (when-let [tx-id (cond
                     (number? tx) tx
                     (map? tx)    (nth (first (:tx-data tx)) 3)
                     :else        nil)]
    (let [tx-log  (-> conn ds/unwrap-conn d/log (d/tx-range tx-id nil) first) ; find the transaction
          txid    (-> tx-log :t d/t->tx) ; get the transaction entity id
          newdata (->> (:data tx-log)   ; get the datoms from the transaction
                       (remove #(= (:e %) txid)) ; remove transaction-metadata datoms
                                        ; invert the datoms add/retract state.
                       (map #(do [(if (:added %) :db/retract :db/add) (:e %) (:a %) (:v %)]))
                       reverse)] ; reverse order of inverted datoms.
      (ds/transact conn newdata))))

(defn- insert-bp-uuid [x]
  (if (map? x)
    (assoc x :bp/uuid (str (squuid)))
    x))

(defn- insert-bp-nid [x]
  (if (map? x)
    (assoc x :bp/nid (nano-id))
    x))

(defn postwalk-insert
  "Postwalk over data and insert bp/uuid and bp/nid into every map form"
  [data]
  (->> data
       (walk/postwalk insert-bp-uuid)
       (walk/postwalk insert-bp-nid)))

(defn build-translations-payload
  "Given a map of translation-key to it's translation create a payload that creates these
  translation entities as well as adding these refs to the exisitng Enlgish language entity.
  `eid-start` is optional and will be used as the starting eid for the
  translation entities (prevents eid overlap if you wish to include this payload
  in the same transaction where you've manually assigned :db/id). "
  ([conn t-key->translation-map]
   (build-translations-payload conn 0 t-key->translation-map))

  ([conn eid-start t-key->translation-map]
   (let [translations              (map-indexed (fn [idx [t-key translation]]
                                                  {:db/id                   (-> idx
                                                                                inc
                                                                                (+ eid-start)
                                                                                (* -1))
                                                   :translation/key         t-key
                                                   :translation/translation translation})
                                                t-key->translation-map)
         english-language-eid      (d/q '[:find ?e .
                                          :in $
                                          :where
                                          [?e :language/name "English"]]
                                        (d/db conn))
         language-translation-refs {:db/id                english-language-eid
                                    :language/translation (map :db/id translations)}]
     (into [language-translation-refs]
           translations))))

(defn ->gv-conditional
  "Payload for a Group Variable Conditional."
  [uuid operator value]
  {:bp/uuid                         (rand-uuid)
   :bp/nid                          (nano-id)
   :conditional/group-variable-uuid uuid
   :conditional/type                :group-variable
   :conditional/operator            operator
   :conditional/values              value})

(defn ->module-conditional
  "Payload for a Module Conditional."
  [operator values]
  {:bp/uuid              (rand-uuid)
   :bp/nid               (nano-id)
   :conditional/type     :module
   :conditional/operator operator
   :conditional/values   values})
