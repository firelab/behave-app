(ns behave-cms.domains.subs
  (:require [re-frame.core :as rf]
            [clojure.set :refer [rename-keys]]))

(rf/reg-sub
 :domains
 (fn [_]
   (rf/subscribe [:pull-with-attr :domain/name '[*]]))
 (fn [lists]
   (sort-by :domain/name lists)))

(rf/reg-sub
 :domain-sets
 (fn [_]
   (rf/subscribe [:pull-with-attr :domain-set/name '[*]]))
 (fn [lists]
   (sort-by :domain-set/name lists)))

(rf/reg-sub
 :domains/options
 (fn [[_ name-attr]]
   (rf/subscribe [:pull-with-attr name-attr]))

 (fn [datoms [_ name-attr]]
   (->> datoms
        (map #(-> %
                  (select-keys [name-attr :bp/uuid])
                  (rename-keys {name-attr :label
                                :bp/uuid  :value})))
        (sort-by :label))))
