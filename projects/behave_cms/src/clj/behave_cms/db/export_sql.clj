(ns behave-cms.db.export
  (:require [clojure.string     :as str]
            [clojure.set        :refer [rename-keys map-invert]]
            [behave-cms.db.core :as db]))

;;; Entity to Tables Mapping

(def ^:private entity->tables
  {:application     :applications
   :module          :modules
   :submodule       :submodules
   :group           :groups
   :group-variable  :group_variables
   :variable        :variables
   :help-page       :help_pages
   :language        :languages
   :translation     :translations
   :cpp.namespace   :cpp.namespaces
   :cpp.enum        :cpp.enums
   :cpp.enum-member :cpp.enum_members
   :cpp.class       :cpp.classes
   :cpp.function    :cpp.functions
   :cpp.parameter   :cpp.function_parameters})

(def ^:private entity-translations
  {"namespace"   "cpp.namespace"
   "enum"        "cpp.enum"
   "enum-member" "cpp.enum-member"
   "class"       "cpp.class"
   "function"    "cpp.function"
   "parameter"   "cpp.parameter"})

;;; Helper Fns

(defn- fmt-col [entity col-kw]
  (let [col-str    (-> col-kw (str) (subs 1))
        entity-str (-> entity (str) (subs 1))]
    (cond
      (= col-str "uuid")
      [col-kw :bp/uuid]

      (and (not= col-str entity-str) (str/starts-with? col-str entity-str))
      [col-kw (-> col-str
                  (str/replace-first #"_" "/")
                  (str/replace #"_" "-")
                  (keyword))]

      (str/ends-with? col-str "rid")
      (let [parent-entity (str/replace-first col-str #"_rid" "")
            parent-entity (get entity-translations parent-entity parent-entity)
            entity-str    (get (map-invert entity-translations) entity-str entity-str)]
        [col-kw (keyword (str parent-entity "/_" entity-str))])

      (str/ends-with? col-str "name")
      [col-kw (keyword (str entity-str "/name"))]

      :else
      [col-kw (keyword (str entity-str "/" (str/replace col-str #"_" "-")))])))

(defn- xform-reference-lookup
  "Transforms `{:entity_rid \"<uuid>\"}` to `{:entity_rid [:bp/uuid \"<uuid>\"]}` for each of the `ref-cols`.
   This allows us to resolve parent/child entities in Datahike/Datascript."
  [entity ref-cols]
  (reduce (fn [entity ref-col]
            (assoc entity ref-col [:bp/uuid (get entity ref-col)]))
          entity
          ref-cols))

;;; Public Fns

(defn export-table
  ([{:keys [entity]}]
   (export-table (keyword entity) (get entity->tables (keyword entity))))
  ([entity table-name]
   (let [cols     (keys (db/exec-one! {:select [:*] :from table-name :limit 1}))
         cols     (filterv #(not (contains? #{:updated_at :created_at} %)) cols)
         mappings (into {} (mapv (partial fmt-col entity) cols))
         ref-cols (mapv keyword (filter #(str/ends-with? (str %) "rid") cols))
         cols     (mapv #(if (or (= % :uuid) (str/ends-with? (str %) "rid")) [[:cast % :varchar]] %) cols)]
     (mapv #(-> %
                (xform-reference-lookup ref-cols)
                (rename-keys mappings))
           (db/exec! {:select cols :from table-name})))))

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
