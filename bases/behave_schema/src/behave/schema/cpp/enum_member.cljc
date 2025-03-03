(ns behave.schema.cpp.enum-member
  (:require [clojure.spec.alpha :as s]))

(s/def :cpp.enum-member/name  string?)
(s/def :cpp.enum-member/value number?)

(s/def :cpp/enum-member (s/keys :req [:cpp.enum-member/name
                                      :cpp.enum-member/value]))

(def schema
  [{:db/ident       :cpp.enum-member/name
    :db/doc         "Enum member name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :cpp.enum-member/value
    :db/doc         "Enum member value."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}])
