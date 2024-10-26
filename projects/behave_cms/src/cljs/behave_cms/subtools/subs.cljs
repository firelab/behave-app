(ns behave-cms.subtools.subs
  (:require [re-frame.core :refer [reg-sub subscribe] :as rf]))

(reg-sub
 :subtool/variables

 (fn [[_ subtool-nid]]
   (subscribe [:pull-children :subtool/variables [:bp/nid subtool-nid] '[* {:variable/_subtool-variables [*]}]]))

 (fn [variables]
   (->> (map (fn [sv] (let [variable (get-in sv [:variable/_subtool-variables 0])]
                        (merge sv (dissoc variable :db/id :bp/nid))))
             variables)
        (sort-by :subtool-variable/order))))

(reg-sub
 :subtool/input-variables

 (fn [[_ subtool-nid]]
   (subscribe [:subtool/variables subtool-nid]))

 (fn [variables _]
   (filter #(= (:subtool-variable/io %) :input) variables)))

(reg-sub
 :subtool/output-variables

 (fn [[_ subtool-nid]]
   (subscribe [:subtool/variables subtool-nid]))

 (fn [variables _]
   (filter #(= (:subtool-variable/io %) :output) variables)))

;;; Tools

(reg-sub
 :tools

 (fn [[_ application-id]]
   (subscribe [:pull-children :application/tools application-id]))
 identity)
