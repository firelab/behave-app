(ns behave.help.subs
  (:require [re-frame.core :as rf]
            [re-posh.core :as rp]))

(rf/reg-sub
  :help/current-tab
  (fn [db _]
    (get-in db [:state :help-tab] :help)))

(rp/reg-sub
  :help/content
  (fn [_ [_ help-key]]
    {:type :query
     :query '[:find [?content]
              :in $ ?help-key
              :where [?e :help/key ?help-key]
                     [?e :help/content ?content]]
     :variables [help-key]}))

(rf/reg-sub
  :help/submodule-help-keys
  (fn [[_ submodule-id]]
    (rf/subscribe [:pull
                   '[:submodule/help-key
                     {:submodule/groups
                      [:group/help-key
                       {:group/group-variables
                        [:group-variable/help-key]}]}]
                   submodule-id]))

  (fn [submodule]
    (persistent!
      (reduce (fn [acc group]
                (conj! acc [(:group/help-key group)
                            (mapv :group-variable/help-key (:group/group-variables group))]))
              (transient [(:submodule/help-key submodule)])
              (:submodule/groups submodule)))))
