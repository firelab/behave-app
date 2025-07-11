(ns migrations.2025-07-11-update-contain-search-table-name
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
(def payload
  (concat [(sm/postwalk-insert
            {:db/id                        (sm/name->eid conn :search-table/name "Minimum Fireline Production Rate Summary")
             :search-table/name            "Minimum Fireline Production Required for Containment"
             :search-table/translation-key "behaveplus:minimum-fireline-production-required-for-containment"})]
          (sm/build-translations-payload conn {"behaveplus:minimum-fireline-production-required-for-containment" "Minimum Fireline Production Required for Containment"})))

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
