(ns nano-ids)

(do
  (require '[datomic.api :as d])
  (require '[datomic-store.main :as ds])
  (require '[behave-cms.server :as cms])
  (require '[behave.schema.core :refer [nano-id-schema]])

  (require '[nano-id.core :refer [nano-id]])

  ;; Get DB
  (cms/init-db!)

  ;; Transact new Nano-ID Schema
  (ds/transact ds/datomic-conn nano-id-schema)

  ;; Transact Nano-IDSs for existing with UUIDs
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def db (d/db @ds/datomic-conn))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def eids-w-uuids
    (d/q '[:find  [?e ...]
           :where [?e :bp/uuid ?uuid]] db))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def nano-id-tx (mapv (fn [eid] [:db/add eid :bp/nid (nano-id)]) eids-w-uuids))

  (ds/transact ds/datomic-conn nano-id-tx))
