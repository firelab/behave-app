(ns fix-actions-conditionals)

;;; Fix Conditionals w/o UUIDs/Nano IDs

(do
  (require '[datomic.api :as d])
  (require '[datomic-store.main :as ds])
  (require '[behave-cms.server :as cms])
  (require '[nano-id.core :refer [nano-id]])

  (def ^:private rand-uuid (comp str d/squuid))

  ;; Get DB
  (cms/init-db!)

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def db (d/db @ds/datomic-conn))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def conditionals-wo-uuids-nids
    (d/q '[:find  [?e ...]
                  :where
                  [?e :conditional/values ?v]
                  [(missing? $ ?e :bp/uuid)]
                  [(missing? $ ?e :bp/nid)]] db))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def actions-wo-uuids-nids
    (d/q '[:find  [?e ...]
                  :where
                  [?e :action/type ?t]
                  [(missing? $ ?e :bp/uuid)]
                  [(missing? $ ?e :bp/nid)]] db))

  #_{:clj-kondo/ignore [:missing-docstring]}
  (def add-uuid-nid-tx
    (mapv (fn [eid] {:db/id   eid
                     :bp/uuid (rand-uuid)
                     :bp/nid  (nano-id)}) (concat conditionals-wo-uuids-nids actions-wo-uuids-nids)))

  (d/transact @ds/datomic-conn add-uuid-nid-tx))
