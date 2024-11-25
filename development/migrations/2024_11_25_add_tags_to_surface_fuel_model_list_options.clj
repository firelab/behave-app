(ns migrations.2024-11-25-add-tags-to-surface-fuel-model-list-options
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]))

;; ===========================================================================================================
;; Overview
;; ===========================================================================================================

;; Add `:list-option/tags` to existing SurfaceFuelModel list options

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(def list-option->tag
  {"FB1/1 - Short grass (Static)"                                         :Grass
   "FB2/2 - Timber grass and understory (Static)"                         :Grass
   "FB3/3 - Tall grass (Static)"                                          :Grass
   "FB4/4 - Chaparral (Static)"                                           :Shrub
   "FB5/5 - Brush (Static)"                                               :Shrub
   "FB6/6 - Dormant brush, hardwood slash (Static)"                       :Shrub
   "FB7/7 - Southern rough (Static)"                                      :Shrub
   "FB8/8 - Short needle litter (Static)"                                 :Timber-Litter
   "FB9/9 - Long needle or hardwood litter (Static)"                      :Timber-Litter
   "FB10/10 - Timber litter & understory (Static)"                        :Timber-Litter
   "FB11/11 - Light logging slash (Static)"                               :Slash-Blowdown
   "FB12/12 - Medium logging slash (Static)"                              :Slash-Blowdown
   "FB13/13 - Heavy logging slash (Static)"                               :Slash-Blowdown
   "GR1/101 - Short sparse, dry climate grass (Dynamic)"                  :Grass
   "GR2/102 - Low load dry climate grass (Dynamic)"                       :Grass
   "GR3/103 - Low load very coarse, humid climate grass (Dynamic)"        :Grass
   "GR4/104 - Moderate load dry climate grass (Dynamic)"                  :Grass
   "GR5/105 - Low load humid climate grass (Dynamic)"                     :Grass
   "GR6/106 - Moderate load humid climate grass (Dynamic)"                :Grass
   "GR7/107 - High load dry climate grass (Dynamic)"                      :Grass
   "GR8/108 - High load very coarse, humid climate grass (Dynamic)"       :Grass
   "GR9/109 - Very high load humid climate grass (Dynamic)"               :Grass
   "GS1/121 - Low load dry climate grass-shrub (Dynamic)"                 :Grass-Shrub
   "GS2/122 - Moderate load, dry climate grass-shrub (Dynamic)"           :Grass-Shrub
   "GS3/123 - Moderate load, humid climate grass-shrub (Dynamic)"         :Grass-Shrub
   "GS4/124 - High load, humid climate grass-shrub (Dynamic)"             :Grass-Shrub
   "SH1/141 - Low load, dry climate shrub (Dynamic)"                      :Shrub
   "SH2/142 - Moderate load, dry climate shrub (Static)"                  :Shrub
   "SH3/143 - Moderate load, humid climate shrub (Static)"                :Shrub
   "SH4/144 - Low load, humid climate timber-shrub (Static)"              :Shrub
   "SH5/145 - High load, dry climate shrub (Static)"                      :Shrub
   "SH6/146 - Low load, humid climate shrub (Static)"                     :Shrub
   "SH7/147 - Very high load, dry climate shrub (Static)"                 :Shrub
   "SH8/148 - High load, humid climate shrub (Static)"                    :Shrub
   "SH9/149 - Very high load, humid climate shrub (Dynamic)"              :Shrub
   "TU1/161 - Light load, dry climate timber-grass-shrub (Dynamic)"       :Timber-Understory
   "TU2/162 - Moderate load, humid climate timber-shrub (Static)"         :Timber-Understory
   "TU3/163 - Moderate load, humid climate timber-grass-shrub (Dynamic)"  :Timber-Understory
   "TU4/164 - Dwarf conifer understory (Static)"                          :Timber-Understory
   "TU5/165 - Very high load, dry climate timber-shrub (Static)"          :Timber-Understory
   "TL1/181 - Low load, compact conifer litter (Static)"                  :Timber-Litter
   "TL2/182 - Low load broadleaf litter (Static)"                         :Timber-Litter
   "TL3/183 - Moderate load conifer litter (Static)"                      :Timber-Litter
   "TL4/184 - Small downed logs (Static)"                                 :Timber-Litter
   "TL5/185 - High load conifer litter (Static)"                          :Timber-Litter
   "TL6/186 - High load broadleaf litter (Static)"                        :Timber-Litter
   "TL7/187 - Large downed logs (Static)"                                 :Timber-Litter
   "TL8/188 - Long-needle litter (Static)"                                :Timber-Litter
   "TL9/189 - Very high load broadleaf litter (Static)"                   :Timber-Litter
   "SB1/201 - Low load activity fuel (Static)"                            :Slash-Blowdown
   "SB2/202 - Moderate load activity or low load blowdown (Static)"       :Slash-Blowdown
   "SB3/203 - High load activity fuel or moderate load blowdown (Static)" :Slash-Blowdown
   "SB4/204 - High load blowdown (Static)"                                :Slash-Blowdown
   "NB1 - Urban, developed [91]"                                          :Non-Burnable
   "NB2 - Snow, ice [92]"                                                 :Non-Burnable
   "NB3 - Agricultural [93]"                                              :Non-Burnable
   "NB4 - Future standard non-burnable [94]"                              :Non-Burnable
   "NB5 - Future standard non-burnable [95]"                              :Non-Burnable
   "NB8 - Open water [98]"                                                :Non-Burnable
   "NB9 - Bare ground [99]"                                               :Non-Burnable})

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (map (fn [[name tag]]
         {:db/id            (sm/name->eid conn :list-option/name name)
          :list-option/tags [tag]}) list-option->tag))

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
