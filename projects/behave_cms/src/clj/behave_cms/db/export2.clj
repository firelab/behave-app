(ns behave-cms.db.export
  (:require [clojure.string     :as str]
            [clojure.set        :refer [rename-keys]]
            [behave-cms.db.core :as db]))

;;; Entity to Tables Mapping

(def ^:private entity->tables
  {:application :applications
   :module :modules
   :submodule :submodules
   :group :groups
   :gv :group_variables
   :variable :variables
   :help :help_pages
   :language :languages
   :translation :translations
   :namespace :cpp.namespaces
   :enum :cpp.enums
   :enum-member :cpp.enum_members
   :class :cpp.classes
   :function :cpp.functions
   :parameter :cpp.function_parameters})

;;; Helper Fns

(defn- fmt-col [entity col-kw]
  (let [col-str    (-> col-kw (str) (subs 1))
        entity-str (-> entity (str) (subs 1))]
    (cond
      (= col-str "uuid")
      [col-kw (keyword (str entity-str "/uuid"))]

      (str/starts-with? col-str entity-str)
      [col-kw (-> col-str
                  (str/replace-first #"_" "/")
                  (str/replace #"_" "-")
                  (keyword))]

      (str/ends-with? col-str "rid")
      [col-kw (keyword (str entity-str "/" (str/replace-first col-str #"_rid" "")))]

      :else
      [col-kw (keyword (str entity-str "/" (str/replace col-str #"_" "-")))])))

;;; Public Fns

(defn export-table
  ([{:keys [entity]}]
   (export-table (keyword entity) (get entity->tables (keyword entity))))
  ([entity table-name]
   (let [cols     (keys (db/exec-one! {:select [:*] :from table-name :limit 1}))
         cols     (filterv #(not (contains? #{:updated_at :created_at} %)) cols)
         mappings (into {} (mapv (partial fmt-col entity) cols))
         cols     (mapv #(if (or (= % :uuid) (str/ends-with? (str %) "rid")) [[:cast % :varchar]] %) cols)]
     (mapv #(rename-keys % mappings) (db/exec! {:select cols :from table-name})))))

(defn export-all []
  (mapv (fn [[entity table]] (export-table entity table)) entity->tables))

(defn export-table-triples
  ([{:keys [entity]}]
   (export-table-triples (keyword entity) (get entity->tables (keyword entity))))
  ([entity table-name]
   (let [rows     (export-table entity table-name)
         uuid-key (keyword (str (subs (str entity) 1) "/uuid"))]
     (mapv first (persistent! (reduce (fn [acc row]
                            (let [uuid (get row uuid-key)]
                              (conj! acc (filterv (comp not nil?) (mapv (fn [[k v]] (when-not (= k uuid-key) [uuid k v])) row)))))
                          (transient [])
                          rows))))))

(defn export-all-triples []
  (apply concat (mapv (fn [[entity table]]
                        (export-table-triples entity table)) entity->tables)))
