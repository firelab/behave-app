(ns cucumber-test-generator.core
  "Core implementation for the cucumber_test_generator component.

   This component generates Cucumber feature files from a Datomic database (behave-cms),
   automating the creation of comprehensive conditional visibility testing scenarios
   for the BehavePlus application.

   The component operates in two phases:
   1. Data Extraction: Query Datomic database and generate test_matrix_data.edn
   2. Feature Generation: Read EDN and generate Cucumber feature files

   Implementation follows patterns from:
   - /home/kcheung/work/code/behave-polylith/development/test_matrix_generator.clj
   - /home/kcheung/work/code/behave-polylith/development/cucumber_test_generator.clj"
  (:require [datomic.api :as d]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.math.combinatorics :as combo]))

;; ===========================================================================================================
;; Translation Resolution (Task 2.2)
;; ===========================================================================================================

(defn get-translation
  "Get the translation value for a translation-key from the database.

   Arguments:
   - db: Datomic database value
   - translation-key: Translation key string to look up

   Returns:
   Translated text string or nil if not found

   Implementation pattern copied from test_matrix_generator.clj lines 31-42"
  [db translation-key]
  (when translation-key
    (when-let [trans (d/q '[:find ?translation .
                            :in $ ?key
                            :where
                            [?t :translation/key ?key]
                            [?t :translation/translation ?translation]]
                          db
                          translation-key)]
      trans)))

;; ===========================================================================================================
;; Database Query Functions (Task 2.3)
;; ===========================================================================================================

(defn find-all-groups-with-conditionals
  "Query all groups that have :group/conditionals attribute.

   Arguments:
   - db: Datomic database value

   Returns:
   Vector of group entity IDs

   Implementation pattern from test_matrix_generator.clj lines 48-54"
  [db]
  (d/q '[:find [?g ...]
         :where
         [?g :group/conditionals]]
       db))

(defn find-all-submodules-with-conditionals
  "Query all submodules that have :submodule/conditionals attribute.

   Arguments:
   - db: Datomic database value

   Returns:
   Vector of submodule entity IDs

   Implementation pattern from test_matrix_generator.clj lines 56-62"
  [db]
  (d/q '[:find [?s ...]
         :where
         [?s :submodule/conditionals]]
       db))

;; ===========================================================================================================
;; Pull Functions for Detailed Entity Data (Task 2.4)
;; ===========================================================================================================

(defn pull-group-details
  "Pull detailed information about a group including parent relationships.

   Arguments:
   - db: Datomic database value
   - group-eid: Group entity ID

   Returns:
   Map with group attributes and parent relationships

   Implementation pattern from test_matrix_generator.clj lines 64-76"
  [db group-eid]
  (d/pull db
          '[*
            {:group/_children [:group/name
                               :group/translation-key]}
            {:submodule/_groups [:submodule/name
                                 :submodule/translation-key
                                 :submodule/io
                                 :submodule/order
                                 {:module/_submodules [:module/name]}]}]
          group-eid))

(defn pull-submodule-details
  "Pull detailed information about a submodule including parent relationships.

   Arguments:
   - db: Datomic database value
   - submodule-eid: Submodule entity ID

   Returns:
   Map with submodule attributes and module relationship

   Implementation pattern from test_matrix_generator.clj lines 78-84"
  [db submodule-eid]
  (d/pull db
          '[*
            {:module/_submodules [:module/name :module/translation-key]}]
          submodule-eid))

;; ===========================================================================================================
;; Group Hierarchy Collection (Task 2.5)
;; ===========================================================================================================

(defn collect-group-hierarchy
  "Recursively collect all parent groups from a group up to the submodule.
   Returns a vector of group names in order from root (closest to submodule) to leaf.

   Example: For 'Live Woody Fuel Moisture' nested under 'By Size Class',
            returns [\"By Size Class\" \"Live Woody Fuel Moisture\"]

   Uses a simple recursive walk up the :group/_children refs.

   Arguments:
   - db: Datomic database value
   - group-eid: Group entity ID to start from

   Returns:
   Vector of translated group names from root to leaf

   Implementation pattern from test_matrix_generator.clj lines 99-117"
  [db group-eid]
  (let [group (d/pull db '[:db/id
                           :group/translation-key
                           {:group/_children [:db/id :group/translation-key]}]
                      group-eid)
        group-name (get-translation db (:group/translation-key group))]
    (if-let [parent-group (:group/_children group)]
      ;; Has a parent, recur and prepend current name
      (conj (collect-group-hierarchy db (:db/id parent-group)) group-name)
      ;; No parent - we're at the root
      [group-name])))

