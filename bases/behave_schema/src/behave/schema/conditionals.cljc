(ns behave.schema.conditionals
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string?]]))

(s/def :conditional/group-variable-uuid uuid-string?)
(s/def :conditional/operator            keyword?)
(s/def :conditional/values              (s/every string?))

(def schema
  [{:db/ident       :conditional/group-variable-uuid
    :db/doc         "Group conditional's group variable UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :conditional/operator
    :db/doc         "Group conditional's operator. Can be either: `:equals`, `:not-equals`, `:in`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :conditional/values
    :db/doc         "Group conditional's values which can be matched against the `:group-conditional/operator`."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many}])

