(ns behave.tool.subs
  (:require [behave.vms.store       :as s]
            [clojure.set            :refer [rename-keys]]
            [datascript.core        :as d]
            [re-frame.core          :refer [reg-sub subscribe] :as rf]))

(reg-sub
 :tool/show-tool-selector?
 (fn [db _]
   (let [state (get-in db [:state :sidebar :*tools-or-settings])]
     (= state :tools))))

(reg-sub
 :tool/all-tools
 (fn [_ _]
   (let [eids (d/q '[:find [?e ...]
                     :in $
                     :where
                     [?e :tool/name ?name]]
                    @@s/vms-conn)]
     (map #(d/entity @@s/vms-conn %) eids))))

(reg-sub
 :tool/selected-tool-uuid
 (fn [db _]
   (get-in db [:state :tool :selected-tool])))

(reg-sub
 :tool/selected-subtool-uuid
 (fn [db _]
   (get-in db [:state :tool :selected-subtool])))

(reg-sub
 :tool/entity
 (fn [_ [_ tool-uuid]]
   (d/entity @@s/vms-conn [:bp/uuid tool-uuid])))

(reg-sub
 :subtool/input-variables
 (fn [_ [_ subtool-uuid]]
   (let [subtool (d/pull @@s/vms-conn '[* {:subtool/input-variables
                                           [* {:variable/_subtool-variables
                                               [* {:variable/list [* {:list/options [*]}]}]}]}]
                         [:bp/uuid subtool-uuid])]
     (->> (mapv #(let [variable-data (rename-keys (first (:variable/_subtool-variables %))
                                                  {:bp/uuid :variable/uuid})]
                   (-> %
                       (dissoc :variable/_subtool-variables)
                       (merge variable-data)
                       (dissoc :variable/subtool-variables)
                       (update :variable/kind keyword)))
                (:subtool/input-variables subtool))
          (sort-by :subtool-variable/order)))))

(reg-sub
 :subtool/output-variables
 (fn [_ [_ subtool-uuid]]
   (let [subtool (d/pull @@s/vms-conn '[* {:subtool/output-variables
                                           [* {:variable/_subtool-variables
                                               [* {:variable/list [* {:list/options [*]}]}]}]}]
                         [:bp/uuid subtool-uuid])]
     (->> (mapv #(let [variable-data (rename-keys (first (:variable/_subtool-variables %))
                                                  {:bp/uuid :variable/uuid})]
                   (-> %
                       (dissoc :variable/_subtool-variables)
                       (merge variable-data)
                       (dissoc :variable/subtool-variables)
                       (update :variable/kind keyword)))
                (:subtool/output-variables subtool))
          (sort-by :subtool-variable/order)))))

(reg-sub
 :tool/input-value
 (fn [[_ tool-uuid subtool-uuid subtool-variable-uuid]]
   (rf/subscribe [:state [:tool
                          :data
                          tool-uuid
                          subtool-uuid
                          :inputs
                          subtool-variable-uuid]]))

 (fn [value _]
   value))
