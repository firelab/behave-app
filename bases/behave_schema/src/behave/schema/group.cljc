(ns behave.schema.group
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn valid-key? [s]
  (re-find #"^[a-z:]+$" s))

;;; Spec

(s/def :group/id              uuid?)
(s/def :group/name            string?)
(s/def :group/order           (s/and integer? #(<= 0 %)))
(s/def :group/translation-key (s/and string? valid-key?))
(s/def :group/help-key        (s/and string? valid-key?))
(s/def :group/children        set?)
(s/def :group/variables       set?)

(s/def :behave/group (s/keys :req [:group/id
                                   :group/name
                                   :group/order
                                   :group/translation-key
                                   :group/help-key]
                             :opt [:group/children
                                   :group/variables]))

;;; Schema

(def schema
  [{:db/ident       :group/children
    :db/doc         "Group's children groups."
    :db/valueType   :db.type/ref
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
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/group {:group/id #uuid "5eb87bf7-9501-4d50-9eee-7d0ffadbb1d0"
                               :group/name "Fire"
                               :group/order 1
                               :group/translation-key "behaveplus:fire"
                               :group/help-key "behaveplus:fire:help"})

  )

