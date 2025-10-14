(ns migrations.205-10-10-add-english-metric-translation-keys
  (:require [schema-migrate.interface :as sm]
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

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(defn get-list-options
  "Retrieves all options for a list entity by list name.
     Returns a vector of maps with option details sorted by order."
  [conn list-name]
  (let [db      (d/db conn)
        ;; Find the list entity ID by name
        list-id (ffirst (d/q '[:find ?e
                               :in $ ?name
                               :where
                               [?e :list/name ?name]]
                             db
                             list-name))]
    (when list-id
      ;; Pull the list with all option IDs
      (let [list-data  (d/pull db '[:list/name :list/options] list-id)
            option-ids (map :db/id (:list/options list-data))
            ;; Pull all option details
            options    (d/pull-many db '[*] option-ids)]
        ;; Sort by order and return
        (sort-by :list-option/order options)))))

;; Usage:
;; (get-list-options conn "FuelMoistureToolDryBulbIndex")
;; (get-list-options conn "FuelMoistureToolElevationIndex")

(def elevation-index-keys-to-update
  {"behaveplus:list-option:fuel-moisture-tool-elevation-index:below-(1000---2000-ft)" {:english "behaveplus:list-option:fuel-moisture-tool-elevation-index:below-one-thousand-to-two-thousand-feet"
                                                                                       :metric  "behaveplus:list-option:fuel-moisture-tool-elevation-index:below-three-hundred-four-to-six-hundred-ten-meters"}
   "behaveplus:list-option:fuel-moisture-tool-elevation-index:level-(within-1000-ft)" {:english "behaveplus:list-option:fuel-moisture-tool-elevation-index:level-within-one-thousand-feet"
                                                                                       :metric  "behaveplus:list-option:fuel-moisture-tool-elevation-index:level-within-six-hundred-four-meters"}
   "behaveplus:list-option:fuel-moisture-tool-elevation-index:above-(1000---2000-ft)" {:english "behaveplus:list-option:fuel-moisture-tool-elevation-index:above-one-thousand-to-two-thousand-feet"
                                                                                       :metric  "behaveplus:list-option:fuel-moisture-tool-elevation-index:above-three-hundred-four-to-six-hundred-ten-meters"}})

(def dry-bulb-index-keys-to-update
  {"behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:10---29-of" {:english "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:ten-to-twenty-nine-degrees-farenheight"
                                                                          :metric  "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:negative-twelve-to-negative-two-degrees-celsius"}
   "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:30---49-of" {:english "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:thirty-to-fourty-nine-edegrees-farenheight"
                                                                          :metric  "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:negative-one-to-nine-degrees-celsius"}
   "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:50---69-of" {:english "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:fifty-to-sixty-nine-degrees-farenheight"
                                                                          :metric  "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:ten-to-twenty-one-degrees-celsius"}
   "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:70---89-of" {:english "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:seventy-to-eighty-nine-degrees-farenheight"
                                                                          :metric  "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:twenty-one-to-thirty-two-degrees-celsius"}
   "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:90--109-of" {:english "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:ninety-to-one-hundred-nine-degrees-farenheight"
                                                                          :metric  "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:thirty-two-to-forty-three-degrees-celsius"}
   "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:>-109-of"   {:english "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:greater-than-one-hundred-nine-degrees-farenheight"
                                                                          :metric  "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:greater-than-forty-three-degrees-celsius"}})

(defn add-translation-keys [conn translation-keys-map]
  (for [[old-key new-keys] translation-keys-map]
    (let [{:keys [english metric]} new-keys
          eid                      (d/q '[:find ?e .
                                          :in $ ?key
                                          :where
                                          [?e :list-option/translation-key ?key]]
                                        (d/db conn)
                                        old-key)]
      {:db/id                                     eid
       :list-option/english-units-translation-key english
       :list-option/metric-units-translation-key  metric})))

;; Usage:
;; (def payload (add-translation-keys conn dry-bulb-index-keys-to-update))
;; @(d/transact conn payload)

(def translation-spayload
  (sm/build-translations-payload
   conn
   {"behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:ten-to-twenty-nine-degrees-farenheight"              "10 - 29 oF"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:thirty-to-fourty-nine-edegrees-farenheight"          "30 - 49 oF"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:fifty-to-sixty-nine-degrees-farenheight"             "50 - 69 oF"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:seventy-to-eighty-nine-degrees-farenheight"          "70 - 89 oF"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:ninety-to-one-hundred-nine-degrees-farenheight"      "90 - 109 oF"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:greater-than-one-hundred-nine-degrees-farenheight"   "> 109 oF"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:negative-twelve-to-negative-two-degrees-celsius"     "-12 - -2 oC"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:negative-one-to-nine-degrees-celsius"                "-1 - 9 oC"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:ten-to-twenty-one-degrees-celsius"                   "10 - 21 oC"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:twenty-one-to-thirty-two-degrees-celsius"            "21 - 32 oC"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:thirty-two-to-forty-three-degrees-celsius"           "32 - 43 oC"
    "behaveplus:list-option:fuel-moisture-tool-dry-bulb-index:greater-than-forty-three-degrees-celsius"            "> 43 oC"
    "behaveplus:list-option:fuel-moisture-tool-elevation-index:below-one-thousand-to-two-thousand-feet"            "Below (1000 - 2000 ft)"
    "behaveplus:list-option:fuel-moisture-tool-elevation-index:level-within-one-thousand-feet"                     "Level (within 2000 ft)"
    "behaveplus:list-option:fuel-moisture-tool-elevation-index:above-one-thousand-to-two-thousand-feet"            "Above (1000 - 2000 ft)"
    "behaveplus:list-option:fuel-moisture-tool-elevation-index:below-three-hundred-four-to-six-hundred-ten-meters" "Below (304 - 610 m)"
    "behaveplus:list-option:fuel-moisture-tool-elevation-index:level-within-six-hundred-four-meters"               "Level (within 604 m)"
    "behaveplus:list-option:fuel-moisture-tool-elevation-index:above-three-hundred-four-to-six-hundred-ten-meters" "Above (304 - 610 m)"
    "behaveplus:english"                                                                                           "English"
    "behaveplus:metric"                                                                                            "Metric"
    "behaveplus:units_system"                                                                                      "Units System"}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat
              (add-translation-keys conn dry-bulb-index-keys-to-update)
              (add-translation-keys conn elevation-index-keys-to-update)
              translation-spayload))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
