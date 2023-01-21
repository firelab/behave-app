(ns behave-cms.modules.subs
  (:require [clojure.string         :as str]
            [bidi.bidi              :refer [path-for]]
            [re-frame.core          :refer [reg-sub subscribe] :as rf]
            [string-utils.interface :refer [kebab-key]]
            [behave-cms.routes      :refer [app-routes]]))

(reg-sub
 :modules
 (fn [[_ application-id]]
   (subscribe [:pull-children :application/module application-id]))
 identity)

(reg-sub
 :module
 (fn [[_ id]]
   (subscribe [:entity id '[* {:application/_module [*]}]]))

 (fn [result _]
   (let [application-name (-> result
                              (:application/_module)
                              (first)
                              (:application/name))
         module-name      (:module/name result)
         translation-key  (kebab-key application-name module-name)
         help-key         (str translation-key ":help")]
     (assoc result
            :module/translation-key translation-key
            :module/help-key help-key))))

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
