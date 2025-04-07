(ns help-import
  (:require [clojure.java.io          :as io]
            [clojure.set              :refer [rename-keys]]
            [clojure.string           :as str]
            [behave-cms.server        :as cms]
            [datomic-store.main       :as ds]
            [datomic.api              :as d]
            [dita.xhtml-cleaner       :refer [clean-topic clean-variables]]
            [me.raynes.fs             :as fs]
            [schema-migrate.interface :as sm]
            [clojure.java.shell       :refer [sh]]))

;;; Helpers

(defn- now []
  (.format (java.time.LocalDateTime/now)
           (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd-H-mm-ss")))

(defn- q-english-lang-id [db]
  (d/q '[:find ?e .
         :where
         [?e :language/shortcode "en-US"]] db))

(defn- q-tx [db m-name]
  (d/q '[:find ?e .
         :where
         [?e :bp/migration-id ?m-name]]
       db m-name))

(defn- remove-existing-pages-tx [db en-us]
  (->> en-us
       (d/pull db '[{:language/help-page [:db/id]}])
       (:language/help-page)
       (map (fn [p] [:db/retractEntity (:db/id p)]))))

(defn- behave-docs-git-hash []
  (let [process (sh "git" "rev-parse" "--short" "HEAD" :dir "bases/behave-docs")]
    (str/replace (:out process) "\n" "")))

;;; Topics

(defn- cleaned-topics-tx [behave-docs-base-dir english-lang]
  (let [topics-dir     (io/file (str behave-docs-base-dir) "Content")
        topics         (fs/find-files topics-dir #".*htm")
        cleaned-topics (mapv clean-topic topics)]

    (->> cleaned-topics
         (remove #(or (nil? (:key %)) (nil? (:content %))))
         (map #(-> %
                   (rename-keys {:key :help-page/key :content :help-page/content})
                   (assoc :language/_help-page english-lang))))))

;;; Variables

(defn- cleaned-variables-tx [behave-docs-base-dir english-lang]
  (let [variable-snippets-dir (io/file (str behave-docs-base-dir) "Resources" "Snippets" "Variables")
        variable-snippets     (->> (fs/glob variable-snippets-dir "*htm")
                                   (remove #(str/includes? (slurp %) "bad snippet") ))
        cleaned-variables     (mapcat clean-variables variable-snippets)]

    (->> cleaned-variables
         (remove #(or (nil? %) (nil? (:key %)) (nil? (:content %))))
         (map #(-> %
                   (rename-keys {:key :help-page/key :content :help-page/content})
                   (assoc :language/_help-page english-lang))))))

;;; Public fns

(defn import-help!
  "Imports help from `bases/behave-docs`."
  [& _]
  (cms/init-db! (io/file "projects/behave_cms/resources/config.edn"))
  (let [conn         @ds/datomic-conn
        db           (d/db conn)
        en-us        (q-english-lang-id db)
        git-hash     (behave-docs-git-hash)
        add-tx       (format "help-import-add-%s" git-hash)
        remove-tx    (format "help-import-remove-%s" git-hash)
        ;; Behave Docs dir
        behave-docs  (io/file "bases" "behave-docs" "XHTML_Output" "BehaveAppHelp")
        existing-tx? (d/q '[:find ?t .
                            :in $ ?mid
                            :where [?t :bp/migration-id ?mid]]
                          (d/db @ds/datomic-conn) add-tx)]

    (when-not existing-tx?
      #_(println (format "Removing old help docs from hash: %s" -now))
      (d/transact conn (concat [(sm/->migration remove-tx)]
                               (remove-existing-pages-tx db en-us)))

      #_(println (format "Adding new help docs %s" -now))
      (d/transact conn (concat [(sm/->migration add-tx)]
                               (cleaned-topics-tx behave-docs en-us)
                               (cleaned-variables-tx behave-docs en-us))))))

(defn rollback-import!
  "Rollback imports. Option to provide a transaction date."
  [& args]
  (cms/init-db!)
  (let [a-datetime   (first args)
        conn         @ds/datomic-conn
        db           (d/db conn)
        git-hash     (behave-docs-git-hash)
        tx-remove-id (q-tx db (format "help-import-remove-%s" (or a-datetime git-hash)))
        tx-add-id    (q-tx db (format "help-import-add-%s" (or a-datetime git-hash)))]

    (sm/rollback-tx! conn tx-remove-id)
    (sm/rollback-tx! conn tx-add-id)))

(comment
  (import-help)

  (rollback-import "2025-01-18-20-02-31")

  (let [behave-docs (io/file "bases" "behave-docs" "XHTML_Output" "BehaveAppHelp")
        variable-snippets-dir (io/file (str behave-docs) "Resources" "Snippets" "Variables")
        variable-snippets     (->> (fs/glob variable-snippets-dir "*htm")
                                   (remove #(str/includes? (slurp %) "bad snippet")))
        cleaned-variables     (mapcat clean-variables variable-snippets)]

    cleaned-variables)

  (def cvs *1)

  (filter #(= (:key %) "behaveplus:surface:input:fuel_models:standard:fuel_model:fuel_model:help") cvs)
  )
