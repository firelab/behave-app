(ns migrations.2024-07-18-mortality-wind-speed)

#_{:clj-kondo/ignore [:missing-docstring]}
(do
  (require '[behave-cms.server        :as cms]
           '[datomic.api              :as d]
           '[datomic-store.main       :as ds]
           '[schema-migrate.interface :as sm]
           '[string-utils.interface   :refer [->snake]]
           '[cms-import               :refer [add-export-file-to-conn]])

  (cms/init-db!)

  (def conn @ds/datomic-conn)
  (def db (d/db @ds/datomic-conn))

  ;; Import new SIGSpot::setActiveCrownFlameLength Setter

  (add-export-file-to-conn "cms-exports/SIGMortality.edn" conn)

  ;; Add new Hidden Subgroups

  (def scorch-height-t-key "behaveplus:mortality:compute-scorch-height")

  (def scorch-height-input-submodule
    (sm/t-key->eid db scorch-height-t-key))

  (def new-groups
    ["Wind Speed" "Wind Measured at" "Wind Adjustment Factor"])

  (def new-groups-tx
    (map-indexed (fn [idx group-name]
                   (-> (sm/->group scorch-height-input-submodule
                                   group-name
                                   (str scorch-height-t-key ":" (->snake group-name)))
                       (merge {:db/id                       (* -1 (inc idx))
                               :group/conditionals-operator :or
                               :group/conditionals
                               [(sm/->module-conditional :equals "surface")]})))
         new-groups))

  ;; Add new Group Variables

  (def new-group-vars
    [{:v-name     "Wind Speed"
      :class-name "SIGMortality"
      :fn-name    "setWindSpeed"
      :p-name     "windSpeed"}
     {:v-name     "Wind Measured at"
      :class-name "SIGMortality"
      :fn-name    "setWindHeightInputMode"
      :p-name     "windHeightInputMode"}
     {:v-name     "Wind Adjustment Factor"
      :class-name "SIGMortality"
      :fn-name    "setUserProvidedWindAdjustmentFactor"
      :p-name     "userProvidedWindAdjustmentFactor"}])

  (def new-group-vars-tx
    (map-indexed
     (fn [idx {:keys [v-name class-name fn-name p-name] :as group-variable}]
       (let [temp-id     (* -1 (+ 10 idx))
             group-eid   (:db/id group-variable)
             group-t-key (:group/translation-key group-variable)]
         (-> (sm/->group-variable group-eid
                                  (sm/name->eid conn :variable/name v-name)
                                  (str group-t-key ":" (->snake v-name)))
             (merge {:db/id                        temp-id
                     :group-variable/cpp-namespace (sm/cpp-ns->uuid conn "global")
                     :group-variable/cpp-class     (sm/cpp-class->uuid conn class-name)
                     :group-variable/cpp-function  (sm/cpp-fn->uuid conn class-name fn-name)
                     :group-variable/cpp-parameter (sm/cpp-param->uuid conn class-name fn-name p-name)}))))
     (map merge new-group-vars new-groups-tx)))

  ;; Link Variables from Surface Wind Inputs to new Inputs

  (def surface-wind-speed-variable
    (sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind_speed:wind_speed"))

  (def surface-wind-height-variable
    (sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind_height:wind_height"))

  (def surface-waf-variable
    (sm/t-key->eid db "behaveplus:surface:input:wind_speed:wind-adjustment-factor:wind-adjustment-factor---user-input:wind-adjustment-factor"))

  (def new-links
    (->> (interleave
          [surface-wind-speed-variable
           surface-wind-height-variable
           surface-waf-variable]
          (map :db/id new-group-vars-tx))
         (partition 2)
         (map vec)))

  (def new-links-tx
    (map
     (fn [[source destination-uuid]]
       (sm/->link source destination-uuid))
     new-links))

  (comment
    (def tx (d/transact @ds/datomic-conn (concat new-groups-tx new-group-vars-tx new-links-tx)))
    )

  (comment
    (sm/rollback-tx! @ds/datomic-conn tx)
    )

  )
