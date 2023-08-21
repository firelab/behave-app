(ns behave.help.subs
  (:require [re-frame.core :as rf]
            [behave.vms.store       :as s]
            [datascript.core        :as d]))

(rf/reg-sub
 :help/current-tab
 (fn [db _]
   (get-in db [:state :help-tab])))

(rf/reg-sub
 :help/current-highlighted-key
 (fn [db _]
   (get-in db [:state :help-current-highlighted-key])))

(rf/reg-sub
 :help/content
 (fn [_ [_ help-key]]
   @(rf/subscribe [:vms/query
                   '[:find  [?content]
                     :in    $ ?help-key
                     :where [?e :help-page/key ?help-key]
                     [?e :help-page/content ?content]]
                   help-key])))

(defn- flatten-help-keys [acc g-or-gv]
  (let [acc (conj acc (or (:group/help-key g-or-gv)
                          (:group-variable/help-key g-or-gv)))]
    (cond-> acc
      (seq (:group/group-variables g-or-gv))
      (concat (map (partial flatten-help-keys []) (sort-by :group-variables/order (:group/group-variables g-or-gv))))

      (seq (:group/children g-or-gv))
      (concat (map (partial flatten-help-keys []) (sort-by :group/order (:group/children g-or-gv)))))))

(rf/reg-sub
 :help/_group+subgroup+variable-help-keys
 (fn [[_ submodule-id]]
   (rf/subscribe [:vms/pull-children :submodule/groups submodule-id
                  '[:group/help-key :group/order {:group/group-variables [:group-variable/help-key :group-variable/order]} {:group/children 6}]]))
 (fn [groups]
   (vec (flatten (concat (map (partial flatten-help-keys []) (sort-by :group/order groups)))))))

(rf/reg-sub
 :help/submodule-help-keys
 (fn [[_ submodule-id]]
   [(rf/subscribe [:vms/pull '[:submodule/help-key] submodule-id])
    (rf/subscribe [:help/_group+subgroup+variable-help-keys submodule-id])])
 (fn [[submodule g-gv-help-keys]]
   (concat [(:submodule/help-key submodule)] g-gv-help-keys)))

(rf/reg-sub
 :help/tool-help-keys
 (fn [[_ tool-uuid subtool-uuid]]
   [(rf/subscribe [:vms/entity-from-uuid tool-uuid])
    (rf/subscribe [:vms/entity-from-uuid subtool-uuid])])

 (fn [[tool subtool] _]
   (concat [(:tool/help-key tool)
            (:subtool/help-key subtool)]
           (map :subtool-variable/help-key
                (sort-by :subtool-variable/order (:subtool/input-variables subtool)))
           (map :subtool-variable/help-key
                (sort-by :subtool-variable/order (:subtool/output-variables subtool))))))
