(ns behave.schema.cpp.namespace
  (:require [clojure.spec.alpha :as s]))

(s/def :namespace/name    string?)
(s/def :namespace/classes set?)
(s/def :namespace/enums   set?)

(s/def :cpp/namespace (s/keys :req [:namespace/name]
                              :opt [:namespace/classes
                                    :namespace/enums]))

(def schema
  [{:db/ident       :namespace/name
    :db/doc         "Namespace's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :namespace/classes
    :db/doc         "Namespace's classes."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :namespace/enums
    :db/doc         "Namespace's enums."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])
