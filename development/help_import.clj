(ns help-import
  (:require 
   [clojure.java.io          :as io]
   [clojure.string           :as str]
   [clojure.set              :refer [rename-keys]]
   [me.raynes.fs             :as fs]
   [behave-cms.server        :as cms]
   [datomic.api              :as d]
   [datomic-store.main       :as ds]
   [dita.xhtml-cleaner       :refer [clean-topic clean-variables]]
   [schema-migrate.interface :as sm]))

;;; Helpers

(defn- today []
  (str (java.time.LocalDate/now)))

(defn- q-english-lang-id [db]
  (d/q '[:find ?e .
         :where
         [?e :language/shortcode "en-US"]] db))

(defn- q-tx [db m-name]
  (d/q '[:find ?e .
         :where
         [?e :bp/migration ?m-name]]
       db m-name))

(defn- remove-existing-pages-tx [db en-us]
  (->> en-us
       (d/pull db '[{:language/help-page [:db/id]}])
       (:language/help-page)
       (map (fn [p] [:db/retractEntity (:db/id p)]))))

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
                                   (filter #(str/includes? (slurp %) "bad snippet") ))
        cleaned-variables     (mapcat clean-variables variable-snippets)]

    (->> cleaned-variables
         (remove #(or (nil? (:key %)) (nil? (:content %))))
         (map #(-> %
                   (rename-keys {:key :help-page/key :content :help-page/content})
                   (assoc :language/_help-page english-lang))))))

;;; Public fns

(defn import-help
  "Imports help from `bases/behave-docs`."
  [& args]
  (cms/init-db! (io/file "projects/behave_cms/resources/config.edn"))
  (let [conn        @ds/datomic-conn
        db          (d/db conn)
        en-us       (q-english-lang-id db)
        -today      (today)
        add-tx      (format "help-import-add-%s" -today)
        remove-tx   (format "help-import-remove-%s" -today)
        ;; Behave Docs dir
        behave-docs (io/file "bases" "behave-docs" "XHTML_Output" "BehaveAppHelp")]

    #_(println (format "Removing old help docs %s" -today))
    (d/transact conn (concat [(sm/->migration remove-tx)]
                             (remove-existing-pages-tx db en-us)))

    #_(println (format "Adding new help docs %s" -today))
    (d/transact conn (concat [(sm/->migration add-tx)]
                             (cleaned-topics-tx behave-docs en-us)
                             (cleaned-variables-tx behave-docs en-us)))))

(defn rollback-import
  "Rollback imports. Option to provide a transaction date."
  [& args]
  (cms/init-db!)
  (let [a-date       (first args)
        conn         @ds/datomic-conn
        db           (d/db conn)
        -today       (today)
        tx-remove-id (q-tx db (format "help-import-remove-%s" (or a-date -today)))
        tx-add-id    (q-tx db (format "help-import-add-%s" (or a-date -today)))]

    (sm/rollback-tx! conn tx-remove-id)
    (sm/rollback-tx! conn tx-add-id)))
