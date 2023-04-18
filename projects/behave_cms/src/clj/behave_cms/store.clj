(ns behave-cms.store
  (:require [behave.schema.core    :refer [all-schemas]]
            [datahike.api          :as d]
            [datom-store.main      :as s]
            [config.interface      :refer [get-config]]
            [datom-utils.interface :refer [safe-deref unwrap]]))

(defn get-existing-schema [db]
  (set (d/q '[:find [?ident ...] :where [?e :db/ident ?ident]] (safe-deref db))))

(defn migrate [db schema]
  (let [existing-schema (get-existing-schema db)
        new-schema      (as-> (map :db/ident all-schemas) $
                          (set $)
                          (apply disj $ existing-schema)
                          (filter #($ (:db/ident %)) all-schemas))]
    (when (seq new-schema)
      (print "Migrating DB with new schema: " new-schema)
      (s/transact db new-schema))))

(defn connect! [config & [reset?]]
  (if reset?
    (s/reset-datahike! config all-schemas)
    (s/default-conn all-schemas config #(migrate % all-schemas))))

(defn default-conn []
  (if (nil? @s/conn)
    (connect! (get-config :database :config))
    @s/conn))

(defn get-entity [db {id :db/id}]
  (d/pull (safe-deref db) '[*] id))

(defn create-entity! [db data]
  (let [db (unwrap db)]
    (s/transact db [(assoc data :db/id -1)])))

(defn update-entity! [db data]
  (let [db (unwrap db)]
    (s/transact db [data])))

(defn delete-entity! [db {id :db/id}]
  (let [db (unwrap db)]
    (s/transact db [[:db/retractEntity id]])))
