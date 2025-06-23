(ns migrations.2025-06-23-adjust-wind-speed-inputs
  (:require [clojure.string :as str]
            [schema-migrate.interface :as sm]
            [schema-migrate.core :as smc]
            [string-utils.interface :refer [->snake]]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-speed-group (sm/t-key->entity conn "behaveplus:surface:input:wind_speed:wind_speed"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-speed-gv (first (:group/group-variables wind-speed-group)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def ten-m-wind-speed-variable-eid
  (d/entity (d/db conn) 
            (sm/bp6-code->variable-eid conn "vWindSpeedAt10MUpslope")))

(:variable/group-variables ten-m-wind-speed-variable-eid)

#_{:clj-kondo/ignore [:missing-docstring]}
(def twenty-ft-wind-speed-variable-eid
  (d/entity (d/db conn) 
            (sm/bp6-code->variable-eid conn "vWindSpeedAt20FtUpslope")))

(:variable/group-variables twenty-ft-wind-speed-variable-eid)

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;; Duplicate Wind Speed as '10-Meter Wind Speed'

(def ten-m-wind-speed "10-Meter Wind Speed")

(def update-variable-name-payload {:db/id ten-m-wind-speed-variable-eid
                                   :variable/name ten-m-wind-speed})

(def new-10-m-wind-speed-gv
  (-> (merge {} wind-speed-gv)
      (dissoc :db/id)
      (assoc :group-variable/name ten-m-wind-speed)
      (update :group/translation-key (fn [k]
                                       (let [base-t-key (vec (butlast (str/split k #":")))]
                                         (str/join ":" (conj base-t-key (->snake ten-m-wind-speed))))))))


(def new-10-m-wind-speed-group
  (-> (merge {} wind-speed-group)
      (dissoc :db/id)
      (assoc :group/name ten-m-wind-speed)
      (update :group/translation-key (fn [k]
                                       (let [base-t-key (vec (butlast (str/split k #":")))]
                                         (str/join ":" (conj base-t-key (->snake ten-m-wind-speed))))))))



(keys wind-speed-group)

wind

MqRWgOmaK_EGhBe9RWZ7t
