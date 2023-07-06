(ns behave.schema.submodule
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [valid-key? uuid-string? zero-pos?]]))

;;; Validation Fns

(def valid-io? (s/and keyword? #(#{:input :output} %)))

;;; Spec

(s/def :submodule/uuid            uuid-string?)
(s/def :submodule/name            string?)
(s/def :submodule/io              valid-io?)
(s/def :submodule/order           zero-pos?)
(s/def :submodule/translation-key valid-key?)
(s/def :submodule/help-key        valid-key?)
(s/def :submodule/groups          set?)

(s/def :behave/submodule (s/keys :req [:submodule/uuid
                                       :submodule/io
                                       :submodule/name
                                       :submodule/order
                                       :submodule/translation-key
                                       :submodule/help-key]))

;;; Schema

(def schema
  [{:db/ident       :submodule/uuid
    :db/doc         "Submodule's ID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :submodule/io
    :db/doc         "Submodule's input/output status."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :submodule/name
    :db/doc         "Submodule's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :submodule/order
    :db/doc         "Submodule's order."
    :db/valueType   :db.type/number
    :db/cardinality :db.cardinality/one}

   {:db/ident       :submodule/groups
    :db/doc         "Subodule's groups."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :submodule/translation-key
    :db/doc         "Submodule's translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :submodule/help-key
    :db/doc         "Submodule's help key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :submodule/conditionals
    :db/doc         "Submodule's conditionals. Determines whether the Group should be displayed based on the conditional."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :submodule/conditionals-operator
    :db/doc         "Submodule's conditional operator, which only applies for multiple conditionals. Can be either: `:and`, `:or`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/submodule {:submodule/uuid            (str (random-uuid))
                               :submodule/io              :output ; :input
                               :submodule/name            "Fire"
                               :submodule/order           1
                               :submodule/translation-key "behaveplus:contain:fire"
                               :submodule/help-key        "behaveplus:contain:fire:help"}))

