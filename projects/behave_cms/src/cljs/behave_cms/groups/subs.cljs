(ns behave-cms.groups.subs
  (:require [bidi.bidi :refer [path-for]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

;;; Conditionals

(reg-sub
  :submodule/variable-conditionals
  (fn [[_ submodule-id]]
   (subscribe [:query
               '[:find ?c ?gv-uuid ?name ?operator ?values
                 :in  $ ?s
                 :where
                 [?s :submodule/conditionals ?c]
                 [?c :conditional/group-variable-uuid ?gv-uuid]
                 [?gv :bp/uuid ?gv-uuid]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]
                 [?c :conditional/operator ?operator]
                 [?c :conditional/values ?values]]
               [submodule-id]]))
  (fn [results]
   (mapv (fn [[id gv-uuid name operator values]]
           {:db/id                           id
            :variable/name                   name
            :conditional/group-variable-uuid gv-uuid
            :conditional/operator            operator
            :conditional/values              values}) results)))

(reg-sub
  :submodule/module-conditionals
  (fn [[_ submodule-id]]
   (subscribe [:query
               '[:find ?c ?uuid ?name ?operator
                 :in  $ ?s
                 :where
                 [?s :submodule/conditionals ?c]
                 [?c :conditional/module-uuids ?uuid]
                 [?m :bp/uuid ?uuid]
                 [?m :module/name ?name]
                 [?c :conditional/operator ?operator]]
               [submodule-id]]))
  (fn [results]
   (mapv (fn [[id uuid name operator values]]
           {:db/id                   id
            :module/name             name
            :conditional/module-uuid uuid
            :conditional/operator    operator}) results)))

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
