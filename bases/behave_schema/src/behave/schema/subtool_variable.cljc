(ns behave.schema.subtool-variable
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? valid-io? zero-pos?]]))

;;; Spec

(s/def :subtool-variable/cpp-class-uuid     string?)
(s/def :subtool-variable/cpp-function-uuid  string?)
(s/def :subtool-variable/cpp-namespace-uuid string?)
(s/def :subtool-variable/cpp-parameter-uuid string?)
(s/def :subtool-variable/help-key           valid-key?)
(s/def :subtool-variable/order              zero-pos?)
(s/def :subtool-variable/translation-key    valid-key?)
(s/def :subtool-variable/io                 valid-io?)

(s/def :behave/subtool-variable (s/keys :req [:bp/uuid
                                              :subtool-variable/io
                                              :subtool-variable/order
                                              :subtool-variable/translation-key
                                              :subtool-variable/help-key
                                              :subtool-variable/cpp-class
                                              :subtool-variable/cpp-namespace
                                              :subtool-variable/cpp-function]
                                        :opt [:subtool-variable/cpp-parameter]))

;;; Schema

(def schema
  [{:db/ident       :subtool-variable/io
    :db/doc         "subtool variable's io #{:input :output}"
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool-variable/order
    :db/doc         "subtool variable's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool-variable/dynamic-units?
    :db/doc         "Whether a subtool variable allow units change dropdown"
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool-variable/cpp-namespace-uuid
    :db/doc         "subtool variable's uuid ref to it's C++ namespace."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool-variable/cpp-class-uuid
    :db/doc         "subtool variable's uuid ref to it's C++ class."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool-variable/cpp-function-uuid
    :db/doc         "subtool variable's uuid ref to it's C++ function."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool-variable/cpp-parameter-uuid
    :db/doc         "subtool variable's uuid ref tor it's C++ parameter."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool-variable/translation-key
    :db/doc         "subtool variable's translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool-variable/help-key
    :db/doc         "subtool variable's help key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}])

;;; Tests

(comment
  (s/explain :behave/subtool-variable {:bp/uuid                          (str (random-uuid))
                                       :subtool-variable/io              :input
                                       :subtool-variable/order           0
                                       :subtool-variable/translation-key "behaveplus:relative-humidity:dry-temp-wet-temp-elevation:var"
                                       :subtool-variable/help-key        "behaveplus:relative-humidity:dry-temp-wet-temp-elevation:var:help"
                                       :subtool-variable/cpp-class       "BehaveRelativeHumidity"
                                       :subtool-variable/cpp-namespace   "global"
                                       :subtool-variable/cpp-function    "setDryBulbTemp"})
  )
