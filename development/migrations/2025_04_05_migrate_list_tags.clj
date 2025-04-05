(ns migrations.2025-04-05-migrate-list-tags
  (:require [schema-migrate.interface :as sm]
            [string-utils.interface :refer [->str ->kebab]]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; Converts all existing :list/tags / :color-tags to :tag-sets
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def db (d/db conn))

(defn- ->tag-set [tag-set-name tag-list]
  (let [translation-key (str "behaveplus:tags:" (->kebab tag-set-name))]
    (sm/->entity
     {:tag-set/name            tag-set-name
      :tag-set/translation-key translation-key
      :tag-set/tags
      (map (fn [[tag-name order]]
             (let [tag-t-key (format "%s:%s" translation-key (->kebab tag-name))]
               (sm/->entity
                {:tag/name            tag-name
                 :tag/order           order
                 :tag/translation-key tag-t-key})))
           tag-list)})))

(defn- ->color-tag-set [tag-set-name tag-color-list]
  (let [translation-key (str "behaveplus:tags:" (->kebab tag-set-name))]
    (sm/->entity
     {:tag-set/name            tag-set-name
      :tag-set/color?          true
      :tag-set/translation-key translation-key
      :tag-set/tags
      (map (fn [[tag-name order color]]
             (let [tag-t-key (format "%s:%s" translation-key (->kebab tag-name))]
               (sm/->entity
                {:tag/name            tag-name
                 :tag/order           order
                 :tag/color           color
                 :tag/translation-key tag-t-key})))
           tag-color-list)})))

(defn- ->tag-translations [tag-set-name tag-list]
  (let [translation-key (str "behaveplus:tags:" (->kebab tag-set-name))
        translations    {translation-key tag-set-name}]
    (reduce
     (fn [m [tag-name]]
       (let [tag-t-key (format "%s:%s" translation-key (->kebab tag-name))]
         (assoc m tag-t-key tag-name)))
     translations
     tag-list)))

(defn- convert-existing-tags [conn list-name]
  (->> (sm/name->nid conn :list/name list-name)
       (conj [:bp/nid])
       (d/pull (d/db conn) '[* {:list/options [*]}])
       (:list/options)
       (mapcat :list-option/tags)
       (set)
       (map ->str)
       (sort)
       (map-indexed #(conj [] %2 %1))))

;;; Region Codes
#_{:clj-kondo/ignore [:missing-docstring]}
(def region-codes
  ["Region Codes"
   (convert-existing-tags conn "MortalitySpeciesMasterList")])

#_{:clj-kondo/ignore [:missing-docstring]}
(def region-codes-tag-set (apply ->tag-set region-codes))
#_{:clj-kondo/ignore [:missing-docstring]}
(def region-codes-translations (apply ->tag-translations region-codes))

;;; Fuel Types
#_{:clj-kondo/ignore [:missing-docstring]}
(def fuel-types ["Fuel Types" [["Grass" 0]
                               ["Grass Shrub" 1]
                               ["Shrub" 2]
                               ["Timber Understory" 3]
                               ["Timber Litter" 4]
                               ["Slash Blowdown" 5]]])

#_{:clj-kondo/ignore [:missing-docstring]}
(def fuel-types-tag-set (apply ->tag-set fuel-types))
#_{:clj-kondo/ignore [:missing-docstring]}
(def fuel-types-translations (apply ->tag-translations fuel-types))

;;; Fuel Region Categories
#_{:clj-kondo/ignore [:missing-docstring]}
(def fuel-region-categories
  ["Fuel Region Categories" [["Standard"                         0 "#FFE93C"]
                             ["Mediterranean"                    1 "#94A839"]
                             ["Chapparal and Coastal Sage Shrub" 2 "#E06E59"]]])

#_{:clj-kondo/ignore [:missing-docstring]}
(def fuel-region-categories-tag-set (apply ->color-tag-set fuel-region-categories))
#_{:clj-kondo/ignore [:missing-docstring]}
(def fuel-region-categories-translations (apply ->tag-translations fuel-region-categories))

;;; Tag Sets
#_{:clj-kondo/ignore [:missing-docstring]}
(def tag-sets-payload
  [region-codes-tag-set
   fuel-types-tag-set
   fuel-region-categories-tag-set])

;;; Translations
#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/build-translations-payload
   conn
   100
   (merge
    region-codes-translations
    fuel-types-translations
    fuel-region-categories-translations)))

;;; Migrate Lists
(defn- ->tag-map [tag-set & [tag-name-xform]]
  (let [tag-name-xform (or tag-name-xform ->kebab)]
    (->> tag-set
         (:tag-set/tags)
         (map (fn [t] [(-> t (:tag/name) tag-name-xform (keyword)) [:bp/nid (:bp/nid t)]]))
         (into {}))))

(def ^:private region-codes-map           (->tag-map region-codes-tag-set identity))
(def ^:private fuel-types-map             (->tag-map fuel-types-tag-set   identity))
(def ^:private fuel-region-categories-map (->tag-map fuel-region-categories-tag-set))

(defn- pull-existing-list [list-name]
  (d/entity db [:bp/nid (sm/name->nid conn :list/name list-name)]))

(defn- add-tag-sets [db list-name filter-tag-set & [color-tag-set]]
  (cond-> {:bp/nid        (sm/name->nid db :list/name list-name)
           :list/tag-set [:bp/nid (:bp/nid filter-tag-set)]}
    color-tag-set
    (merge {:list/color-tag-set [:bp/nid (:bp/nid color-tag-set)]})))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn migrate-existing-list-tags [list-name filter-tags-map & [color-tags-map]]
  (let [existing-list    (pull-existing-list list-name)
        existing-options (:list/options existing-list)]

    (->> existing-options
         (mapv
          (fn [option]
            (cond-> option
              :always
              (select-keys [:db/id :bp/nid])

              (and filter-tags-map (:list-option/tags option))
              (assoc :list-option/tag-refs (map filter-tags-map (:list-option/tags option)))

              (and color-tags-map (:list-option/color-tag option))
              (assoc :list-option/color-tag-ref (color-tags-map (-> option :list-option/color-tag :color-tag/id))))))
         ;; Remove all 'nil' lists
         (remove #(-> (:list-option/tag-refs %) (set) (= #{nil}))))))

(def ^:private migrate-lists-payload 
  (concat 
   [(add-tag-sets conn "SurfaceFuelModels" fuel-types-tag-set fuel-region-categories-tag-set)
    (add-tag-sets conn "WindDrivenSurfaceFuelModelCodes" fuel-types-tag-set fuel-region-categories-tag-set)
    (add-tag-sets conn "MortalitySpeciesMasterList" region-codes-tag-set)]
   (migrate-existing-list-tags "SurfaceFuelModels" fuel-types-map fuel-region-categories-map)
   (migrate-existing-list-tags "WindDrivenSurfaceFuelModelCodes" fuel-types-map fuel-region-categories-map)
   (migrate-existing-list-tags "MortalitySpeciesMasterList" region-codes-map)))


;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat tag-sets-payload translations-payload migrate-lists-payload))

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
