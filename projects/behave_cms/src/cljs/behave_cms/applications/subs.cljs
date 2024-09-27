(ns behave-cms.applications.subs
  (:require [bidi.bidi :refer [path-for]]
            [behave-cms.store   :refer [conn]]
            [datascript.core    :as d]
            [re-frame.core :as rf]
            [string-utils.interface :refer [->kebab]]
            [behave-cms.routes :refer [app-routes]]))

(rf/reg-sub
  :applications
  (fn [_]
    (rf/subscribe [:pull-with-attr :application/name]))
  identity)

(rf/reg-sub
  :application
  (fn [[_ id]]
    (rf/subscribe [:entity id]))
  (fn [result _]
    (let [app-name        (:application/name result)
          translation-key (->kebab app-name)
          help-key        (str translation-key ":help")]
      (assoc result
             :application/help-key help-key
             :application/translation-key translation-key))))

(rf/reg-sub
  :sidebar/applications
  :<- [:applications]
  (fn [applications _]
    (->> applications
         (map (fn [{nid :bp/nid app-name :application/name}]
                {:label app-name
                 :link  (path-for app-routes :get-application :nid nid)}))
         (sort-by :label))))

;;; Modules

(rf/reg-sub
 :application/modules
 (fn [[_ application-id]]
   (rf/subscribe [:pull-children :application/modules application-id]))
 identity)

;;; Tools

(rf/reg-sub
 :application/tools
 (fn [[_ application-id]]
   (rf/subscribe [:pull-children :application/tools application-id]))
 identity)

;; Group Variable Order Overrides
(rf/reg-sub
 :application/group-variable-order-overrides

 (fn [[_ application-id]]
   (rf/subscribe [:pull-children :application/group-variable-order-overrides application-id]))

 (fn [group-variable-order-overrides _]
   (map
    (fn [{gv  :group-variable-order-override/group-variable
          :as group-variable-order-override-entity}]
      (let [gv-entity     (d/entity @@conn (:db/id gv))
            variable-name (->> gv-entity
                               :variable/_group-variables
                               first
                               :variable/name)]
        (merge group-variable-order-override-entity
               {:variable/name variable-name})))
    group-variable-order-overrides)))

(rf/reg-sub
 :application/group-variable-order-overrides-count
 (fn [_ [_ app-id]]
   (count (:application/group-variable-order-overrides (d/entity @@conn app-id)))))
