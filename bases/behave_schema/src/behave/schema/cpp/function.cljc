(ns behave.schema.cpp.function
  (:require [clojure.spec.alpha :as s]))

(s/def :cpp.function/name        string?)
(s/def :cpp.function/return-type string?)
(s/def :cpp.function/parameters  set?)

(s/def :cpp/function (s/keys :req [:cpp.function/name
                                   :cpp.function/return-type]
                             :opt [:cpp.function/parameters]))

(def schema
  [{:db/ident       :cpp.function/name
    :db/doc         "Function's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :cpp.function/return-type
    :db/doc         "Function's return type."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :cpp.function/parameter
    :db/doc         "Function's parameters."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}])
