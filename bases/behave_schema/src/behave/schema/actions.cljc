(ns behave.schema.actions
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [not-empty-string?]]))

(s/def :action/name                  not-empty-string?)
(s/def :action/target-value          not-empty-string?)
(s/def :action/type                  #{:select :disable})
(s/def :action/conditionals          (s/coll-of (s/or :ref int?
                                                      :conditoinal :behave/conditional)))
(s/def :action/conditionals-operator #{:and :or})

(s/def :behave/action (s/keys :req [:action/name
                                    :action/type]
                              :opt [:action/target-value]))

(def schema
  [{:db/ident       :action/name
    :db/doc         "Action's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :action/target-value
    :db/doc         "Action's target value."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :action/type
    :db/doc         "Action's type. Can be either: `:select`, or `:disable`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :action/conditionals
    :db/doc         "Action's conditionals. Determines whether the Group should be displayed based on the conditional."
    :db/valueType   :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many}

   {:db/ident       :action/conditionals-operator
    :db/doc         "Action's conditional operator, which only applies for multiple conditionals. Can be either: `:and`, `:or`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}])
