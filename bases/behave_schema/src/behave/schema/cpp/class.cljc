(ns behave.schema.cpp.class
  (:require [clojure.spec.alpha :as s]))

(s/def :cpp.class/name      string?)
(s/def :cpp.class/functions set?)

(s/def :cpp/class (s/keys :req [:cpp.class/name]
                          :opt [:cpp.class/functions]))

(def schema
  [{:db/ident       :cpp.class/name
    :db/doc         "Class name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :cpp.class/function
    :db/doc         "Class functions."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])
