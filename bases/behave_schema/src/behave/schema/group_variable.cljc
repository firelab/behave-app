(ns behave.schema.group-variable
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn valid-key? [s]
  (re-find #"^[a-z:]+$" s))

;;; Spec

(s/def :group-variable/id              uuid?)
(s/def :group-variable/cpp-class       pos?)
(s/def :group-variable/cpp-function    pos?)
(s/def :group-variable/cpp-namespace   pos?)
(s/def :group-variable/cpp-parameter   pos?)
(s/def :group-variable/help-key        (s/and string? valid-key?))
(s/def :group-variable/order           (s/and integer? #(<= 0 %)))
(s/def :group-variable/translation-key (s/and string? valid-key?))

(s/def :behave/group (s/keys :req [:group-variable/id
                                   :group-variable/order
                                   :group-variable/translation-key
                                   :group-variable/help-key
                                   :group-variable/cpp-class
                                   :group-variable/cpp-function
                                   :group-variable/cpp-namespace
                                   :group-variable/cpp-parameter]
                             :opt []))

;;; Schema

(def schema
  [{:db/ident       :group-variable/cpp-namespace
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
   {:db/ident       :group-variable/order
    :db/doc         "Group variable's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident       :group-variable/translation-key
    :db/doc         "Group variable's translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident       :group-variable/help-key
    :db/doc         "Group variable's help key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}])
