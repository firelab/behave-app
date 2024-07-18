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

  (def scorch-height-input-group
    (sm/t-key->eid db scorch-height-t-key))

  (def new-subgroups
    ["Wind Speed" "Wind Measured at" "Wind Adjustment Factor"])

  (def new-subgroups-tx
    (map #(-> (sm/->subgroup scorch-height-input-group
                             %
                             (str scorch-height-t-key ":" (->snake %)))
              (merge {:group/conditionals-operator :or
                      :group/conditionals
                      [(sm/->module-conditional :equals ["surface"])]}))
         new-subgroups))

  (def tx-1 (d/transact @ds/datomic-conn new-subgroups-tx))

  #_(sm/rollback-tx! @ds/datomic-conn tx-1)

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
    (map
     (fn [{:keys [v-name class-name fn-name p-name]}]
       (let [subgroup-t-key (str scorch-height-t-key ":" (->snake v-name))
             subgroup-eid   (sm/t-key->eid db subgroup-t-key)]
         (-> (sm/->group-variable subgroup-eid
                                  (sm/name->eid conn :variable/name v-name)
                                  (str subgroup-t-key ":" (->snake v-name)))
             (merge {:group-variable/cpp-namespace (sm/cpp-ns->uuid conn "global")
                     :group-variable/cpp-class     (sm/cpp-class->uuid conn class-name)
                     :group-variable/cpp-function  (sm/cpp-fn->uuid conn fn-name)
                     :group-variable/cpp-parameter (sm/cpp-param->uuid conn fn-name p-name)})))) new-group-vars))

  (def tx-2 (d/transact @ds/datomic-conn new-group-vars-tx))

  #_(sm/rollback-tx! @ds/datomic-conn tx-2)

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
          (map :bp/uuid new-group-vars-tx))
         (partition 2)
         (map vec)))

  (def new-links-tx
    (map
     (fn [[source destination-uuid]]
       (sm/->link source [:bp/uuid destination-uuid]))
     new-links))

  (def tx-3 (d/transact @ds/datomic-conn new-links-tx))

  #_(sm/rollback-tx! @ds/datomic-conn tx-3)

  )
