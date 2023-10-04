(ns behave-cms.units.subs
  (:require [re-frame.core :as rf]
            [clojure.set :refer [rename-keys]]))

(rf/reg-sub
 :units/enum-options
 (fn [_ _]
   (rf/subscribe [:pull-with-attr :cpp.enum/name]))

 (fn [enums]
   (sort-by :label (map #(-> %
                             (select-keys [:cpp.enum/name :bp/uuid])
                             (rename-keys {:cpp.enum/name :label
                                           :bp/uuid       :value}))
                        enums))))

(rf/reg-sub
 :units/enum-member-options
 (fn [[_ dimension-eid]]
   (rf/subscribe
    [:query
     '[:find ?member-uuid ?member-name
       :in $ ?d
       :where
       [?d :dimension/cpp-enum-uuid ?enum-uuid]
       [?e :bp/uuid ?enum-uuid]
       [?e :cpp.enum/enum-member ?m]
       [?m :bp/uuid ?member-uuid]
       [?m :cpp.enum-member/name ?member-name]]
     [dimension-eid]]))

 (fn [enum-members]
   (sort-by :label (map (fn [[value label]] {:label label :value value}) enum-members))))
