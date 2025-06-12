(ns migrations.2025-06-02-fix-torching-tree-species
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; Renames Torching Tree Species
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def columns [:species :latin :common])

#_{:clj-kondo/ignore [:missing-docstring]}
(def tree-species
  (->> ["PIEN" "Picea engelmannii" "Engelmann spruce"
        "PSME" "Pseudotsuga menziesii" "Douglas-fir"
        "ABLA" "Abies lasiocarpa" "Subalpine fir"
        "TSHE" "Tsuga heterophylla" "Western hemlock"
        "PIPO" "Pinus ponderosa" "Ponderosa pine"
        "PICO" "Pinus contorta" "Lodgepole pine"
        "PIMO3" "Pinus monticola" "Western white pine"
        "ABGR" "Abies grandis" "Grand fir"
        "ABBA" "Abies balsamea" "Balsam fir"
        "PIEL" "Pinus elliottii" "Slash pine"
        "PIPA2" "Pinus palustris" "Longleaf pine"
        "PISE" "Pinus serotina" "Pond pine"
        "PIEC2" "Pinus echinata" "Shortleaf pine"
        "PITA" "Pinus taeda" "Loblolly pine"]
       (partition 3)
       (map (fn [c1 c2] (apply assoc {} (interleave c1 c2))) (repeat columns))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def existing-species 
  (d/q '[:find ?o ?o-name
         :keys id existing
         :where
         [?l :list/name "TreeSpeciesSpot"]
         [?l :list/options ?o]
         [?o :list-option/name ?o-name]]
       (d/db conn)))

(defn- new-name
  [{:keys [id existing]}]
  (let [[_ common latin]  (re-find #"(.*) \((.*)\)" existing)
        {:keys [species]} (filter #(= (:common %) common) tree-species)]
    {:species          species
     :db/id            id
     :list-option/name (format "%s / %s (%s)" latin species common)}))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload 
  (->> (map new-name existing-species)
       (sort-by :species)
       (map-indexed (fn [idx m]
                      (-> m
                          (assoc :list-option/order idx)
                          (dissoc :species))))))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn @tx-data))
