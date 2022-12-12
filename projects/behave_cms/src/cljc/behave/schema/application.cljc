(ns behave.schema.application
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn valid-key? [s]
  (re-find #"^[a-z:]+$" s))

;;; Spec

(s/def :application/id              uuid?)
(s/def :application/name            string?)
(s/def :application/version-major   integer?)
(s/def :application/version-minor   integer?)
(s/def :application/version-patch   integer?)
(s/def :application/help-key        (s/and string? valid-key?))
(s/def :application/modules         set?)

(s/def :behave/module (s/keys :req [:application/id
                                    :application/name
                                    :application/version-major
                                    :application/version-minor
                                    :application/version-patch
                                    :application/translation-key
                                    :application/help-key]
                              :opt [:application/modules]))

;;; Schema

(def schema
  [{:db/ident       :application/name
    :db/doc         "Application's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :application/version-major
    :db/doc         "Application's major version."
    :db/valueType   :db.type/number
    :db/cardinality :db.cardinality/one}
   {:db/ident       :application/version-minor
    :db/doc         "Application's minor version."
    :db/valueType   :db.type/number
    :db/cardinality :db.cardinality/one}
   {:db/ident       :application/version-patch
    :db/doc         "Application's patch version."
    :db/valueType   :db.type/number
    :db/cardinality :db.cardinality/one}
   {:db/ident       :application/version
    :db/doc         "Application's version."
    :db/valueType   :db.type/tuple
    :db/tupleAttrs  [:application/version-major
                     :application/version-minor
                     :application/version-patch]
    :db/cardinality :db.cardinality/one}
   {:db/ident       :application/modules
    :db/doc         "Application's modules."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :application/translation-key
    :db/doc         "Application's translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident       :application/help-key
    :db/doc         "Application's help key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/module {:module/id #uuid "5eb87bf7-9501-4d50-9eee-7d0ffadbb1d0"
                            :module/name "Contain"
                            :module/order 1
                            :module/translation-key "behaveplus:contain"
                            :module/help-key "behaveplus:contain:help"})

)
