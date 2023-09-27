(ns behave.schema.diagrams)

(def schema
  [{:db/ident       :diagram/type
    :db/doc         "Keyword #{:contain :fire-shape :wind-slope-spread-direction}"
    :db/valueType   :db.type/keyword
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/group-variable
    :db/doc         "Diagram's reference to the output group variable."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/output-group-variables
    :db/doc         "Diagram output group-variables to show in summary table"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :diagram/input-group-variables
    :db/doc         "Diagram input group-variables to show in summary table"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])
