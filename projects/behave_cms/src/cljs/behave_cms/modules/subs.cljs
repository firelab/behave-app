(ns behave-cms.modules.subs
  (:require [datascript.core    :as d]
            [behave-cms.store   :refer [conn]]
            [behave-cms.queries :refer [rules]]
            [re-frame.core      :refer [reg-sub subscribe]]))

(reg-sub
 :module/_app-module-id
 (fn [_ [_ group-id]]
   (d/q '[:find ?a .
          :in $ % ?s
          :where (app-root ?a ?m)]
        @@conn rules group-id)))

(reg-sub
 :module/app-modules
 (fn [[_ module-id]]
   (subscribe [:module/_app-module-id module-id]))
 (fn [app-id]
   @(subscribe [:pull-children :application/modules app-id])))

