(ns behave.schema.cpp.parameter
  (:require [clojure.spec.alpha :as s]))

(s/def :cpp.parameter/name  string?)
(s/def :cpp.parameter/order (s/and integer? #(<= 0 %)))
(s/def :cpp.parameter/type  string?)

(s/def :cpp/parameter (s/keys :req [:cpp.parameter/name
                                    :cpp.parameter/order
                                    :cpp.parameter/type]))

(def schema
  [{:db/ident       :cpp.parameter/name
    :db/doc         "Parameter's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :cpp.parameter/order
    :db/doc         "Parameter's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :cpp.parameter/type
    :db/doc         "Parameter's return type."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
