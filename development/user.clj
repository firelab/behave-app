(ns user)

(comment
  (require '[behave.core :as core])

  (core/init!)
  (core/vms-sync!)

  (core/vms-sync!)

  (require '[behave-cms.server :as cms])
  (cms/init-datahike!)

  (require '[clj-http.client :as client])
  (client/get "https://behave.sig-gis.com/sync" {:as :byte-array})

  (require '[datahike.migrate :refer [export-db import-db]])
  (require '[me.raynes.fs :as fs])
  (require '[clojure.java.io :as io])
  (require '[behave.schema.core :refer [all-schemas]])
  (require '[datahike.api :as d])
  (require '[datom-store.main :as ds])
  (require '[datom-utils.interface :refer [split-datoms
                                           safe-attr?
                                           safe-deref]])

  (def lists (clojure.edn/read-string (slurp "lists.edn")))
  (d/q '[:find ?e
         :in $ ?uuid
         :where [?e :bp/uuid ?uuid]] @@ds/conn (:bp/uuid (first lists)))


  (ds/export-datoms @@ds/conn)

  (def attrs {:application/module      :application/modules
              :module/submodule        :module/submodules
              :submodules/group        :submodules/groups
              :group/group-variable    :group/group-variables
              :variable/group-variable :variable/group-variables})

  (def single-attrs (set (keys attrs)))
  single-attrs

  (defn xform-attrs [[e a v]]
    (cond
      (single-attrs a)
      [e (get attrs a) v]

      (single-attrs v)
      [e a (get attrs v)]

      :else
      [e a v]))


  (def tuple-attrs (set (map :db/ident (filter #(= :db.type/tuple (:db/valueType %)) all-schemas))))

  (defn tuple-attr? [[_ a _]]
    (tuple-attrs a))

  (def datoms (->> (d/datoms @@ds/conn :eavt)
                   (split-datoms)
                   (remove tuple-attr?)
                   (map xform-attrs)))

  ((set (map second datoms)) :application/version)
  (sort (into '() (set (map second datoms))))
  (sort (into '() (set (filter keyword? (map #(nth % 2) datoms)))))

  (def datoms-to-add (mapv #(vec (apply list :db/add (take 3 %))) datoms))

  (def config {:store {:backend :file :path "~/.behave_cms/db-new-schema"}})
  (d/create-database (update-in config [:store :path] #(-> % fs/expand-home (.getPath))))
  (def conn (d/connect (update-in config [:store :path] #(-> % fs/expand-home (.getPath)))))
  conn

  (:max-tx @conn)
  (d/datoms @conn :eavt)

  (d/transact conn datoms-to-add)

  (export-db @ds/conn "behave-dump")
  (export-db conn "behave-dump-05-17")

  (d/q [:find ?app
        :in ?module
        :where [?module :application/_module ?app]])

  (d/pull '[{:application/_module [*]}] module-id)

  ;; Before tx

  (d/q '[:find ?e ?a ?v
         :where [?e :db/ident :application/module]
                [?e ?a ?v]]
       @@ds/conn)

  (d/q '[:find ?e ?a ?v
         :where [?e :application/module ?v]
                [?e ?a ?v]]
       @@ds/conn)

  ;; Transact modifying attribute

  (d/q '[:find ?e ?a ?v
         :where [?e :application/modules ?v]
                [?e ?a ?v]]
       @conn)

  ;; Resolve issues with Help content

  (d/q '[:find ?k ?h ?c ?g-name
         :where
         [249 :group/help-key ?k]
         [249 :group/name ?g-name]
         [?h :help-page/key ?k]
         [?h :help-page/content ?c]]
       @@ds/conn)

  (d/transact (unwrap ds/conn) [{:db/id 730 :help-page/content "#### Resources\n\n"}])

  )
