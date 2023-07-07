(ns behave.schema.conditionals
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string?]]))

(s/def :conditional/group-variable-uuid uuid-string?)
(s/def :conditional/operator            keyword?)
(s/def :conditional/values              (s/every string?))

(def schema
  [{:db/ident       :conditional/group-variable-uuid
    :db/doc         "Conditional's group variable UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :conditional/type
    :db/doc         "Conditional's type. Can be either: `:module` or `:group-variable`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :conditional/operator
    :db/doc         "Conditional's operator. Can be either: `:equals`, `:not-equals`, `:in`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :conditional/values
    :db/doc         "Conditional's values which can be matched against. When `:conditional/type` is `:module`, values are the lower cased strings of the modules (e.g. ['surface' 'crown']). Otherwise, values are the values that the group-variable may hold (e.g 'true'/'false'). The `:group-conditional/operator` determines the comparison method."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many}])

