(ns behave-cms.lists.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :lists
  (fn [_]
    (rf/subscribe [:pull-with-attr :list/name '[* {:list/options [*]}]]))
  (fn [lists]
    (sort-by :list/name lists)))
