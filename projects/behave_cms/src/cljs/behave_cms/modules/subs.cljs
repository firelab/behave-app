(ns behave-cms.modules.subs
  (:require [bidi.bidi         :refer [path-for]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

(reg-sub
  :modules
  (fn [[_ application-id]]
    (subscribe [:pull-children :application/modules application-id]))

  (fn [result _]
    result))

(reg-sub
  :sidebar/modules
  (fn [[_ id]]
    (subscribe [:modules id]))

  (fn [modules]
    (->> modules
         (map (fn [{id :db/id module-name :module/name}]
                {:label module-name
                 :link  (path-for app-routes :get-module :id id)}))
         (sort-by :label))))
