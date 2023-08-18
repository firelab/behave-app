(ns behave.schema.subtool
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [valid-key? uuid-string? zero-pos?]]))

;;; Spec

(s/def :subtool/uuid                     uuid-string?)
(s/def :subtool/name                     string?)
(s/def :subtool/order                    zero-pos?)
(s/def :subtool/translation-key          valid-key?)
(s/def :subtool/help-key                 valid-key?)
(s/def :subtool/input-variables  set?)
(s/def :subtool/output-variables set?)

(s/def :behave/subtool (s/keys :req [:bp/uuid
                                     :subtool/name
                                     :subtool/order
                                     :subtool/translation-key
                                     :subtool/help-key]
                               :opt [:subtool/input-variables
                                     :subtool/output-variables]))

;;; Schema

(def schema
  [{:db/ident       :subtool/name
    :db/doc         "Subtool's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/order
    :db/doc         "Subtool's order."
    :db/valueType   :db.type/number
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/input-variables
    :db/doc         "Subtool's input variables."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :subtool/output-variables
    :db/doc         "Subtool's output variables."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :subtool/translation-key
    :db/doc         "Subtool's translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/help-key
    :db/doc         "Subtool's help key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/cpp-namespace-uuid
    :db/doc         "Subtool calculation function's uuid ref to it's C++ namespace."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/cpp-class-uuid
    :db/doc         "Subtool calculation function's ref to it's C++ class."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/cpp-function-uuid
    :db/doc         "Subtool calculation function's ref to it's C++ function."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(comment
  (s/valid? :behave/subtool {:bp/uuid                 (str (random-uuid))
                             :subtool/name            "Relative Humidity"
                             :subtool/order           1
                             :subtool/translation-key "behaveplus:relative-humidity:dry-temp-wet-temp-elevation"
                             :subtool/help-key        "behaveplus:relative-humidity:dry-temp-wet-temp-elevation:help"}))
