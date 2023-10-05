(ns behave-cms.subgroups.subs
  (:require [clojure.string     :as str]
            [bidi.bidi          :refer [path-for]]
            [datascript.core    :as d]
            [behave-cms.store   :refer [conn]]
            [behave-cms.queries :refer [rules]]
            [re-frame.core      :refer [reg-sub subscribe]]
            [behave-cms.routes  :refer [app-routes]]))

;;; Applications, Modules, Submodules

(reg-sub
 :subgroup/_app-module-id
 (fn [_ [_ group-id]]
   (d/q '[:find ?a .
          :in $ % ?s
          :where (app-root ?a ?s)]
        @@conn rules group-id)))

(reg-sub
 :subgroup/app-modules
 (fn [[_ group-id]]
   (subscribe [:subgroup/_app-module-id group-id]))
 (fn [app-id]
   @(subscribe [:pull-children :application/modules app-id])))

;;; Discrete Variables

(reg-sub
 :group/discrete-variables
 (fn [[_ group-id]]
   (subscribe [:query '[:find [?gv ...]
                        :in $ ?g
                        :where
                        [?g :group/group-variables ?gv]
                        [?v :variable/group-variables ?gv]
                        [?v :variable/kind :discrete]]
               [group-id]]))
 (fn [results]
   (->> @(subscribe [:pull-many '[* {:variable/_group-variables [*]}] results])
        (map (fn [group-variable]
               (let [variable (get-in group-variable [:variable/_group-variables 0])
                     variable (dissoc variable :db/id :bp/uuid)]
                 (merge group-variable variable)))))))

(reg-sub
 :group/discrete-variable-options
  (fn [[_ gv-uuid]]
    (subscribe [:query
                   '[:find ?l .
                     :in $ ?gv-uuid
                     :where
                     [?gv :bp/uuid ?gv-uuid]
                     [?v :variable/group-variables ?gv]
                     [?v :variable/list ?l]]
                   [gv-uuid]]))
  (fn [list]
    @(subscribe [:pull-children :list/options list])))

(reg-sub
 :submodule/is-output? 
  (fn [[_ submodule-id]]
    (subscribe [:query
                   '[:find ?io .
                     :in $ ?sm
                     :where
                     [?sm :submodule/io ?io]]
                   [submodule-id]]))
  (fn [io]
    (= io :output)))

;;; Subgroups

(reg-sub
 :subgroups
 (fn [[_ group-id]]
   (subscribe [:pull-children :group/children group-id]))
 identity)

(reg-sub
 :subgroup/parent
 (fn [[_ group-id]]
   (println group-id)
   (subscribe [:query
               '[:find ?g .
                 :in $ ?c
                 :where [?g :group/children ?c]]
               [group-id]]))
 (fn [id]
   (when id @(subscribe [:entity id]))))

(reg-sub
 :group/subgroups
 (fn [[_ group-id]]
   (subscribe [:pull-children :group/children group-id]))
 identity)

(reg-sub
 :sidebar/subgroups
 (fn [[_ group-id]]
   (subscribe [:group/subgroups group-id]))

 (fn [groups _]
   (->> groups
        (map (fn [{id :db/id name :group/name}]
               {:label name
                :link  (path-for app-routes :get-group :id id)}))
        (sort-by :label))))

;;; Conditionals

(reg-sub
 :group/variable-conditionals
 (fn [[_ group-id]]
   (subscribe [:query
               '[:find ?c ?name
                 :in  $ ?g
                 :where
                 [?g :group/conditionals ?c]
                 [?c :conditional/type :group-variable]
                 [?c :conditional/group-variable-uuid ?gv-uuid]
                 [?gv :bp/uuid ?gv-uuid]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]]
               [group-id]]))
 (fn [results]
   (mapv (fn [[id name]]
           (-> @(subscribe [:entity id])
               (assoc :variable/name name))) results)))

(reg-sub
 :group/module-conditionals
 (fn [[_ group-id]]
   (subscribe [:query
               '[:find ?c
                 :in $ ?g
                 :where
                 [?g :group/conditionals ?c]
                 [?c :conditional/type :module]]
               [group-id]]))
 (fn [results]
   (mapv (fn [[id]]
           (-> @(subscribe [:entity id])
               (assoc :variable/name "Modules selected"))) results)))

;;; Variables

(reg-sub
 :group/_variables
 (fn [[_ group-id]]
   (subscribe [:pull-children :group/group-variables group-id '[* {:variable/_group-variables [*]}]]))
 identity)

(reg-sub
 :group/variables
 (fn [[_ group-id]]
   (subscribe [:group/_variables group-id]))

 (fn [variables]
   (map (fn [group-variable]
          (let [variable (get-in group-variable [:variable/_group-variables 0])
                variable (dissoc variable :db/id :bp/uuid)]
            (merge group-variable variable))) variables)))

(reg-sub
 :sidebar/variables
 (fn [[_ group-id]]
   (subscribe [:group/_variables group-id]))

 (fn [variables]
   (->> variables
        (map (fn [variable]
               (let [id   (:db/id variable)
                     name (get-in variable [:variable/_group-variables 0 :variable/name])]
                 {:label name
                  :link  (path-for app-routes :get-group-variable :id id)})))
        (sort-by :label))))

;;; Variables Search

(defn- match-query? [query datom]
  (-> datom
      (nth 2)
      (str/lower-case)
      (str/includes? query)))

(reg-sub
 :group/search-variable-ids
 (fn [_ [_ query]]
   (let [variables (d/datoms @@conn :avet :variable/name)
         query     (str/lower-case (or query ""))]
     (when (seq query)
       (as-> variables $
         (filter #(match-query? query %) $)
         (take 20 $)
         (map first $))))))

(reg-sub
 :group/search-variables
 (fn [[_ query]]
   (subscribe [:group/search-variable-ids query]))

 (fn [eids]
   @(subscribe [:pull-many '[*] (into [] eids)])))

;;; CPP Operations

(reg-sub
 :cpp/namespaces
 (fn [_]
   (subscribe [:pull-with-attr :cpp.namespace/name]))
 identity)

(reg-sub
 :cpp/enums
 (fn [[_ namespace-uuid]]
   (subscribe [:bp/lookup namespace-uuid]))

 (fn [namespace-id]
   @(subscribe [:pull-children :cpp.namespace/enum namespace-id])))

(reg-sub
 :cpp/enum-members
 (fn [[_ enum-uuid]]
   (subscribe [:bp/lookup enum-uuid]))

 (fn [enum-id]
   (sort-by :cpp.enum-member/name @(subscribe [:pull-children :cpp.enum/enum-member enum-id]))))

(reg-sub
 :cpp/classes
 (fn [[_ namespace-uuid]]
   (subscribe [:bp/lookup namespace-uuid]))

 (fn [namespace-id]
   (sort-by :cpp.class/name @(subscribe [:pull-children :cpp.namespace/class namespace-id]))))

(reg-sub
 :cpp/functions
 (fn [[_ class-uuid]]
   (subscribe [:bp/lookup class-uuid]))

 (fn [class-id]
   (sort-by :cpp.function/name @(subscribe [:pull-children :cpp.class/function class-id]))))

(reg-sub
 :cpp/parameters
 (fn [[_ function-uuid]]
   (subscribe [:bp/lookup function-uuid]))

 (fn [function-id]
   (sort-by :cpp.parameter/name @(subscribe [:pull-children :cpp.function/parameter function-id]))))
