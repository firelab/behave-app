(ns migrations.2025-02-20-add-gacc-tags-to-mortality-tree-species
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================


(defn- csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map keyword)
            repeat)
       (rest csv-data)))

(def species-master-table
  (with-open [reader (io/reader "projects/behave_cms/resources/public/csv/mortality_tree_species_master_table_gacc.csv")]
    (csv-data->maps (doall (csv/read-csv reader)))))

(set (map :Species species-master-table))

(def list-options
  (sort-by :list-option/order
           (:list/options (d/entity (d/db conn) (sm/name->eid conn :list/name "MortalitySpeciesMasterList")))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (let [index         (atom -1)
        species-codes (set (map :Species species-master-table))]
    (map (fn [list-option]
           (let [species-code (:list-option/value list-option)]
             (if (contains? species-codes species-code)
               (do
                 (swap! index inc)
                 (let [{:keys [AICC EACC GBCC NRCC NWCC RMCC SACC SWCC] :as entry}
                       (->> species-master-table
                            (filter #(= species-code (:Species %)))
                            (first))
                       ONCC (get entry (keyword (str "ONCC & OSCC")))]
                   {:db/id             ( :db/id list-option)
                    :list-option/order @index
                    :list-option/tags  (cond-> []
                                         (= AICC "1") (conj :Alaska)
                                         (= ONCC "2") (conj :California)
                                         (= EACC "3") (conj :Eastern-Area)
                                         (= GBCC "4") (conj :Great-Basin)
                                         (= NRCC "5") (conj :Northern-Rockies)
                                         (= NWCC "6") (conj :Northwest)
                                         (= RMCC "7") (conj :Rockey-Mountain)
                                         (= SACC "8") (conj :Southern-Area)
                                         (= SWCC "9") (conj :Southwest))}))
               [:db/retractEntity (:db/id list-option)])))
         list-options)))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data (d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
