(ns behave-cms.store
  (:require [behave.schema.core :refer [all-schemas]]
            [datomic-store.main :as s]
            [config.interface   :refer [get-config]]
            [datomic.api :as d]))

(defn connect! [db-config & [reset?]]
  (if reset?
    (s/reset-db! db-config all-schemas)
    (s/default-conn db-config all-schemas #(s/migrate! % all-schemas))))

(defn default-conn []
  (if (nil? @s/datomic-conn)
    (connect! (get-config :database))
    @s/datomic-conn))

(defn attribute->value-type [attr]
  (d/q '[:find ?value-type .
         :in $ ?attr
         :where
         [?e :db/ident ?attr]
         [?e :db/valueType ?v]
         [?v :db/ident ?value-type]]
       (d/db @s/datomic-conn)
       attr))
