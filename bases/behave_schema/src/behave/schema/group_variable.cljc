(ns behave.schema.group-variable
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string? zero-pos?]]))

;;; Spec

(s/def :group-variable/uuid            uuid-string?)
(s/def :group-variable/cpp-class       string?)
(s/def :group-variable/cpp-function    string?)
(s/def :group-variable/cpp-namespace   string?)
(s/def :group-variable/cpp-parameter   string?)
(s/def :group-variable/help-key        valid-key?)
(s/def :group-variable/order           zero-pos?)
(s/def :group-variable/translation-key valid-key?)
(s/def :group-variable/research?       boolean?)

(s/def :behave/group-variable (s/keys :req [:group-variable/uuid
                                            :group-variable/order
                                            :group-variable/translation-key
                                            :group-variable/help-key
                                            :group-variable/cpp-class
                                            :group-variable/cpp-namespace
                                            :group-variable/cpp-function]
                                      :opt [:group-variable/cpp-parameter
                                            :group-variable/research?]))

;;; Schema

(def schema
  [{:db/ident       :group-variable/uuid
    :db/doc         "Group variable's UUID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/cpp-namespace
    :db/doc         "Group variable's C++ namespace."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/cpp-class
    :db/doc         "Group variable's C++ class."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/cpp-function
    :db/doc         "Group variable's C++ function."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/cpp-parameter
    :db/doc         "Group variable's C++ parameter."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/research?
    :db/doc         "Whether a Group Variable is for research."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/discrete-multiple?
    :db/doc         "Whether a Group Variable is a multi discrete variable."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/order
    :db/doc         "Group variable's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/translation-key
    :db/doc         "Group variable's translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/result-translation-key
    :db/doc         "Group variable's second translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/help-key
    :db/doc         "Group variable's help key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group-variable/actions
    :db/doc         "Group variable's actions."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])

;;; Tests

(comment
  (s/explain :behave/group-variable {:group-variable/uuid           (str (random-uuid))
                                    :group-variable/order           0
                                    :group-variable/translation-key "behave:contain:fire:group:var"
                                    :group-variable/help-key        "behave:contain:fire:group:var:help"
                                    :group-variable/cpp-class       "BehaveContain"
                                    :group-variable/cpp-namespace   "global"
                                    :group-variable/cpp-function    "setFireSize"})
)