;; ===========================================================================================================
;; Submodule Finder for Nested Groups (Task 2.6)
;; ===========================================================================================================

(defn find-parent-submodule-for-group
  "Traverse up the group hierarchy to find the parent submodule.

   Groups can be nested (subgroups), so we need to walk up through :group/_children
   until we find a group that has a :submodule/_groups relationship.

   Arguments:
   - group-entity: Group entity map (from d/pull)

   Returns:
   Submodule entity map or nil if not found

   Implementation pattern from test_matrix_generator.clj lines 86-97"
  [group-entity]
  (when group-entity
    (if-let [submodule (:submodule/_groups group-entity)]
      submodule
      ;; No submodule at this level, try the parent group
      (when-let [parent-group (:group/_children group-entity)]
        (recur parent-group)))))

;; ===========================================================================================================
;; Group-Variable UUID Resolution (Task 2.7)
;; ===========================================================================================================

(defn resolve-group-variable-uuid
  "Resolve a group-variable UUID to its variable name and parent path.

   Returns nil if the variable has nil :variable/name or :variable/bp6-code (filters invalid entries).

   Arguments:
   - db: Datomic database value
   - gv-uuid: Group-variable UUID

   Returns:
   Map with:
   - :group-variable/translated-name - The translated variable name
   - :group-variable/research? - Whether this is a research variable
   - :io - :input or :output, derived from parent submodule
   - :path - FULL path including Module > Submodule > :io > Groups... (up to variable's parent group)
   - :submodule/order - The parent submodule's order
   - :submodule/research? - Whether submodule is research
   - :group/order - The parent group's order

   Returns nil if translation key is missing

   Implementation pattern from test_matrix_generator.clj lines 119-168"
  [db gv-uuid]
  (let [gv (d/pull db
                   '[*
                     {:variable/_group-variables [:variable/name]}
                     {:group/_group-variables [:db/id
                                               :group/name
                                               :group/translation-key
                                               :group/order
                                               {:group/_children 5}
                                               {:submodule/_groups [:submodule/name
                                                                    :submodule/translation-key
                                                                    :submodule/io
                                                                    :submodule/order
                                                                    :submodule/research?
                                                                    {:module/_submodules [:module/name
                                                                                          :module/translation-key]}]}]}]
                   [:bp/uuid gv-uuid])]
    (when (seq gv)
      (let [parent-group (:group/_group-variables gv)
            parent-group-eid (:db/id parent-group)
            ;; Collect full hierarchy
            group-hierarchy (collect-group-hierarchy db parent-group-eid)
            ;; Use helper function to find the submodule by traversing up the group hierarchy
            parent-submodule (find-parent-submodule-for-group parent-group)
            parent-module (:module/_submodules parent-submodule)
            io (:submodule/io parent-submodule)
            ;; Build complete path: Module > Submodule > :io > Groups...
            module-name (get-translation db (:module/translation-key parent-module))
            submodule-name (:submodule/name parent-submodule)
            base-path (filterv some? (concat [module-name submodule-name] group-hierarchy))
            ;; Insert :io keyword before the last element (the variable's parent group)
            full-path (if (> (count base-path) 2)
                        (vec (concat (take 2 base-path) [io] (drop 2 base-path)))
                        base-path)]
        ;; Only return if variable has valid name/code
        (when (:group-variable/translation-key gv)
          {:group-variable/translated-name (get-translation db (:group-variable/translation-key gv))
           :group-variable/research? (:group-variable/research? gv)
           :io io
           :path full-path
           :submodule/order (:submodule/order parent-submodule)
           :submodule/research? (:submodule/research? parent-submodule)
           :group/order (:group/order parent-group)})))))

;; ===========================================================================================================
;; Enum Value Resolution (Task 2.8)
;; ===========================================================================================================

(defn get-variable-list-options
  "Get list options for a group-variable UUID.

   Returns a map of {value -> translation} for all list options.
   Example: {\"1\" \"10-foot wind speed\", \"2\" \"20-foot wind speed\"}

   Arguments:
   - db: Datomic database value
   - gv-uuid: Group-variable UUID

   Returns:
   Map of value to translation, or nil if the variable doesn't have a list (continuous variables)

   Implementation pattern from test_matrix_generator.clj lines 170-194"
  [db gv-uuid]
  (let [gv (d/pull db
                   '[{:variable/_group-variables
                      [{:variable/list
                        [{:list/options
                          [:list-option/value
                           :list-option/translation-key
                           :list-option/order]}]}]}]
                   [:bp/uuid gv-uuid])
        ;; :variable/_group-variables returns a vector, get first element
        variable (first (:variable/_group-variables gv))]
    (when-let [list-options (get-in variable [:variable/list :list/options])]
      (into {}
            (map (fn [opt]
                   [(:list-option/value opt)
                    (or (get-translation db (:list-option/translation-key opt))
                        (:list-option/value opt))]) ; fallback to value if translation fails
                 list-options)))))

