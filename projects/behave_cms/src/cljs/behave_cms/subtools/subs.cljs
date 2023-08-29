(ns behave-cms.subtools.subs
  (:require [re-frame.core :refer [reg-sub subscribe] :as rf]))

(reg-sub
 :subtool/variables
 (fn [[_ variables-attr subtool-eid]]
   (subscribe [:pull-children variables-attr subtool-eid '[* {:variable/_subtool-variables [*]}]]))
 (fn [variables]
   (map (fn [sv] (let [variable (get-in sv [:variable/_subtool-variables 0])]
                   (-> sv
                       (dissoc :variable/_subtool-variables)
                       (merge (dissoc variable :db/id)))))
        variables)))

(reg-sub
 :subtool/input-variables
 (fn [[_ subtool-eid]]
   (subscribe [:subtool/variables :subtool/input-variables subtool-eid]))
 identity)

(reg-sub
 :subtool/output-variables
 (fn [[_ subtool-eid]]
   (subscribe [:subtool/variables :subtool/output-variables subtool-eid]))
 identity)

;;; Tools

(reg-sub
 :tools
 (fn [[_ application-id]]
   (subscribe [:pull-children :application/tools application-id]))
 identity)

