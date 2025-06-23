(ns migrations.2025-06-23-adjust-wind-speed-inputs
  (:require [schema-migrate.interface :as sm]
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
(def wind-speed-submodule-eid (sm/t-key->eid conn "behaveplus:surface:input:wind_speed"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-speed-group (sm/t-key->entity conn "behaveplus:surface:input:wind_speed:wind_speed"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-speed-gv (first (:group/group-variables wind-speed-group)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-speed-conditional (first (:group/conditionals wind-speed-group)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def ten-m-wind-speed-variable-eid
  (sm/bp6-code->variable-eid conn "vWindSpeedAt10MUpslope"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def twenty-ft-wind-speed-variable-eid
  (sm/bp6-code->variable-eid conn "vWindSpeedAt20FtUpslope"))

#_{:clj-kondo/ignore [:missing-docstring]}
(def ten-m-wind-speed "10-Meter Wind Speed")

#_{:clj-kondo/ignore [:missing-docstring]}
(def twenty-ft-wind-speed "20-Foot Wind Speed")

#_{:clj-kondo/ignore [:missing-docstring]}
(def wind-speed-source-links
  (d/q '[:find [?d-gv ...]
         :in $ ?gv
         :where
         [?l :link/source ?gv]
         [?l :link/destination ?d-gv]]
       (d/db conn) (:db/id wind-speed-gv)))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

;;; 1. Rename existing 10M Wind Speed variable as '10-Meter Wind Speed'

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-variable-name-payload [{:db/id         ten-m-wind-speed-variable-eid
                                    :variable/name ten-m-wind-speed}])

;;; 2. Duplicate existing Wind Speed as '10-Meter Wind Speed'

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-10-m-wind-speed-group
  (let [t-key (str "behaveplus:surface:input:wind_speed:" (->snake ten-m-wind-speed))]
    {:db/id                 -1
     :submodule/_groups     wind-speed-submodule-eid
     :group/name            ten-m-wind-speed
     :group/translation-key t-key
     :group/help-key        (str t-key ":help")}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-10-m-wind-speed-gv
  (let [t-key (str (:group/translation-key new-10-m-wind-speed-group) ":" (->snake ten-m-wind-speed))]
    (merge {:db/id -2
            :group/_group-variables         -1
            :group-variable/translation-key t-key
            :group-variable/help-key        (str t-key ":help")}
           (select-keys wind-speed-gv [:group-variable/cpp-namespace
                                       :group-variable/cpp-class
                                       :group-variable/cpp-function
                                       :group-variable/cpp-parameter]))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-10-m-wind-speed-variable->group-variable-payload
  [{:db/id                    ten-m-wind-speed-variable-eid
    :variable/group-variables -2}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-10-m-duplicate-links-payload
  (->> wind-speed-source-links
       (map (fn [dest-gv]
              {:link/source      (:db/id new-10-m-wind-speed-gv)
               :link/destination dest-gv})) 
       (sm/postwalk-insert)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def new-10-m-wind-speed-payload
  (sm/postwalk-insert [new-10-m-wind-speed-group new-10-m-wind-speed-gv]))
  
#_{:clj-kondo/ignore [:missing-docstring]}
(def new-10-m-translations-payload
  (sm/build-translations-payload
   conn
   100
   {(:group/translation-key new-10-m-wind-speed-group) ten-m-wind-speed
    (:group-variable/translation-key new-10-m-wind-speed-gv) ten-m-wind-speed}))

;;; 3. Rename existing Wind Speed as '20-Foot Wind Speed'

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-existing-to-20-ft-wind-speed-payload
  [{:db/id      (:db/id wind-speed-group)
    :group/name twenty-ft-wind-speed}
   {:db/id         twenty-ft-wind-speed-variable-eid
    :variable/name twenty-ft-wind-speed}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-existing-translations-payload
  (sm/update-translations-payload
   conn
   "en-US"
   {(:group/translation-key wind-speed-group)       twenty-ft-wind-speed
    (:group-variable/translation-key wind-speed-gv) twenty-ft-wind-speed}))

;;; 4. Update Conditionals

#_{:clj-kondo/ignore [:missing-docstring]}
(def duplicate-conditional-10-m-payload
  [(-> {:group/_conditionals -1}
       (merge wind-speed-conditional)
       (dissoc :bp/nid :bp/uuid)
       (merge {:conditional/operator :equal :conditional/values "2"})
       (sm/postwalk-insert))])

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-conditional-20-ft-payload
  (let [eid (:db/id wind-speed-conditional)]
    [[:db/retract eid :conditional/values "2"]
     [:db/add eid :conditional/operator :equal]]))

;;; 5. Final Payload

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat []
                     update-variable-name-payload
                     new-10-m-wind-speed-payload
                     new-10-m-wind-speed-variable->group-variable-payload
                     new-10-m-duplicate-links-payload
                     new-10-m-translations-payload
                     update-existing-to-20-ft-wind-speed-payload
                     update-existing-translations-payload
                     duplicate-conditional-10-m-payload
                     update-conditional-20-ft-payload))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
