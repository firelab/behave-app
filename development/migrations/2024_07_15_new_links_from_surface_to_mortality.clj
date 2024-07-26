(ns migrations.2024-07-15-new-links-from-surface-to-mortality
  (:require [schema-migrate.interface :as sm]
            [behave.schema.rules :refer [vms-rules]]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [nano-id.core :refer [nano-id]]
            [cms-import :refer [add-export-file-to-conn]]
            [datascript.core :refer [squuid]]
            [clojure.walk :as walk]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(add-export-file-to-conn "./cms-exports/SIGMortality.edn" conn)

;; ===========================================================================================================
;; Workspace
;; ===========================================================================================================

(d/q '[:find ?e ?doc
       :in $ %
       :where
       [?e :db/doc ?doc]]
     (d/db conn)
     vms-rules)

#_{:clj-kondo/ignore [:missing-docstring]}
(defn insert-bp-uuid [x]
  (if (map? x)
    (assoc x :bp/uuid (str (squuid)))
    x))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn insert-bp-nid [x]
  (if (map? x)
    (assoc x :bp/nid (nano-id))
    x))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn postwalk-assoc-uuid+nid [data]
  (->> data
       (walk/postwalk insert-bp-uuid)
       (walk/postwalk insert-bp-nid)))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn name->eid [attr name]
  (d/q '[:find ?e .
         :in $ ?attr ?name
         :where [?e ?attr ?name]]
       (d/db conn)
       attr
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn name->uuid [attr name]
  (d/q '[:find ?uuid .
         :in $ ?attr ?name
         :where
         [?e ?attr ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       attr
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn name->nid [attr name]
  (d/q '[:find ?nid .
         :in $ ?attr ?name
         :where
         [?e ?attr ?name]
         [?e :bp/nid ?nid]]
       (d/db conn)
       attr
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn cpp-ns->uuid [name]
  (d/q '[:find ?uuid .
         :in $ % ?name
         :where
         [?e :cpp.namespace/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       vms-rules
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn cpp-class->uuid [name]
  (d/q '[:find ?uuid .
         :in $ % ?name
         :where
         [?e :cpp.class/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       vms-rules
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn cpp-fn->uuid [name]
  (d/q '[:find ?uuid .
         :in $ % ?name
         :where
         [?e :cpp.function/name ?name]
         [?e :bp/uuid ?uuid]]
       (d/db conn)
       vms-rules
       name))

#_{:clj-kondo/ignore [:missing-docstring]}
(def english-language-eid (d/q '[:find ?e .
                                 :in $
                                 :where
                                 [?e :language/name "English"]]
                               (d/db conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-new-entities
  [;;New Group and Group variable inputs
   {:db/id                 -1
    :group/name            "Surface Fire Fireline Intensity"
    :group/translation-key "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity"
    :group/help-key        "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity:help"
    :group/conditionals    [{:conditional/type     :module
                             :conditional/operator :equal
                             :conditional/values   #{"mortality"}}]
    :group/group-variables
    [{:db/id                        -2
      :group-variable/cpp-namespace (cpp-ns->uuid "global")
      :group-variable/cpp-class     (cpp-class->uuid "SIGMortality")
      :group-variable/cpp-function  (cpp-fn->uuid "setSurfaceFireFirelineIntensity")
      :group-variable/translation-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity:surface-fire-fireline-intensity-at-vector"
      :group-variable/result-translation-key
      "behaveplus:mortality:result:scorch:fire:surface-fire-fireline-intensity:surface-fire-fireline-intensity-at-vector"
      :group-variable/help-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity:surface-fire-fireline-intensity-at-vector:help"
      :group-variable/order         0}]}

   {:db/id                 -5
    :group/name            "Surface Fire Fireline Intensity Backing"
    :group/translation-key "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity-backing"
    :group/help-key        "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity-backing:help"
    :group/research?       true
    :group/group-variables
    [{:db/id                        -6
      :group-variable/cpp-namespace (cpp-ns->uuid "global")
      :group-variable/cpp-class     (cpp-class->uuid "SIGMortality")
      :group-variable/cpp-function  (cpp-fn->uuid "setSurfaceFireFirelineIntensityBacking")
      :group-variable/translation-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity-backing:surface-fire-fireline-intensity-at-vector"
      :group-variable/result-translation-key
      "behaveplus:mortality:result:scorch:fire:surface-fire-fireline-intensity-backing:surface-fire-fireline-intensity-at-vector"
      :group-variable/help-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity-backing:surface-fire-fireline-intensity-at-vector:help"
      :group-variable/order         0}]}

   {:db/id                 -7
    :group/name            "Surface Fire Fireline Intensity Flanking"
    :group/translation-key "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity-flanking"
    :group/help-key        "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity-flanking:help"
    :group/research?       true
    :group/group-variables
    [{:db/id                        -8
      :group-variable/cpp-namespace (cpp-ns->uuid "global")
      :group-variable/cpp-class     (cpp-class->uuid "SIGMortality")
      :group-variable/cpp-function  (cpp-fn->uuid "setSurfaceFireFirelineIntensityFlanking")
      :group-variable/translation-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity-flanking:surface-fire-fireline-intensity-at-vector"
      :group-variable/result-translation-key
      "behaveplus:mortality:result:scorch:fire:surface-fire-fireline-intensity-flanking:surface-fire-fireline-intensity-at-vector"
      :group-variable/help-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity-flanking:surface-fire-fireline-intensity-at-vector:help"
      :group-variable/order         0}]}

   {:db/id                 -11
    :group/name            "Surface Fire Midflame Wind Speed"
    :group/translation-key "behaveplus:mortality:input:scorch:fire:surface-fire-midflame-wind-speed"
    :group/help-key        "behaveplus:mortality:input:scorch:fire:surface-fire-midflame-wind-speed:help"
    :group/conditionals    [{:conditional/type     :module
                             :conditional/operator :equal
                             :conditional/values   #{"mortality"}}]
    :group/group-variables
    [{:db/id                        -12
      :group-variable/cpp-namespace (cpp-ns->uuid "global")
      :group-variable/cpp-class     (cpp-class->uuid "SIGMortality")
      :group-variable/cpp-function  (cpp-fn->uuid "setMidFlameWindSpeed")
      :group-variable/translation-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity:midflame-wind-speed"
      :group-variable/result-translation-key
      "behaveplus:mortality:result:scorch:fire:surface-fire-fireline-intensity:midflame-wind-speed"
      :group-variable/help-key
      "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity:midflame-wind-speed:help"
      :group-variable/order         0}]}

   {:db/id                 -13
    :group/name            "Air Temperature"
    :group/translation-key "behaveplus:mortality:input:scorch:fire:air-temperature"
    :group/help-key        "behaveplus:mortality:input:scorch:fire:air-temperature:help"
    :group/group-variables
    [{:db/id                        -14
      :group-variable/cpp-namespace (cpp-ns->uuid "global")
      :group-variable/cpp-class     (cpp-class->uuid "SIGMortality")
      :group-variable/cpp-function  (cpp-fn->uuid "setAirTemperature")
      :group-variable/translation-key
      "behaveplus:mortality:input:scorch:fire:air-temperature:air-temperature"
      :group-variable/result-translation-key
      "behaveplus:mortality:result:scorch:fire:air-temperature:air-temperature"
      :group-variable/help-key
      "behaveplus:mortality:input:scorch:fire:air-temperature:air-temperature:help"
      :group-variable/order         0}]}

   ;; New Translations
   {:db/id                   -42
    :translation/key         "behaveplus:mortality:input:scorch:fire:air-temperature"
    :translation/translation "Air Temperature"}

   {:db/id                   -43
    :translation/key         "behaveplus:mortality:input:scorch:fire:surface-fire-midflame-wind-speed"
    :translation/translation "Midflame Wind Speed"}

   {:db/id                   -44
    :translation/key         "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity"
    :translation/translation "Fireline Intensity"}

   {:db/id                   -45
    :translation/key         "behaveplus:mortality:input:scorch:fire:surface-fire-fireline-intensity:surface-fire-fireline-intensity-at-vector"
    :translation/translation "Fireline Intensity"}

   ;; New links
   {:link/source      (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:heading_fireline_intensity")
    :link/destination -2}

   {:link/source      (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:backing_fireline_intensity")
    :link/destination -6}

   {:link/source      (sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:flanking_fireline_intensity")
    :link/destination -8}

   {:link/source      (sm/t-key->eid conn "behaveplus:surface:input:wind_speed:wind_speed:wind_speed")
    :link/destination -12}])

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-refs-to-existing-entities
  [{:db/id            (sm/t-key->eid conn "behaveplus:mortality:input:scorch")
    :submodule/groups [-1 -5 -7 -11 -13]}
   {:db/id                    (name->eid :variable/name "Surface Fireline Intensity at Vector")
    :variable/group-variables [-2 -6 -8]}
   {:db/id                    (name->eid :variable/name "Midflame (eye level) Wind Speed")
    :variable/group-variables [-12]}
   {:db/id                    (name->eid :variable/name "Air Temperature")
    :variable/group-variables [-14]}
   {:db/id                english-language-eid
    :language/translation [-42 -43 -44 -45]}])

;; Retract the conditional from the submodule Mortality > Scorch (input).
#_{:clj-kondo/ignore [:missing-docstring]}
(def retract-entities
  [[:db/retractEntity 4611681620380880818]])

;; ===========================================================================================================
;; Payload and Transact
;; ===========================================================================================================


#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat (-> add-new-entities postwalk-assoc-uuid+nid)
          add-refs-to-existing-entities
          retract-entities))

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data @(d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment (sm/rollback-tx! conn tx-data))