(defn resolve-enum-values
  "Resolve enum values to their human-readable translations.

   Arguments:
   - db: Datomic database value
   - gv-uuid: Group-variable UUID
   - values: Vector of enum values like [\"1\" \"2\"]

   Returns:
   Vector of resolved translations, or nil if any value cannot be resolved.

   Example:
     (resolve-enum-values db gv-uuid [\"1\" \"2\"])
     => [\"10-foot wind speed\" \"20-foot wind speed\"]

     (resolve-enum-values db gv-uuid [\"99\"]) ; value doesn't exist
     => nil

   Implementation pattern from test_matrix_generator.clj lines 196-217"
  [db gv-uuid values]
  (when-let [value-map (get-variable-list-options db gv-uuid)]
    (let [resolved (map #(get value-map %) values)]
      ;; Only return entries with proper translation
      (remove nil? resolved))))

;; ===========================================================================================================
;; Conditional Processing (Task 3.2 - 3.3)
;; ===========================================================================================================

(defn process-conditional
  "Process a single conditional and extract relevant information.

   For input-based conditionals (when :io is :input), resolves enum values
   to their human-readable translations. Returns nil if resolution fails.

   Arguments:
   - db: Datomic database value
   - conditional: Conditional entity map

   Returns:
   Map with :type, :operator, :values, :group-variable (if applicable),
   :sub-conditionals and :sub-conditional-operator (if nested conditionals exist).
   Returns nil if resolution fails.

   Implementation pattern from test_matrix_generator.clj lines 223-257"
  [db conditional]
  (let [cond-type (:conditional/type conditional)
        operator (:conditional/operator conditional)
        values (:conditional/values conditional)
        gv-uuid (:conditional/group-variable-uuid conditional)
        sub-conditionals (:conditional/sub-conditionals conditional)
        sub-operator (:conditional/sub-conditional-operator conditional)

        ;; Resolve group-variable info
        gv-info (when gv-uuid (resolve-group-variable-uuid db gv-uuid))

        ;; For input conditionals, try to resolve enum values
        resolved-values (if (and gv-info (= (:io gv-info) :input))
                          (resolve-enum-values db gv-uuid values)
                          values)]

    ;; Return nil if this is an input conditional and values couldn't be resolved
    (when (or (not= (:io gv-info) :input) ; not an input conditional, proceed
              resolved-values) ; input conditional with resolved values

      (cond-> {:type cond-type
               :operator operator
               :values (if (set? resolved-values) (vec resolved-values) resolved-values)}

        gv-info
        (assoc :group-variable gv-info)

        sub-conditionals
        (assoc :sub-conditionals (keep #(process-conditional db %) sub-conditionals) ; filter nils
               :sub-conditional-operator sub-operator)))))

(defn process-group-conditionals
  "Process all conditionals for a group.
   Filters out conditionals that couldn't be resolved (nil values).
   Returns nil if no valid conditionals remain.

   Arguments:
   - db: Datomic database value
   - group: Group entity map with :group/conditionals

   Returns:
   Map with :conditionals and :conditionals-operator, or nil if no valid conditionals

   Implementation pattern from test_matrix_generator.clj lines 259-270"
  [db group]
  (let [conditionals (:group/conditionals group)
        operator (:group/conditionals-operator group)
        ;; Process and filter out failed resolutions (nils)
        processed (keep #(process-conditional db %) conditionals)]
    (when (seq processed) ; only return if at least one conditional succeeded
      {:conditionals processed
       :conditionals-operator operator})))

(defn process-submodule-conditionals
  "Process all conditionals for a submodule.
   Filters out conditionals that couldn't be resolved (nil values).
   Returns nil if no valid conditionals remain.

   Arguments:
   - db: Datomic database value
   - submodule: Submodule entity map with :submodule/conditionals

   Returns:
   Map with :conditionals and :conditionals-operator, or nil if no valid conditionals

   Implementation pattern from test_matrix_generator.clj lines 272-283"
  [db submodule]
  (let [conditionals (:submodule/conditionals submodule)
        operator (:submodule/conditionals-operator submodule)
        ;; Process and filter out failed resolutions (nils)
        processed (keep #(process-conditional db %) conditionals)]
    (when (seq processed) ; only return if at least one conditional succeeded
      {:conditionals processed
       :conditionals-operator operator})))

;; ===========================================================================================================
;; Data Extraction and Organization (Task Group 2 helpers needed for Task 3.7)
;; ===========================================================================================================

(defn find-parent-submodule
  "Recursively find the parent submodule for a group, even if nested.

   Arguments:
   - db: Datomic database value
   - group-eid: Group entity ID

   Returns:
   Submodule entity map

   Implementation pattern from test_matrix_generator.clj lines 289-303"
  [db group-eid]
  (let [group (d/pull db '[{:submodule/_groups [:submodule/name
                                                :submodule/translation-key
                                                :submodule/io
                                                :submodule/order
                                                {:module/_submodules [:module/name
                                                                      :module/translation-key]}]}
                           {:group/_children [:db/id]}]
                      group-eid)]
    (if-let [submodule (:submodule/_groups group)]
      submodule
      (when-let [parent-group (:group/_children group)]
        (find-parent-submodule db (:db/id parent-group))))))

(defn extract-group-info
  "Extract relevant information from a group entity.
   Returns nil if no valid conditionals remain after processing.

   Arguments:
   - db: Datomic database value
   - group-eid: Group entity ID

   Returns:
   Map with :path, :group/translated-name, :group/research?, :parent-submodule/io,
   order fields, and :conditionals. Returns nil if no valid conditionals.

   Implementation pattern from test_matrix_generator.clj lines 305-328"
  [db group-eid]
  (let [group (pull-group-details db group-eid)
        parent-submodule (find-parent-submodule db group-eid)
        parent-module (:module/_submodules parent-submodule)
        io (:submodule/io parent-submodule)
        ;; Collect full group hierarchy
        group-hierarchy (collect-group-hierarchy db group-eid)
        ;; Build complete path: Module > Submodule > :io > Groups...
        module-name (get-translation db (:module/translation-key parent-module))
        submodule-name (:submodule/name parent-submodule)
        base-path (filterv some? (concat [module-name submodule-name] group-hierarchy))
        ;; Insert :io keyword after module and submodule (if path is long enough)
        full-path (if (> (count base-path) 2)
                    (vec (concat (take 2 base-path) [io] (drop 2 base-path)))
                    base-path)
        conditionals-info (process-group-conditionals db group)]
    ;; Only return group info if it has valid conditionals
    (when conditionals-info
      {:path full-path
       :group/translated-name (get-translation db (:group/translation-key group))
       :group/research? (:group/research? group)
       :group/hidden? (:group/hidden? group)
       :parent-submodule/io io
       :group/order (:group/order group)
       :submodule/order (:submodule/order parent-submodule)
       :conditionals conditionals-info})))

(defn extract-submodule-info
  "Extract relevant information from a submodule entity.
   Returns nil if no valid conditionals remain after processing.

   Arguments:
   - db: Datomic database value
   - submodule-eid: Submodule entity ID

   Returns:
   Map with :path, :submodule/name, :submodule/io, :submodule/research?,
   and :conditionals. Returns nil if no valid conditionals.

   Implementation pattern from test_matrix_generator.clj lines 330-346"
  [db submodule-eid]
  (let [submodule (pull-submodule-details db submodule-eid)
        parent-module (:module/_submodules submodule)
        io (:submodule/io submodule)
        module-name (get-translation db (:module/translation-key parent-module))
        submodule-name (:submodule/name submodule)
        ;; Submodule paths should have :io appended at the end
        full-path [module-name submodule-name io]
        conditionals-info (process-submodule-conditionals db submodule)]
    ;; Only return submodule info if it has valid conditionals
    (when conditionals-info
      {:path full-path
       :submodule/name submodule-name
       :submodule/io io
       :submodule/research? (:submodule/research? submodule)
       :conditionals conditionals-info})))

;; ===========================================================================================================
;; Ancestor Path Collection and Processing (Task 3.4 - 3.7)
;; ===========================================================================================================

(defn collect-parent-groups
  "Given a path like [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"],
   return all parent group paths in order from module/submodule to leaf.

   Skips module (first element) and submodule (second element) in intermediate paths,
   but includes them in final paths for lookup.

   Example input:  [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"]
   Example output: [[\"Surface\" \"Fuel Moisture\"]
                    [\"Surface\" \"Fuel Moisture\" \"By Size Class\"]
                    [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"]]

   Arguments:
   - path: Vector of path elements [Module Submodule Groups...]

   Returns:
   Sequence of parent paths, or nil if path has 2 or fewer elements

   Implementation pattern from test_matrix_generator.clj lines 358-375"
  [path]
  (when (> (count path) 2)
    (let [module (take 1 path)
          rest-path (drop 1 path)
          num-groups (count rest-path)]
      (-> (for [i (range 1 num-groups)]
            (vec (concat module (take i rest-path))))
          rest))))

;; ===========================================================================================================
;; EDN Data Structure Builder (Task 4.4)
;; ===========================================================================================================

(defn generate-edn-data
  "Generate EDN data structure with all conditional information.
   Filters out groups/submodules where conditional resolution failed.
   Returns a map with path vectors as keys.

   Arguments:
   - db: Datomic database value
   - groups: Collection of group entity IDs
   - submodules: Collection of submodule entity IDs

   Returns:
   Map with path vectors as keys, entity info as values
   {['Crown' 'Spot' :input 'Torching Trees'] {:path [...] :conditionals [...] ...}
    ['Crown' 'Spot' :input] {:path [...] :conditionals [...] ...}
    ...}"
  [db groups submodules]
  (let [all-groups (keep #(extract-group-info db %) groups)
        all-submodules (keep #(extract-submodule-info db %) submodules)
        ;; Combine groups and submodules, using :path as key
        all-entities (concat all-groups all-submodules)
        path-map (into {} (map (fn [entity] [(:path entity) entity]) all-entities))]
    path-map))

;; ===========================================================================================================
;; Main Generation Function (Task 4.5)
;; ===========================================================================================================

(defn generate-test-matrix!
  "Generate test_matrix_data.edn from Datomic database.

   Queries the database to find all groups and submodules with conditionals,
   processes them with ancestor enrichment, and writes structured EDN data for
   feature file generation.

   Arguments:
   - db: Datomic database value (from d/db)
   - edn-path (optional): Path to output EDN file
     Default: 'development/test_matrix_data.edn'

   Returns:
   Map with :edn-path, :groups-count, :submodules-count

   Implementation pattern from test_matrix_generator.clj lines 456-472"
  ([db]
   (generate-test-matrix! db "development/test_matrix_data.edn"))
  ([db edn-path]
   (let [groups (find-all-groups-with-conditionals db)
         submodules (find-all-submodules-with-conditionals db)
         edn-data (generate-edn-data db groups submodules)]

     ;; Write EDN data
     (spit edn-path (with-out-str (pprint edn-data)))
     (println (format "✓ EDN data written to: %s" edn-path))

     {:edn-path edn-path
      :groups-count (count groups)
      :submodules-count (count submodules)})))
