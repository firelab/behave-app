(ns behave-cms.groups.subs
  (:require [bidi.bidi :refer [path-for]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

;;; Conditionals

(reg-sub
 :submodule/variable-conditionals
 (fn [[_ submodule-id]]
   (subscribe [:query
               '[:find ?c ?name
                 :in  $ ?s
                 :where
                 [?s :submodule/conditionals ?c]
                 [?c :conditional/group-variable-uuid ?gv-uuid]
                 [?gv :bp/uuid ?gv-uuid]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]]
               [submodule-id]]))
 (fn [results]
   (mapv (fn [[id name]]
           (-> @(subscribe [:entity id])
               (assoc :variable/name name))) results)))

(reg-sub
 :submodule/module-conditionals
 (fn [[_ submodule-id]]
   (subscribe [:query
               '[:find ?c
                 :in $ ?s
                 :where
                 [?s :submodule/conditionals ?c]]
               [submodule-id]]))
 (fn [results]
   (mapv (fn [[id]]
           (-> @(subscribe [:entity id])
               (assoc :variable/name "Modules selected"))) results)))

;;; Groups

(reg-sub
  :groups
  (fn [[_ submodule-id]]
    (subscribe [:pull-children :submodule/groups submodule-id]))
  identity)

;;; Sidebar

(reg-sub
  :sidebar/groups
  (fn [[_ submodule-id]]
    (subscribe [:groups submodule-id]))

  (fn [groups]
    (->> groups
         (map (fn [{id :db/id name :group/name}]
                {:label name
                 :link  (path-for app-routes :get-group :id id)}))
         (sort-by :label))))
