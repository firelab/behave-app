(ns behave.schema.cpp.enum
  (:require [clojure.spec.alpha :as s]))

(s/def :enum/name    string?)
(s/def :enum/members set?)

(s/def :cpp/enum (s/keys :req [:enum/name
                               :enum/members]))

(def schema
  [{:db/ident       :enum/name
    :db/doc         "Enum's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :enum/enum-members
    :db/doc         "Enum's members."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])
