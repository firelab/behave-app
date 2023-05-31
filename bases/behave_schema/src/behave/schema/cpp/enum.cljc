(ns behave.schema.cpp.enum
  (:require [clojure.spec.alpha :as s]))

(s/def :cpp.enum/name   string?)
(s/def :cpp.enum/member set?)

(s/def :cpp/enum (s/keys :req [:cpp.enum/name
                               :cpp.enum/member]))

(def schema
  [{:db/ident       :cpp.enum/name
    :db/doc         "Enum's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :cpp.enum/enum-member
    :db/doc         "Enum's members."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])
