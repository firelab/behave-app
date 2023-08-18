(ns behave-cms.tools.subs
  (:require [re-frame.core     :refer [reg-sub subscribe]]))

(reg-sub
  :tool/subtools
  (fn [[_ tool-id]]
    (subscribe [:pull-children :tool/subtools tool-id]))
  identity)
