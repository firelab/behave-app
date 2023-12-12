(ns behave-cms.domains.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :domains
 (fn [_]
   (rf/subscribe [:pull-with-attr :domain/name '[*]]))
 (fn [lists]
   (sort-by :domain/name lists)))
