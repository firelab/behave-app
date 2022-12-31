(ns behave-cms.submodules.subs
  (:require [bidi.bidi         :refer [path-for]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

(reg-sub
  :submodules
  (fn [[_ module-id]]
    (subscribe [:pull-children :module/submodules module-id]))
  identity)

(reg-sub
  :sidebar/submodules
  (fn [[_ module-id]]
    (subscribe [:submodules module-id]))

  (fn [submodules _]
    (->> submodules
         (map (fn [{id   :db/id
                    s-name :submodule/name
                    io   :submodule/io}]
                {:label (str s-name " (" (name io) ")")
                 :link  (path-for app-routes :get-submodule :id id)}))
         (sort-by :label))))
