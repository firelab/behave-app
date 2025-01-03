(ns help-import
  (:require 
   [clojure.java.io          :as io]
   [clojure.set              :refer [rename-keys]]
   [me.raynes.fs             :as fs]
   [behave-cms.server        :as cms]
   [datomic.api              :as d]
   [datomic-store.main       :as ds]
   [dita.xhtml-cleaner       :refer [clean-topic clean-variable]]
   [schema-migrate.interface :as sm]))

#_{:clj-kondo/ignore [:missing-docstring]}
(do 
  (cms/init-db!)
  (def db (d/db @ds/datomic-conn))

  (def english-lang
    (d/q '[:find ?e .
           :where
           [?e :language/shortcode "en-US"]] db))

  (def remove-help-pages-tx
    (->> english-lang
         (d/pull db '[{:language/help-page [:db/id]}])
         (:language/help-page)
         (map (fn [p] [:db/retractEntity (:db/id p)]))))

  ;;; Behave Docs dir
  (def behave-docs (io/file "bases" "behave-docs" "XHTML_Output" "BehaveAppHelp"))

  ;;; Topics
  (def topics-dir (io/file (str behave-docs) "Content"))
  (def topics (fs/find-files topics-dir #".*htm"))
  (def cleaned-topics (mapv clean-topic topics))

  ;;; Variables
  (def variable-snippets-dir (io/file (str behave-docs) "Resources" "Snippets" "Variables"))
  (def variable-snippets (fs/glob variable-snippets-dir "*.htm"))
  (def cleaned-variables (map clean-variable variable-snippets))

  (def topics-help-tx
    (->> cleaned-topics
         (remove #(or (nil? (:key %)) (nil? (:content %))))
         (map #(-> %
                   (rename-keys {:key :help-page/key :content :help-page/content})
                   (assoc :language/_help-page english-lang)))))

  (def variables-help-tx
    (->> cleaned-variables
         (remove #(or (nil? (:key %)) (nil? (:content %))))
         (map #(-> %
                   (rename-keys {:key :help-page/key :content :help-page/content})
                   (assoc :language/_help-page english-lang)))))

  )

;; Perform TX
(comment
  (do 
    (def tx-1 (d/transact @ds/datomic-conn remove-help-pages-tx))
    (def tx-2 (d/transact @ds/datomic-conn (concat topics-help-tx variables-help-tx)))
    )
  )

;; Rollback
(comment 
  (do 
    (sm/rollback-tx! @ds/datomic-conn tx-1)
    (sm/rollback-tx! @ds/datomic-conn tx-2)
    )
  )

(comment
  (defn q-help-page [db lang-eid key]
    (d/q '[:find ?h .
           :in $ ?l
           :where
           [?l :language/help-page ?h]
           [?h :help-page/key ?k]]
         db lang-eid key))

  (q-help-page db english-lang "behaveplus:crown:help")
  (q-help-page db english-lang "behaveplus:crown:output:size:help")
 )
