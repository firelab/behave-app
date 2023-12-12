(ns behave.schema.domain)

(def schema
  [{:db/ident       :domain/name
    :db/doc         "Domain's name"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
