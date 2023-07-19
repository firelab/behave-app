(ns behave.schema.simple
  (:require [clojure.spec.alpha :as s]))

(s/def :entity/id uuid?)

(def schema
  [{:db/ident       :entity/id
    :db/doc         "Entity's ID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :entity/name
    :db/doc         "Entity's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
