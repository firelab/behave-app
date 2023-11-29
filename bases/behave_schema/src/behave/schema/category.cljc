(ns behave.schema.category)

(def schema
  [{:db/ident       :category/uuid
    :db/doc         "Category's UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :category/name
    :db/doc         "Category's name"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
