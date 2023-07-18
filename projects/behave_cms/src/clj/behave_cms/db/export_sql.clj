(ns behave-cms.db.export
  (:require [clojure.string     :as str]
            [clojure.set        :refer [rename-keys map-invert]]
            [behave-cms.db.core :as db]))

;;; Entity to Tables Mapping

(def ^:private entity->table
  {:application     :applications
   :module          :modules
   :submodule       :submodules
   :group           :groups
   :group-variable  :group_variables
   :variable        :variables
   :language        :languages
   :help-page       :help_pages
   :translation     :translations
   :cpp.namespace   :cpp.namespaces
   :cpp.enum        :cpp.enums
   :cpp.enum-member :cpp.enum_members
   :cpp.class       :cpp.classes
   :cpp.function    :cpp.functions
   :cpp.parameter   :cpp.function_parameters
   :user            :users})

(def ordered-entities
  [:user
   :application
   :module
   :submodule
   :group
   :variable
   :group-variable
   :language
   :help-page
   :translation
   :cpp.namespace
   :cpp.enum
   :cpp.enum-member
   :cpp.class
   :cpp.function
   :cpp.parameter])

(def ^:private entity-translations
  {"namespace"    "cpp.namespace"
   "enum"         "cpp.enum"
   "enum-member"  "cpp.enum-member"
   "class"        "cpp.class"
   "function"     "cpp.function"
   "parameter"    "cpp.parameter"})

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
   (export-table (keyword entity) (get entity->table (keyword entity))))
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
  (mapv (fn [entity] (export-table entity (get entity->table entity))) ordered-entities))

(defn export-table-triples
  ([{:keys [entity]}]
   (export-table-triples (keyword entity) (get entity->table (keyword entity))))
  ([entity table-name]
   (let [rows     (export-table entity table-name)
         uuid-key (keyword (str (subs (str entity) 1) "/uuid"))]
     (mapv first (persistent! (reduce (fn [acc row]
                            (let [uuid (get row uuid-key)]
                              (conj! acc (filterv (comp not nil?) (mapv (fn [[k v]] (when-not (= k uuid-key) [uuid k v])) row)))))
                          (transient [])
                          rows))))))

(defn export-all-triples []
  (apply concat (mapv (fn [entity]
                        (export-table-triples (get entity->table entity))) ordered-entities)))


(comment

  (require '[clojure.java.io :as io])
  (require '[behave.schema.core :refer [all-schemas]])
  (require '[datahike.api :as d])
  (require '[datahike.core :as dc])
  (require '[datom-store.main :as s])
  (require '[behave-cms.store :as store])
  (require '[config.interface :refer [get-config load-config]])
  (require '[behave.download-vms :refer [->io
                                         ->kind
                                         ->rename-keys
                                         ->repeat?
                                         dissoc-nil
                                         ->longify]])

  (def exported-entities (export-all))

  (def users 
    (map #(-> %
              (rename-keys {:user/verified :user/verified? :user/super-admin :user/super-admin?})
              (dissoc :user/otp-code :user/settings :user/reset-key)) (first exported-entities)))
  users

  (def applications (map #(dissoc % :application/settings) (second exported-entities)))

  (def processed-entities (->> (drop 2 exported-entities)
                               (apply concat)
                               (mapv #(-> %
                                          (->io)
                                          (->kind)
                                          (->repeat?)
                                          (->longify)
                                          (->rename-keys)
                                          (dissoc-nil)))))


  (count processed-entities)

  ; Real DB
  (load-config (io/resource "cms-config.edn"))
  (def conn (store/connect! (get-config :database :config) true))

  (s/transact conn (vec users))
  (s/transact conn applications)
  (s/transact conn processed-entities)

  ; Testing after restart
  (def conn (store/default-conn))

  (d/q '[:find ?e :where [?e :user/name ?name]] @conn)

  (d/q '[:find ?e :where [?e :application/name ?name]] @conn)
  
  (d/q '[:find ?e
          :where
          [?a :application/name ?name]
          [?a :application/module ?e]] @conn)

  (d/q '[:find ?e
          :where
          [?e :language/name ?name]] @conn)

  (d/q '[:find ?e
          :where
          [?e :variable/bp6-code ?name]] @conn)
  
  (d/pull @conn '[*] 251)
  (dc/pull @conn '[*] 705)
  )



