(ns behave-cms.variables.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :variables
  (fn [_]
    (rf/subscribe [:pull-with-attr :variable/name]))
  identity)
