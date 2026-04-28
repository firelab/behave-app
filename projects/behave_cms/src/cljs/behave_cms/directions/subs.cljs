(ns behave-cms.directions.subs
  (:require [behave-cms.subs]
            [re-frame.core :as rf]))

(rf/reg-sub
 :directions
 (fn [_] (rf/subscribe [:pull-with-attr :direction/id]))
 (fn [entities _]
   (sort-by :direction/order entities)))
