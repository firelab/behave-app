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

;; Prioritized Results
(rf/reg-sub
 :application/prioritized-results

 (fn [[_ application-id]]
   (rf/subscribe [:pull-children :application/prioritized-results application-id]))

 (fn [prioritized-results _]
   (map
    (fn [{gv  :prioritized-results/group-variable
          :as prioritized-results-entity}]
      (let [gv-entity     (d/entity @@conn (:db/id gv))
            variable-name (->> gv-entity
                               :variable/_group-variables
                               first
                               :variable/name)]
        (merge prioritized-results-entity
               {:variable/name variable-name})))
    prioritized-results)))

(rf/reg-sub
 :application/prioritized-results-count
 (fn [_ [_ app-id]]
   (count (:application/prioritized-results (d/entity @@conn app-id)))))
