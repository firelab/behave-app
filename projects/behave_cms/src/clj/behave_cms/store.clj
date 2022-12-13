(ns behave-cms.store
  (:require [behave.schema.core    :refer [all-schemas]]
            [datahike.api          :as d]
            [datom-store.main      :as s]
            [config.interface      :refer [get-config]]
            [datom-utils.interface :refer [safe-deref unwrap]]))

(defn connect! [config]
  (s/default-conn all-schemas config))

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
