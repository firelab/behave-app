(ns find-parent-entities
  (:require [behave-cms.server :as cms]
            [behave-cms.store :refer [default-conn]]
            [schema-migrate.interface :as sm]
            [datomic.api :as d]
            [behave.schema.rules :as rules]))

;; ==================================================================================
;; Group-Variable Hierarchy Function (Using Datomic Rules)
;; =================================================================================
;; Returns a sequence of datomic entities from submodule down to group
;; Uses datomic rules from behave.schema.rules for cleaner, more declarative queries

(defn get-group-variable-hierarchy
  "Returns a sequence of datomic entities from submodule down to group.

  Uses datomic rules from behave.schema.rules for cleaner queries:
  - lookup: Find entity by UUID
  - group-variable: Link group-variable to its parent group
  - submodule-root: Recursively find the submodule for any (sub)group
  - subgroup: Recursively find all ancestor groups

  Returns: [{:db/id submodule} {:db/id parent-1} ... {:db/id immediate-group}]

  Arguments:
    db - Datomic database value
    gv-uuid - UUID of the group-variable

  Returns:
    Sequence of entities (just :db/id) from submodule to immediate group,
    or nil if group-variable not found"
  [db gv-uuid]
  ;; Use rules to find the immediate group and submodule
  (when-let [[submodule-eid immediate-group-eid]
             (d/q '[:find [?submodule ?group]
                    :in $ % ?gv-uuid
                    :where
                    (lookup ?gv-uuid ?gv)
                    (group-variable ?group ?gv ?v)
                    (submodule-root ?submodule ?group)]
                  db
                  rules/all-rules
                  gv-uuid)]

    ;; Use the subgroup rule to find all ancestor groups
    ;; The subgroup rule: (subgroup ?parent ?child) means ?child is a subgroup of ?parent
    (let [ancestor-eids (d/q '[:find [?ancestor ...]
                               :in $ % ?child
                               :where
                               (subgroup ?ancestor ?child)]
                             db
                             rules/all-rules
                             immediate-group-eid)

          ;; Pull all groups with their parent references to sort them
          all-groups          (cons immediate-group-eid ancestor-eids)
          groups-with-parents (map #(d/pull db '[:db/id {:group/_children [:db/id]}] %)
                                   all-groups)

          ;; Build a map of child -> parent for quick lookup
          parent-map (into {} (map (fn [g]
                                     [(:db/id g)
                                      (when-let [parent (:group/_children g)]
                                        (:db/id parent))])
                                   groups-with-parents))

          ;; Sort groups from root to leaf by following parent chain
          sort-groups (fn [group-eid]
                        (loop [current group-eid
                               path    []]
                          (if-let [parent (get parent-map current)]
                            (recur parent (conj path current))
                            (reverse (conj path current)))))

          sorted-group-eids (sort-groups immediate-group-eid)
          submodule         {:db/id submodule-eid}]

      ;; Return: [submodule parent-groups... immediate-group]
      (cons submodule (map #(hash-map :db/id %) sorted-group-eids)))))


(comment
  ;; Initialize database

  (cms/init-db!)

  (def db (d/db (default-conn)))

  ;; Example group-variable UUID (get from translation key)
  (def test-uuid
    (sm/t-key->uuid db "behaveplus:surface:input:wind_speed:wind-adjustment-factor:wind-adjustment-factor---user-input:wind-adjustment-factor"))

  ;; Call the function
  (get-group-variable-hierarchy db test-uuid)
  ;; => (#:db{:id 4611681620380879811} #:db{:id 4611681620380880862} #:db{:id 4611681620380880865})

  ;; To see full entity details:
  (def hierarchy (get-group-variable-hierarchy db test-uuid))

  (map #(d/pull db '[*] (:db/id %)) hierarchy)

  ;; View with specific attributes:
  (map #(d/pull db '[:db/id
                     :group/name :group/translation-key :group/conditionals-operator
                     :submodule/name :submodule/conditionals-operator]
                (:db/id %))
       hierarchy)
  ;; => ({:db/id 4611681620380879811,
  ;;      :submodule/name "Wind and Slope",
  ;;      :submodule/conditionals-operator :or}
  ;;     {:db/id 4611681620380880862,
  ;;      :group/name "Wind Adjustment Factor",
  ;;      :group/translation-key "behaveplus:surface:input:wind_speed:wind-adjustment-factor",
  ;;      :group/conditionals-operator :and}
  ;;     {:db/id 4611681620380880865,
  ;;      :group/name "Wind Adjustment Factor - User Input",
  ;;      :group/translation-key
  ;;      "behaveplus:surface:input:wind_speed:wind-adjustment-factor:wind-adjustment-factor---user-input",
  ;;      :group/conditionals-operator :and})
  )
