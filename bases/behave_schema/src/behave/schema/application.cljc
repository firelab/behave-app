(ns behave.schema.application
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string?]]))

;;; Spec

(s/def :application/uuid            uuid-string?)
(s/def :application/name            string?)
(s/def :application/version-major   integer?)
(s/def :application/version-minor   integer?)
(s/def :application/version-patch   integer?)
(s/def :application/help-key        valid-key?)
(s/def :application/modules         set?)

(s/def :behave/application (s/keys :req [:application/uuid
                                         :application/name
                                         :application/version-major
                                         :application/version-minor
                                         :application/version-patch
                                         :application/translation-key
                                         :application/help-key]
                                   :opt [:application/modules]))

;;; Schema

(def schema
  [{:db/ident       :application/uuid
    :db/doc         "Application's UUID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :application/name
    :db/doc         "Application's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :application/version-major
    :db/doc         "Application's major version."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :application/version-minor
    :db/doc         "Application's minor version."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :application/version-patch
    :db/doc         "Application's patch version."
    :db/valueType   :db.type/long
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

   {:db/ident       :application/tools
    :db/doc         "Application's tools."
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
  (s/valid? :behave/application {:application/uuid            (str (random-uuid))
                                 :application/name            "BehavePlus"
                                 :application/version-major   7
                                 :application/version-minor   0
                                 :application/version-patch   0
                                 :application/translation-key "behaveplus"
                                 :application/help-key        "behaveplus:help"}))
