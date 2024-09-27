(ns behave.schema.group-variable-order-override)

(def schema
  [{:db/ident       :group-variable-order-override/group-variable
    :db/doc         ""
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable-order-override/order
    :db/doc         ""
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}])
