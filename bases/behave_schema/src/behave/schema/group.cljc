(ns behave.schema.group
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string?]]))

;;; Spec

(s/def :group/uuid                   uuid-string?)
(s/def :group/name                   string?)
(s/def :group/order                  (s/and integer? #(<= 0 %)))
(s/def :group/translation-key        (s/and string? valid-key?))
(s/def :group/help-key               (s/and string? valid-key?))
(s/def :group/children               set?)
(s/def :group/group-variables        set?)
(s/def :group/research?              boolean?)
(s/def :group/conditionals           (s/coll-of int?))
(s/def :group/conditionals-operator  #{:and :or})


(s/def :behave/group (s/keys :req [:group/uuid
                                   :group/name
                                   :group/order
                                   :group/translation-key
                                   :group/help-key]
                             :opt [:group/children
                                   :group/group-variables
                                   :group/research?]))

;;; Schema

(def schema
  [{:db/ident       :group/uuid
    :db/doc         "Group's UUID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/children
    :db/doc         "Group's children groups."
    :db/valueType   :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many}

   {:db/ident       :group/group-variables
    :db/doc         "Group's group variables."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :group/name
    :db/doc         "Group's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   
   {:db/ident       :group/order
    :db/doc         "Group's order."
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

   {:db/ident       :group/max-repeat
    :db/doc         "Group's maximum number of repeats."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :group/translation-key
    :db/doc         "Group's translation key."
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
    :db/cardinality :db.cardinality/many}

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

