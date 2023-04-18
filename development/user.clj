(ns user)

(comment
  (require '[behave.core :as core])
  (core/init!)

  (require '[behave-cms.server :as cms])
  (cms/init-datahike!)

  (require '[datahike.core :as dh])
  (require '[datahike.api :as d])
  (require '[datom-store.main :as store])
  (require '[datom-store.main :as store])
  (require '[behave.schema.core :refer [all-schemas]])
  (require '[clojure.set :as set])
  (require '[datom-utils.interface :refer [safe-deref]])


  (def datoms (d/datoms @@store/conn :eavt))
  (first datoms)

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
        (store/transact db new-schema))))

  (migrate store/conn all-schemas)

  ;; Extract lists from behave6.xml file

  (require
   '[clojure.java.io :as io]
   '[clojure.xml :as xml]
   '[clojure.zip :as zip])

  ;;convenience function, first seen at nakkaya.com later in clj.zip src
  (defn zip-str [s]
    (zip/xml-zip 
     (xml/parse (java.io.ByteArrayInputStream. (.getBytes s)))))

  (def results (zip-str (slurp (io/resource "public/xml/behave6.xml"))))

  ;; Get all ItemLists
  (def item-lists (filter #(= (:tag %) :itemList) (:content (first results))))

  (def item-lists-w-items
    (mapv (fn [item-list]
            {:name  (get-in item-list [:attrs :name])
             :items (mapv #(-> % (:attrs) (select-keys [:name :sort :index])) (:content (first item-lists)))})
          item-lists))

  (spit "lists.edn" (pr-str item-lists-w-items))


  )
