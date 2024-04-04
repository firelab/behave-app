(ns user)

(comment
  (do
    (require '[behave.core :as core])
    (core/init!)
    (core/vms-sync!))

  (do
    (require '[behave-cms.server :as cms])
    (cms/init-db!))

  (require '[clj-http.client :as client])
  (require '[me.raynes.fs :as fs])
  (require '[clojure.java.io :as io])
  (require '[behave.schema.core :refer [all-schemas]])
  (require '[datomic.api :as d])
  (require '[datomic-store.main :as ds])
  (require '[datom-utils.interface :refer [split-datoms]])

  )
