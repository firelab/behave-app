(ns migrations.2025-03-18-update-mortality-tree-species-names
  (:require [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]
            [behave-cms.server :as cms]
            [clojure.string :as str]))

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
(defn t-key->translation [t-key]
  (d/q '[:find [?t ?translation]
         :in $ ?t-key
         :where
         [?t :translation/key ?t-key]
         [?t :translation/translation ?translation]]
       (d/db conn)
       t-key))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn update-label [label]
  (let [common-name (str/trim (re-find #"^[^\(]+" label))
        latin+code  (re-find #"(?<=\().+?(?=\))" label)]
    (format "%s (%s)" latin+code common-name)))

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-list-option-names
  (->> (sm/name->eid conn :list/name "MortalitySpeciesMasterList")
       (d/entity (d/db conn))
       (:list/options)
       (map (fn [option]
              (let [nname (:list-option/name option)]
                {:db/id            (:db/id option)
                 :list-option/name (update-label nname)})))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def update-list-option-translations
  (->> (sm/name->eid conn :list/name "MortalitySpeciesMasterList")
       (d/entity (d/db conn))
       (:list/options)
       (map (fn [option]
              (let [[eid translation] (t-key->translation (:list-option/translation-key option))]
                {:db/id                   eid
                 :translation/translation (update-label translation)})))))

#_{:clj-kondo/ignore [:missing-docstring]}
(def payload (concat update-list-option-names update-list-option-translations))

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
