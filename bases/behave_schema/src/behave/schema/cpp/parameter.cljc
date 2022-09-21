(ns behave.schema.cpp.parameter
  (:require [clojure.spec.alpha :as s]))

(s/def :parameter/name  string?)
(s/def :parameter/order (s/and integer? #(<= 0 %)))
(s/def :parameter/type  string?)

(s/def :cpp/parameter (s/keys :req [:parameter/name
                                    :parameter/order
                                    :parameter/type]))

(def schema
  [{:db/ident       :parameter/name
    :db/doc         "Parameter's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :parameter/order
    :db/doc         "Parameter's order."
    :db/valueType   :db.type/number
    :db/cardinality :db.cardinality/one}
   {:db/ident       :parameter/type
    :db/doc         "Parameter's return type."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
