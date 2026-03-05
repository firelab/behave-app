(ns migrations.2025-11-12-add-fire-shape-diagram
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

(def fire-shape-diagram-eid
  (d/q '[:find ?e .
         :in $
         :where
         [?e :diagram/type :fire-shape]]
       (d/db conn)))

(def new-fire-shape-diagram-output-group-variable
  (sm/->group-variable
   conn
   {:db/id            -1
    :parent-group-eid (sm/t-key->eid conn "behaveplus:surface:output:size:surface___fire_size")
    :order            7
    :variable-eid     (sm/name->eid conn :variable/name "Fire Shape Diagram")
    :hide-results?    true
    :translation-key  "behaveplus:surface:output:size:surface___fire_size:fire-shape-diagram"}))

;; ===========================================================================================================
;; Payload
;; ===========================================================================================================

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat [new-fire-shape-diagram-output-group-variable
           {:db/id                  fire-shape-diagram-eid
            :diagram/group-variable -1}
           {:db/id                  (sm/t-key->eid conn "behaveplus:surface:input:size")
            :submodule/conditionals [(sm/->conditional conn {:ttype               :group-variable
                                                             :operator            :equal
                                                             :values              #{"true"}
                                                             :group-variable-uuid (:bp/uuid new-fire-shape-diagram-output-group-variable)})]}]
          (sm/build-translations-payload conn 2 {"behaveplus:surface:output:size:surface___fire_size:fire-shape-diagram" "Fire Shape Diagram"})))


;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (try (def tx-data @(d/transact conn payload))
       (catch Exception e  (str "caught exception: " (.getMessage e)))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (sm/rollback-tx! conn tx-data))
