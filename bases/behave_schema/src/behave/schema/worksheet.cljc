(ns behave.schema.worksheet)

;;; Schema

(def schema
  [{:db/ident       :worksheet/id
    :db/doc         "Worksheet's ID."
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one}
   {:db/ident       :worksheet/name
    :db/doc         "Worksheet's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :worksheet/notes
    :db/doc         "Worksheet's notes."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :worksheet/inputs
    :db/doc         "Worksheet's inputs."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :worksheet/outputs
    :db/doc         "Worksheet's outputs."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :worksheet/results
    :db/doc         "Worksheet's results."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :worksheet/graph-settings
    :db/doc         "Worksheet's graph settings."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :worksheet/table-settings
    :db/doc         "Worksheet's table settings."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def notes-schema
  [{:db/ident       :note/submodule
    :db/doc         "Note's name."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :note/content
    :db/doc         "Note's content."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(def inputs--schema
  [{:db/ident       :input/variable
    :db/doc         "Note's variable."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :note/kind
    :db/doc         "Note's kind."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}])
