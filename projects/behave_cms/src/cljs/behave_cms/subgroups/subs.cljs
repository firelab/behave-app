(ns behave-cms.subgroups.subs
  (:require [clojure.string    :as str]
            [bidi.bidi         :refer [path-for]]
            [datascript.core   :as d]
            [behave-cms.store  :refer [conn]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

(reg-sub
  :subgroups
  (fn [[_ submodule-id]]
    (subscribe [:pull-children :submodule/groups submodule-id]))

  (fn [result _] result))

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
    (subscribe [:pull-children :group/group-variables group-id '[* {:variable/_group-variables [*]}]]))

  identity)

(reg-sub
  :group/variables
  (fn [[_ group-id]]
    (subscribe [:variables group-id]))

  (fn [variables]
    (map (fn [group-variable]
           (let [variable (get-in group-variable [:variable/_group-variables 0])]
             (merge group-variable variable))) variables)))

(reg-sub
  :sidebar/variables
  (fn [[_ group-id]]
    (subscribe [:variables group-id]))

  (fn [variables]
    (->> variables
         (map (fn [variable]
                (let [id (:db/id variable)
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
    (let [variables (d/datoms @@conn :avet :variable/name)]
      (as-> variables $
        (filter #(match-query? query %) $)
        (take 20 $)
        (map first $)))))

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

  (fn [result _] result))

(reg-sub
  :cpp/enums
  (fn [[_ namespace-id]]
    (subscribe [:pull-children :cpp.namespace/enums namespace-id]))

  (fn [results _]
    (sort-by :cpp.enum/name results)))

(reg-sub
  :cpp/enum-members
  (fn [[_ enum-id]]
    (subscribe [:pull-children :cpp.enum/enum-members enum-id]))

  (fn [results _]
    (sort-by :cpp.enum-member/name results)))

(reg-sub
  :cpp/classes
  (fn [[_ namespace-id]]
    (subscribe [:pull-children :cpp.namespace/classes namespace-id]))

  (fn [results _]
    (sort-by :cpp.class/name results)))

(reg-sub
  :cpp/functions
  (fn [[_ class-id]]
    (subscribe [:pull-children :cpp.class/functions class-id]))

  (fn [results _]
    (sort-by :cpp.function/name results)))

(reg-sub
  :cpp/parameters
  (fn [[_ function-id]]
    (subscribe [:pull-children :cpp.function/parameters function-id]))

  (fn [results _]
    (sort-by :cpp.parameter/name results)))
