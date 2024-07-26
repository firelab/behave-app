(ns migrations.2024-07-23-surface-mortality-bole-char
  (:require [schema-migrate.interface :as sm]
            [behave.schema.rules :refer [vms-rules]]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [nano-id.core :refer [nano-id]]
            [cms-import :refer [add-export-file-to-conn]]
            [datascript.core :refer [squuid]]
            [clojure.walk :as walk]
            [clojure.string :as str]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

(add-export-file-to-conn "./cms-exports/SIGMortality.edn" conn)

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

(defn- build-gv [tempid direction]
  (let [dir (name direction)]
    (sm/postwalk-assoc-uuid+nid
     {:group/name            (str "Bole Char Height " (str/capitalize (name direction)))
      :group/group-variables [{:db/id                        (* (inc tempid) -1)
                               :group-variable/cpp-namespace (sm/cpp-ns->uuid conn "global")
                               :group-variable/cpp-class     (sm/cpp-class->uuid conn "SIGMortality")
                               :group-variable/cpp-function  (sm/cpp-fn->uuid conn (str "setBoleCharHeightFromFlameLength" (str/capitalize dir)))
                               :group-variable/translation-key
                               (format "behaveplus:mortality:input:fuelvegetation_overstory:bole-char-height-%s:bole-char-height-%s" dir dir)
                               :group-variable/help-key
                               (format "behaveplus:mortality:input:fuelvegetation_overstory:bole-char-height-%s:bole-char-height-%s:help" dir dir)
                               :group-variable/result-translation-key
                               (format "behaveplus:mortality:result:fuelvegetation_overstory:bole-char-height-%s:bole-char-height-%s" dir dir)}]
      :group/conditionals    [{:conditional/type     :module
                               :conditional/operator :in
                               :conditional/values   #{"surface" "crown" "mortality" "contain"}}]
      :group/translation-key (format "behaveplus:mortality:input:fuelvegetation_overstory:bole-char-height-%s" dir)})))

(defn build-v [[direction tempid]]
  (sm/postwalk-assoc-uuid+nid
   {:variable/name            (str "Bole Char Height " (str/capitalize (name direction)))
    :variable/domain-uuid     (sm/name->uuid conn :domain/name "Tree & Canopy Height")
    :variable/kind            :continuous
    :variable/group-variables [tempid]}))

(defn build-link [[src dest]]
  (sm/postwalk-assoc-uuid+nid
   {:link/source      src
    :link/destination dest}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat [{:db/id            (sm/t-key->eid conn "behaveplus:mortality:input:fuelvegetation_overstory")
            :submodule/groups (map-indexed build-gv [:heading :backing :flanking])}]
          (map build-link
               [[(sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:heading_flame_length") -1]
                [(sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:backing_flame_length") -2]
                [(sm/t-key->eid conn "behaveplus:surface:output:fire_behavior:surface_fire:flanking_flame_length") -3]])
          (map build-v [[:heading -1] [:backing -2] [:flanking -3]])))

;; ===========================================================================================================
;; Transact
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (def tx-data @(d/transact conn payload)))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment (sm/rollback-tx! conn tx-data))
