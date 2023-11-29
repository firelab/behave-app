(ns behave.schema.category)

(def schema
  [{:db/ident       :category/name
    :db/doc         "Category's name"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
