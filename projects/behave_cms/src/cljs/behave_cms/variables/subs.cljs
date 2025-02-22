(ns behave-cms.variables.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :variables
  (fn [_]
    (rf/subscribe [:pull-with-attr :variable/name]))
  identity)

(rf/reg-sub
  :dimensions
  (fn [_]
    (rf/subscribe [:pull-with-attr :dimension/name '[* {:dimension/units [*]}]]))
  (fn [dimensions]
    (sort-by :dimension/name dimensions)))

(rf/reg-sub
 :domains
 (fn [_]
   (rf/subscribe [:pull-with-attr :domain/name '[*]]))
 (fn [domains]
   (sort-by :domain/name domains)))
