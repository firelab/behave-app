(ns behave.schema.cpp.class
  (:require [clojure.spec.alpha :as s]))

(s/def :class/name      string?)
(s/def :class/functions set?)

(s/def :cpp/class (s/keys :req [:class/name]
                          :opt [:class/functions]))

(def schema
  [{:db/ident       :class/name
    :db/doc         "Classes's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :class/functions
    :db/doc         "Classes's enums."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])
