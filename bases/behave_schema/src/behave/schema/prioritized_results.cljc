(ns behave.schema.prioritized-results)

(def schema
  [{:db/ident       :prioritized-results/group-variable
    :db/doc         "The results order override's group variable"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :prioritized-results/order
    :db/doc         "The results order override's order"
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}])
