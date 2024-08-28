(ns migrations.2024-08-26-fix-list-options-order
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [clojure.string :as str]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))


;; ===========================================================================================================
;; helpers
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(defn parse-fuel-model-name [n]
  (map read-string (str/split (re-find #".+(?= - )" n) #"/")))

;; ===========================================================================================================
;; Build Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def list-options
  (d/q '[:find  ?order ?name ?lo
         :in $
         :where
         [?l :list/name "SurfaceFuelModels"]
         [?l :list/options ?lo]
         [?lo :list-option/name ?name]
         [?lo :list-option/order ?order]]
       (d/db conn)))

;; handles cases when both values left and right of slashes are number (i.e. 1/1 , 2/2)
#_{:clj-kondo/ignore [:missing-docstring]}
(def first-group-sorted
  (->> list-options
       (filter (fn [[_ n]]
                 (let [[v1 v2] (parse-fuel-model-name n)]
                   (and (number? v1) (number? v2)))))
       (sort-by (fn [[_ n]]
                  (let [[v1 v2] (parse-fuel-model-name n)]
                    [v1 v2])))))

;; handles all other cases (i.e. GS1/121)
#_{:clj-kondo/ignore [:missing-docstring]}
(def second-group-sorted
  (->> list-options
       (remove (fn [[_ n]]
                 (let [[v1 v2] (parse-fuel-model-name n)]
                   (and (number? v1) (number? v2)))))
       (sort-by (fn [[_ n]]
                  (let [[v1 v2] (parse-fuel-model-name n)]
                    [v1 v2])))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def all-list-options-sorted
  (concat first-group-sorted second-group-sorted))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (map-indexed
   (fn [idx [_ _ eid]]
     {:db/id             eid
      :list-option/order idx})
   all-list-options-sorted))

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
