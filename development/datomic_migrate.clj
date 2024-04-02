(ns datomic-migrate
  (:require 
   [behave.schema.core  :refer [all-schemas]]
   [clojure.string      :as str]
   [clojure.set         :as set]
   [datahike-store.main :as dh]
   [datomic-store.main  :as dm]
   [datomic.api         :as d]
   [me.raynes.fs        :as fs]))

;;; Helpers

(defn datom->tx-vec
  "Turns a e-a-v datom into a transaction, makes the entity
   id and any reference values negative."
  [ref-attrs [e a v]]
  (let [e (* -1 e)]
    (cond
      (ref-attrs a)
      [:db/add e a (* -1 v)]

      :else
      [:db/add e a v])))

;;; Migration

(defn migrate-dh-to-datomic
  "Migrates datoms from DataHike to Datomic."
  [dh-conn datomic-conn schema]
  (let [dh-datoms     (dh/export-datoms @dh-conn true)
        db-attrs      (as-> dh-datoms $
                        (map second $)
                        (filter #(-> % (str) (str/starts-with? ":db")) $)
                        (set $))
        ref-attrs     (set (map :db/ident (filter #(= :db.type/ref (:db/valueType %)) schema)))
        all-ids       (set (map first dh-datoms))

        old-attrs
        (set/difference 
         (set/difference (set (map second dh-datoms)) db-attrs)
         (set (map :db/ident schema)))

        tx
        (->> dh-datoms
             (remove (fn [[_ a v]]
                       (or
                        ;; Remove schema attributes
                        (db-attrs a)

                        ;; Remove unused attributes
                        (old-attrs a)
                        
                        ;; Remove references to entities that no longer exist
                        (and (ref-attrs a) (not (all-ids v))))))
             (mapv (partial datom->tx-vec ref-attrs)))]

        (println "Transacting" (count tx) "datoms (Filtered " (- (count dh-datoms) (count tx)) " schema/unused datoms)")
        (d/transact datomic-conn tx)

        (println "Migration completed!")))

(comment
  (def datahike-config {:store {:backend :file :path (str (fs/expand-home "~/.behave_cms/db"))}})
  (def dh-conn (dh/default-conn datahike-config all-schemas))
  (def datomic-conn (dm/default-conn {:project "behave"} all-schemas))

  ;; Perform Migration
  (migrate-dh-to-datomic dh-conn datomic-conn all-schemas)
  )
