(ns behave.schema.note-category)

(def schema
  [{:db/ident       :note-category/name
    :db/doc         "Note Category's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
