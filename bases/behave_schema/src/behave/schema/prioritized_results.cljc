(ns behave.schema.prioritized-results)

(def schema
  [{:db/ident       :prioritized-results/group-variable
    :db/doc         "Prioritzed-result's group variable"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :prioritized-results/order
    :db/doc         "Prioritzed-result's order"
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}])
