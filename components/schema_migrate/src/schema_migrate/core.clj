(ns schema-migrate.core
  (:require [datomic.api :as d]
            [datomic-store.main :as ds]))

(defn migrate-attr-is-component
  "Sets :db/isComponent true for a given schema attribute"
  [conn attr]
  (let [eid (d/q '[:find ?e .
                   :in $ ?attr
                   :where [?e :db/ident ?attr]]
                 (ds/unwrap-db conn)
                 attr)]
    (ds/transact conn [{:db/id          eid
                        :db/isComponent true}])))

(defn migrate-attr-is-unique-identity
  "Sets :db/isComponent true for a given schema attribute"
  [conn attr]
  (let [eid (d/q '[:find ?e .
                   :in $ ?attr
                   :where [?e :db/ident ?attr]]
                 (ds/unwrap-db conn)
                 attr)]
    (ds/transact conn [{:db/id     eid
                        :db/unique :db.unique/identity}])))

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
  [conn t]
  (d/entity (ds/unwrap-db conn) [:bp/uuid (t-key->uuid conn t)]))

(defn t-key->eid
  "Get the db/id using translation-key"
  [conn t]
  (:db/id (t-key->entity conn t)))

(defn rollback-tx
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
