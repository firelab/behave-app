(ns behave.vms.subs
  (:require [re-frame.core    :refer [reg-sub subscribe]]
            [behave.vms.store :refer [pull pull-many q]]))

(reg-sub
 :vms/query
 (fn [_ [_ query variables]]
   @(q query variables)))

(reg-sub
 :vms/pull
 (fn [_ [_ pattern id]]
   @(pull pattern id)))

(reg-sub
 :vms/pull-many
 (fn [_ [_ pattern ids]]
   @(pull-many pattern ids)))

(reg-sub
 :vms/ids-with-attr
 (fn [_ [_ attr]]
   @(q '[:find  ?e
         :in    $ ?attr
         :where [?e ?attr]]
       attr)))

(reg-sub
 :vms/pull-with-attr
 (fn [[_ attr _]]
   (subscribe [:vms/ids-with-attr attr]))

 (fn [eids [_ _ pattern]]
   @(pull-many (or pattern '[*])
               (reduce into [] eids))))

(reg-sub
 :vms/children-ids
 (fn [_ [_ child-attr eid]]
   @(q '[:find  ?children
         :in    $ ?child-attr ?e
         :where [?e ?child-attr ?children]]
       child-attr eid)))

(reg-sub
 :vms/pull-children
 (fn [[_ child-attr id _]]
   (subscribe [:vms/children-ids child-attr id]))

 (fn [eids [_ _ _ pattern]]
   @(pull-many (or pattern '[*]) (reduce into [] eids))))
