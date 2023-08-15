(ns behave.schema.subtool
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [valid-key? uuid-string? zero-pos?]]))

;;; Spec

(s/def :subtool/uuid                     uuid-string?)
(s/def :subtool/name                     string?)
(s/def :subtool/order                    zero-pos?)
(s/def :subtool/translation-key          valid-key?)
(s/def :subtool/help-key                 valid-key?)
(s/def :subtool/input-subtool-variables  set?)
(s/def :subtool/output-subtool-variables set?)

(s/def :behave/subtool (s/keys :req [:subtool/uuid
                                     :subtool/name
                                     :subtool/order
                                     :subtool/translation-key
                                     :subtool/help-key]
                               :opt [:subtool/input-subtool-variables
                                     :subtool/input-subtool-variables]))

;;; Schema

(def schema
  [{:db/ident       :subtool/uuid
    :db/doc         "Subtool's ID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/name
    :db/doc         "Subtool's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/order
    :db/doc         "Subtool's order."
    :db/valueType   :db.type/number
    :db/cardinality :db.cardinality/one}

   {:db/ident       :subtool/input-subtool-variables
    :db/doc         "Subtool's input variables."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :subtool/output-subtool-variables
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
    :db/cardinality :db.cardinality/one}])

(comment
  (s/valid? :behave/subtool {:subtool/uuid            (str (random-uuid))
                             :subtool/name            "Relative Humidity"
                             :subtool/order           1
                             :subtool/translation-key "behaveplus:relative-humidity:dry-temp-wet-temp-elevation"
                             :subtool/help-key        "behaveplus:relative-humidity:dry-temp-wet-temp-elevation:help"}))
