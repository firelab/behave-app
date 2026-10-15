(ns behave.diagram.subs
  (:require [behave.diagram.core :as core]
            [re-frame.core       :as rf]))

(rf/reg-sub
 :diagram/visible-series-map
 (fn [db _]
   (get-in db core/db-path {})))

(rf/reg-sub
 :diagram/visible-series
 :<- [:diagram/visible-series-map]
 ;; nil = this diagram has never been toggled; see
 ;; [[behave.diagram.core/resolve-visible]].
 (fn [m [_ k]]
   (get m k)))
