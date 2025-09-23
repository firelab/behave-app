(ns migrations.2025-09-17-update-ssd-tool
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

#_{:clj-kondo/ignore [:missing-docstring]}
(def ssd-tool
  (d/entity (d/db conn) 
            (d/q '[:find ?e .
                   :in $ ?name
                   :where [?e :tool/name ?name]]
                 (d/db conn) "Safe Separation Distance")))

#_{:clj-kondo/ignore [:missing-docstring]}
(def ssd-subtool (first (:tool/subtools ssd-tool)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def ssd-name "Safe Separation Distance & Safety Zone Size")

#_{:clj-kondo/ignore [:missing-docstring]}
(def translations-payload
  (sm/update-translations-payload
   conn
   "en-US"
   {(:tool/translation-key ssd-tool)       ssd-name
    (:subtool/translation-key ssd-subtool) ssd-name}))

#_{:clj-kondo/ignore [:missing-docstring]}
(def name-payload
  [{:db/id     (:db/id ssd-tool)
    :tool/name ssd-name}
   {:db/id        (:db/id ssd-subtool)
    :subtool/name ssd-name}])

(def payload (concat name-payload translations-payload))

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

