(ns migrations.2024-10-17-add-input-group-translations
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave.schema.rules :refer [vms-rules]]
            [behave-cms.server :as cms]
            [clojure.string :as str]))

;; ===========================================================================================================
;; Initialize
;; ===========================================================================================================

(cms/init-db!)

#_{:clj-kondo/ignore [:missing-docstring]}
(def conn (default-conn))

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-translation-to-existing-translation-keys
  (->> (d/q '[:find ?t-key ?name
              :in $ %
              :where
              [?e :group/translation-key ?t-key]
              [?e :group/name ?name]]
            (d/db conn)
            vms-rules)
       (reduce (fn [acc [t-key nname]]
                 (assoc acc t-key nname))
               {})
       (sm/build-translations-payload conn)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def add-new-result-translation-keys-to-group-entities
  (->> (d/q '[:find ?e ?t-key ?name
              :in $ %
              :where
              [?e :group/translation-key ?t-key]
              [?e :group/name ?name]]
            (d/db conn)
            vms-rules)
       (map (fn [[eid t-key]]
              {:db/id                        eid
               :group/result-translation-key (str/replace t-key #":input:|:output:" ":result:")}))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload
  (concat add-translation-to-existing-translation-keys
          add-new-result-translation-keys-to-group-entities))

;; adds new translations for Wind Measured at -> wind Tpe in results input table
#_{:clj-kondo/ignore [:missing-docstring]}
(def add-new-translations-payload
  (sm/build-translations-payload conn {"behaveplus:surface:result:wind_speed:wind_height" "wind type"}))

;; ===========================================================================================================
;; Transact Payload
;; ===========================================================================================================

(comment
  #_{:clj-kondo/ignore [:missing-docstring]}
  (do (def tx-data (d/transact conn payload ))
      (def tx-data-2 (d/transact conn add-new-translations-payload))))

;; ===========================================================================================================
;; In case we need to rollback.
;; ===========================================================================================================

(comment
  (do
    (sm/rollback-tx! conn @tx-data-2)
    (sm/rollback-tx! conn @tx-data)))
