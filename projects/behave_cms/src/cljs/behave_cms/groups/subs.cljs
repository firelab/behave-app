(ns behave-cms.groups.subs
  (:require [bidi.bidi :refer [path-for]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

(reg-sub
  :groups
  (fn [[_ submodule-id]]
    (subscribe [:pull-children :submodule/groups submodule-id]))

  (fn [result _]
    result))

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
