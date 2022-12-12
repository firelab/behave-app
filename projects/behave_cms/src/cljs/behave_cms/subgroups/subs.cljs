(ns behave-cms.subgroups.subs
  (:require [bidi.bidi :refer [path-for]]
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
    (subscribe [:pull-children :group/variables group-id]))

  (fn [result _] result))

(reg-sub
  :sidebar/variables
  (fn [[_ group-id]]
    (subscribe [:subgroups group-id]))

  (fn [variables]
    (->> variables
         (map (fn [{id :db/id name :variable/name}]
                {:label name
                 :link  (path-for app-routes :get-group-variable :id id)}))
         (sort-by :label))))

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
