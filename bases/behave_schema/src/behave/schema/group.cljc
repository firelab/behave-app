(ns behave.schema.group
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string?]]))

;;; Spec

(s/def :bp/uuid                      uuid-string?)
(s/def :bp/nid                       string?)
(s/def :group/name                   string?)
(s/def :group/order                  (s/and integer? #(<= 0 %)))
(s/def :group/translation-key        (s/and string? valid-key?))
(s/def :group/help-key               (s/and string? valid-key?))
(s/def :group/children               (s/coll-of (s/or :behave/group :behave/group
                                                      :ref  int?)))
(s/def :group/group-variables        (s/coll-of (s/or :behave/group-variable :behave/group-variable
                                                      :ref                  int?)))
(s/def :group/research?              boolean?)
(s/def :group/conditionals           (s/coll-of (s/or :behave/conditional :behave/conditional
                                                      :int int?)))
(s/def :group/conditionals-operator  #{:and :or})
(s/def :group/hidden?                boolean?)


(s/def :behave/group (s/keys :req [:bp/uuid
                                   :bp/nid
                                   :group/name
                                   :group/order
                                   :group/translation-key
                                   :group/help-key]
                             :opt [:group/children
                                   :group/group-variables
                                   :group/hidden?
                                   :group/research?
                                   :group/conditionals]))

;;; Schema

#_{:clj-kondo/ignore [:missing-docstring]}
(def schema
  [{:db/ident       :group/children
    :db/doc         "Group's children groups."
    :db/valueType   :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many}

   {:db/ident       :group/group-variables
    :db/doc         "Group's group variables."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident       :group/name
    :db/doc         "Group's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/order
    :db/doc         "Group's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/results-order
    :db/doc         "Group's order in Results."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/repeat?
    :db/doc         "Whether a Group repeats."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/research?
    :db/doc         "Whether a Group represents a research-only group."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/single-select?
    :db/doc         "Whether a Group allows only one group-variable to be set"
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/hidden?
    :db/doc         "Whether this group should always be hidden"
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/max-repeat
    :db/doc         "Group's maximum number of repeats."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/translation-key
    :db/doc         "Group's translation key in the wizard."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/result-translation-key
    :db/doc         "Group's translation key in the results page."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/help-key
    :db/doc         "Group's help key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/conditionals
    :db/doc         "Group's conditionals. Determines whether the Group should be displayed based on the conditional."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident       :group/conditionals-operator
    :db/doc         "Group's conditional operator, which only applies for multiple conditionals. Can be either: `:and`, `:or`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/group {:group/uuid            (str (random-uuid))
                           :group/name            "Inner Group"
                           :group/order           1
                           :group/translation-key "behaveplus:fire:inner-group"
                           :group/help-key        "behaveplus:fire:inner-group:help"})
  )
