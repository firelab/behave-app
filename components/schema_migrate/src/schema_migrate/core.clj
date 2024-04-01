(ns schema-migrate.core
  (:require [datahike.api :as d]))

(defn migrate-attr-is-component
  "Sets :db/isComponent true for a given schema attribute"
  [conn attr]
  (let [eid (d/q '[:find ?e .
                   :in $ ?attr
                   :where [?e :db/ident ?attr]]
                 @conn
                 attr)]
    (d/transact conn [{:db/id          eid
                       :db/isComponent true}])))
