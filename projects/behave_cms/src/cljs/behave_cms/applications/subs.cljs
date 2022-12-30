(ns behave-cms.applications.subs
  (:require [bidi.bidi :refer [path-for]]
            [re-frame.core :as rf]
            [string-utils.interface :refer [->kebab]]
            [behave-cms.routes :refer [app-routes]]))

(rf/reg-sub
  :applications
  (fn [_]
    (rf/subscribe [:pull-with-attr :application/name]))
  (fn [result _] result))

(rf/reg-sub
  :application
  (fn [[_ id]]
    (rf/subscribe [:entity id]))
  (fn [result _]
    (let [name            (:application/name result)
          translation-key (->kebab name)
          help-key        (str translation-key ":help")]
      (assoc result
             :application/help-key help-key
             :application/translation-key translation-key))))

(rf/reg-sub
  :sidebar/applications
  :<- [:applications]
  (fn [applications _]
    (->> applications
         (map (fn [{id :db/id app-name :application/name}]
                {:label app-name
                 :link  (path-for app-routes :get-application :id id)}))
         (sort-by :label))))
