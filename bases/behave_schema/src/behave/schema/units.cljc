(ns behave.schema.units
  (:require [clojure.spec.alpha :as s]))

;;; Specs

;; Dimension

(s/def :dimension/name          string?)
(s/def :dimension/units         (s/and set? #(every? integer? %)))
(s/def :dimension/cpp-enum-uuid string?)

(s/def :behave/dimension        (s/keys :req [:bp/uuid
                                              :dimension/units
                                              :dimension/cpp-enum-uuid]))

;; Unit

(s/def :unit/name                 string?)
(s/def :unit/short-code           string?)
(s/def :unit/system               #{:metric :english :time})
(s/def :unit/cpp-enum-member-uuid string?)

(s/def :behave/unit              (s/keys :req [:bp/uuid
                                               :unit/name
                                               :unit/short-code
                                               :unit/cpp-enum-member-uuid]
                                         :opt [:unit/system]))

;;; Schema

(def schema
  [
   ;; Dimension
   {:db/ident       :dimension/name
    :db/doc         "Dimension's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :dimension/units
    :db/doc         "Dimension's units."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :dimension/cpp-enum-uuid
    :db/doc         "Dimension's CPP Enum UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Units

   {:db/ident       :unit/name
    :db/doc         "Unit's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :unit/short-code
    :db/doc         "Unit's short code (e.g. 'm', 'ft')."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :unit/short-code
    :db/doc         "[Optional] Unit's system (one of: `:metric`, `:english`, `:time`)."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :dimension/cpp-enum-member-uuid
    :db/doc         "Dimension's CPP Enum Member UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
