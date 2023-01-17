(ns behave.schema.cpp.namespace
  (:require [clojure.spec.alpha :as s]))

(s/def :cpp.namespace/name  string?)
(s/def :cpp.namespace/class set?)
(s/def :cpp.namespace/enum  set?)

(s/def :cpp/namespace (s/keys :req [:cpp.namespace/name]
                              :opt [:cpp.namespace/class
                                    :cpp.namespace/enum]))

(def schema
  [{:db/ident       :cpp.namespace/name
    :db/doc         "Namespace's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :cpp.namespace/class
    :db/doc         "Namespace's classes."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :cpp.namespace/enum
    :db/doc         "Namespace's enums."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])
