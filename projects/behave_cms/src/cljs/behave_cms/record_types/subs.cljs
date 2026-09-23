(ns behave-cms.record-types.subs
  (:require [re-frame.core :as rf]))

;;; Record Types

(rf/reg-sub
 :application/record-types
 (fn [[_ application-id]]
   (rf/subscribe [:pull-children :application/record-types application-id]))
 identity)

;;; Record Fields

(rf/reg-sub
 :record-type/fields
 (fn [[_ record-type-id]]
   (rf/subscribe [:pull-children :record-type/fields record-type-id]))
 identity)
