(ns behave-cms.domains.subs
  (:require [re-frame.core :as rf]
            [clojure.set :refer [rename-keys]]
            [datascript.core :as d]
            [behave-cms.store :refer [conn]]))

(rf/reg-sub
 :domains
 (fn [_]
   (rf/subscribe [:pull-with-attr :domain/name '[*]]))
 (fn [lists]
   (sort-by :domain/name lists)))

(rf/reg-sub
 :domains/editor
 (fn [_]
   (rf/subscribe [:state [:editors :domain]]))
 identity)

(rf/reg-sub
 :domains/dimension-units
 (fn [[_ dimension-eid]]
   [(rf/subscribe [:pull '[*] dimension-eid])
    (rf/subscribe [:domains/editor])])
 (fn [[domain domain-temp]]
   (let [dimension-uuid (or (:domain/dimension-uuid domain-temp)
                            (:domain/dimension-uuid domain))
         dimension-eid  (rf/subscribe [:query '[:find ?e .
                                                :where [?e :bp/uuid ?uuid]
                                                :in $ ?uuid]
                                       [dimension-uuid]])]
     (-> (rf/subscribe [:pull '[*] @dimension-eid])
         (deref)
         (:dimension/units)))))

(rf/reg-sub
 :domains/dimension-units-2
 (fn [[_ dimension-eid]]
   [(rf/subscribe [:pull '[*] dimension-eid])
    (rf/subscribe [:domains/editor])])
 (fn [[domain domain-temp]]
   (let [dimension-uuid (or (:domain/dimension-uuid domain-temp)
                            (:domain/dimension-uuid domain))
         dimension-eid  (rf/subscribe [:query '[:find ?e .
                                                :where [?e :bp/uuid ?uuid]
                                                :in $ ?uuid]
                                       [dimension-uuid]])]
     (-> (rf/subscribe [:pull '[*] @dimension-eid])
         (deref)
         (:dimension/units)))))


(comment
  (clear! :domains/dimension)
  (rf/subscribe [:domains/editor])
  (rf/subscribe [:domains/dimension-units 5330])


  (defn clear! [k]
    (rf/clear-sub k)
    (rf/clear-subscription-cache!))

  (clear! :domains/editor)

  ;;; Write a Datomic Query with a pull in the :find statement
  [:find (pull ?e [*])
   :where
   [?e :your-entity/attribute some-value]]

  @(rf/subscribe [:query '[:find [(pull ?e [*])]
                           :where [?e :bp/uuid ?uuid]
                           :in $ ?uuid]
                  [dimension-uuid]])

  (def dimension-uuid @(rf/subscribe [:domains/dimension 5330]))
  (rf/subscribe [:pull 
                 '[*]
                 ])

  (rf/subscribe [:query '[:find ?e
                          :where [?e :bp/uuid ?uuid]
                          :in $ ?uuid]
                 [dimension-uuid]])



  (rf/subscribe [:pull '[*] 5330])


  (rf/subscribe [:query '[:find ?a ?v
                          :where [5330 ?a ?v]]])

  []

  )

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
