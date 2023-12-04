(ns behave-cms.categories.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :categories
 (fn [_]
   (rf/subscribe [:pull-with-attr :category/name '[*]]))
 (fn [lists]
   (sort-by :category/name lists)))
