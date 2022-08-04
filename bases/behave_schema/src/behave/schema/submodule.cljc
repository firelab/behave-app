(ns behave.schema.submodule
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn valid-key? [s]
  (re-find #"^[a-z:]+$" s))

(defn valid-io? [io]
  (#{:input :output} io))

;;; Spec

(s/def :submodule/id              uuid?)
(s/def :submodule/name            string?)
(s/def :submodule/io              (s/and keyword? valid-io?))
(s/def :submodule/order           (s/and integer?
                                         #(<= 0 %)))
(s/def :submodule/translation-key (s/and string? valid-key?))
(s/def :submodule/help-key        (s/and string? valid-key?))
(s/def :submodule/module          integer?)
(s/def :submodule/groups          set?)

(s/def :behave/submodule (s/keys :req [:submodule/id
                                       :submodule/io
                                       :submodule/name
                                       :submodule/order
                                       :submodule/translation-key
                                       :submodule/help-key]
                                 :opt [:submodule/module]))

;;; Schema

(def schema
  [{:db/ident       :submodule/id
    :db/doc         "Submodule's ID."
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one}
   {:db/ident       :submodule/module
    :db/doc         "Submodule's parent module."
    :db/valueType   :db.type/ref
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
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident       :submodule/help-key
    :db/doc         "Submodule's help key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/submodule {:submodule/id #uuid "5eb87bf7-9501-4d50-9eee-7d0ffadbb1d0"
                               :submodule/io :output ; :input
                               :submodule/name "Contain"
                               :submodule/order 1
                               :submodule/translation-key "behaveplus:contain"
                               :submodule/help-key "behaveplus:contain:help"})

  )

