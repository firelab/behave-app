(ns behave-cms.subgroups.subs
  (:require [clojure.string    :as str]
            [bidi.bidi         :refer [path-for]]
            [datascript.core   :as d]
            [behave-cms.store  :refer [conn]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

(reg-sub
 :subgroups
 (fn [[_ group-id]]
   (subscribe [:pull-children :group/children group-id]))
 identity)

(reg-sub
 :sidebar/subgroups
 (fn [[_ group-id]]
   (subscribe [:groups group-id]))

 (fn [groups _]
   (->> groups
        (map (fn [{id :db/id name :group/name}]
               {:label name
                :link  (path-for app-routes :get-group :id id)}))
        (sort-by :label))))

(reg-sub
 :variables
 (fn [[_ group-id]]
   (subscribe [:pull-children :group/group-variable group-id '[* {:variable/_group-variable [*]}]]))

 identity)

(reg-sub
 :group/variables
 (fn [[_ group-id]]
   (subscribe [:variables group-id]))

 (fn [variables]
   (map (fn [group-variable]
          (let [variable (get-in group-variable [:variable/_group-variable 0])
                variable (dissoc variable :db/id :bp/uuid)]
            (merge group-variable variable))) variables)))

(reg-sub
 :sidebar/variables
 (fn [[_ group-id]]
   (subscribe [:variables group-id]))

 (fn [variables]
   (->> variables
        (map (fn [variable]
               (let [id   (:db/id variable)
                     name (get-in variable [:variable/_group-variable 0 :variable/name])]
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
