(ns behave.schema.cpp.function
  (:require [clojure.spec.alpha :as s]))

(s/def :function/name        string?)
(s/def :function/return-type string?)
(s/def :function/parameters  set?)

(s/def :cpp/function (s/keys :req [:function/name
                                   :function/return-type]
                             :opt [:function/parameters]))

(def schema
  [{:db/ident       :function/name
    :db/doc         "Function's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :function/return-type
    :db/doc         "Function's return type."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :function/parameters
    :db/doc         "Function's parameters."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])
