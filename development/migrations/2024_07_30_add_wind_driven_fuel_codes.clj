(ns migrations.2024-07-30-add-wind-driven-fuel-codes
  (:require
   [schema-migrate.interface :as sm]
   [datomic.api :as d]
   [behave-cms.store :refer [default-conn]]
   [behave-cms.server :as cms]
   [nano-id.core :refer [nano-id]]))

#_{:clj-kondo/ignore [:missing-docstring]}
(do 

  (cms/init-db!)

  (def conn (default-conn))

  (def db (d/db conn))

  (def wind-driven-fuel-code-list-tx
    {:db/id     -1
     :bp/nid    (nano-id)
     :bp/uuid   "6629527e-75c8-4707-a589-d2dbce3f966f"
     :list/name "WindDrivenSurfaceFuelModelCodes"
     :list/options
     [{:bp/uuid                            "662bc941-ced5-478a-96fe-ee4db59d7359"
       :bp/nid                             (nano-id)
       :list-option/name                   "1/1 - Short grass (Static)"
       :list-option/order                  0
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:fm1---short-grass-(s)"	
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:fm1---short-grass-(s)"
       :list-option/value                  "1"}

      {:bp/uuid                            "662bc95f-370c-4697-a31f-d3c90b117f99"
       :bp/nid                             (nano-id)
       :list-option/name                   "3/3 - Tall grass (Static)"
       :list-option/order                  1
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:fm3---tall-grass-(s)"	
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:fm3---tall-grass-(s)"
       :list-option/value                  "3"}

      {:bp/uuid                            "66295268-16d9-4778-bed0-47ee8a719aae"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR1/101 - Short sparse, dry climate grass (Dynamic)"
       :list-option/order                  2
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr1---short,-sparse,-dry-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr1---short,-sparse,-dry-climate-grass-(d)"
       :list-option/value                  "101"}

      {:bp/uuid                            "66295268-31e6-4a94-b0ff-c99af0e96038"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR2/102 - Low load dry climate grass (Dynamic)"
       :list-option/order                  3
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr2---low-load,-dry-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr2---low-load,-dry-climate-grass-(d)"
       :list-option/value                  "102"}

      {:bp/uuid                            "66295268-eb0b-4f66-a15a-fa1db3a97b0f"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR3/103 - Low load very coarse, humid climate grass (Dynamic)"
       :list-option/order                  4
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr3---low-load,-very-coarse,-humid-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr3---low-load,-very-coarse,-humid-climate-grass-(d)"
       :list-option/value                  "103"}

      {:bp/uuid                            "66295268-90e3-4e27-909d-97497bfd3196"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR4/104 - Moderate load dry climate grass (Dynamic)"
       :list-option/order                  5
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr4---moderate-load,-dry-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr4---moderate-load,-dry-climate-grass-(d)"
       :list-option/value                  "104"}

      {:bp/uuid                            "66295268-a32f-4d48-831e-be5931e1e84f"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR5/105 - Low load humid climate grass (Dynamic)"
       :list-option/order                  6
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr5---low-load,-humid-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr5---low-load,-humid-climate-grass-(d)"
       :list-option/value                  "105"}

      {:bp/uuid                            "66295268-8c2c-4ab1-b2b4-ee1c1339ce28"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR6/106 - Moderate load humid climate grass (Dynamic)"
       :list-option/order                  7
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr6---moderate-load,-humid-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr6---moderate-load,-humid-climate-grass-(d)"
       :list-option/value                  "106"}

      {:bp/uuid                            "66295268-325d-434a-8bdd-7402fe8053ac"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR7/107 - High load dry climate grass (Dynamic)"
       :list-option/order                  8
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr7---high-load,-dry-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr7---high-load,-dry-climate-grass-(d)"
       :list-option/value                  "107"}

      {:bp/uuid                            "66295268-6112-494e-a123-36d466cddbd3"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR8/108 - High load very coarse, humid climate grass (Dynamic)"
       :list-option/order                  9
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr8---high-load,-very-coarse,-humid-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr8---high-load,-very-coarse,-humid-climate-grass-(d)"
       :list-option/value                  "108"}

      {:bp/uuid                            "66295268-1eb8-4cca-9bac-858fb0a7c764"
       :bp/nid                             (nano-id)
       :list-option/name                   "GR9/109 - Very high load humid climate grass (Dynamic)"
       :list-option/order                  11
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gr9---very-high-load,-humid-climate-grass-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gr9---very-high-load,-humid-climate-grass-(d)"
       :list-option/value                  "109"}

      {:bp/uuid                            "66295268-8f94-4b95-8b4f-de6eed836066"
       :bp/nid                             (nano-id)
       :list-option/name                   "GS1/121 - Low load dry climate grass-shrub (Dynamic)"
       :list-option/order                  11
       :list-option/result-translation-key "behaveplus:list-option:result:surface-fuel-models:gs1---low-load,-dry-climate-grass-shrub-(d)"
       :list-option/translation-key        "behaveplus:list-option:surface-fuel-models:gs1---low-load,-dry-climate-grass-shrub-(d)"
       :list-option/value                  "121"}]})

  (def wind-driven-fuel-code-variable-tx
    {:db/id              -2
     :bp/nid             (nano-id)
     :bp/uuid            "66295285-9423-4ac7-beae-ce05e260efd1",
     :variable/name      "Wind Driven Surface Fuel Model Code",
     :variable/bp6-label "Fuel Model Code",
     :variable/bp6-code  "vSurfaceFuelBedModelCode",
     :variable/kind      :discrete,
     :variable/list      (:db/id wind-driven-fuel-code-list-tx)}) 

  (def fuel-models-parent-group
    (sm/t-key->eid db "behaveplus:surface:input:fuel_models:standard"))

  (def spot-wind-driven-surface-fire-output
    (sm/t-key->uuid db "behaveplus:surface:output:spot:maximum_spotting_distance:wind_driven_surface_fire"))

  (def wind-driven-fuel-code-subgroup-tx
    (merge
     (sm/->subgroup fuel-models-parent-group
                    "Wind Driven Fuel Model"
                    "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model")
     {:db/id                       -3
      :group/conditionals-operator :or
      :group/conditionals
      [(sm/->gv-conditional spot-wind-driven-surface-fire-output :equal "true")]}))

  (def wind-driven-group-variable-tx
    (merge
     (sm/->group-variable (:db/id wind-driven-fuel-code-subgroup-tx)
                          (:db/id wind-driven-fuel-code-variable-tx)
                          "behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model")
     {:group-variable/cpp-namespace      (sm/cpp-ns->uuid conn "global")
      :group-variable/cpp-class          (sm/cpp-class->uuid conn "SIGSurface")
      :group-variable/cpp-function       (sm/cpp-fn->uuid conn "SIGSurface" "setFuelModelNumber")
      :group-variable/cpp-parameter      (sm/cpp-param->uuid conn "SIGSurface" "setFuelModelNumber" "fuelModelNumber")
      :group-variable/discrete-multiple? true}))

  (def new-translations-tx
    (sm/build-translations-payload
     conn
     100
     {"behaveplus:surface:input:fuel_models:standard:wind-driven-fuel-model:wind-driven-fuel-model" "Wind Driven Fuel Model"}))

  (comment
    (def tx (d/transact conn [wind-driven-fuel-code-list-tx
                              wind-driven-fuel-code-variable-tx
                              wind-driven-fuel-code-subgroup-tx
                              wind-driven-group-variable-tx
                              new-translations-tx]))
    )

  (comment
    (sm/rollback-tx! conn tx)
    )
  )
