(ns behave.tool.subs
  (:require [behave.vms.store       :as s]
            [clojure.set            :refer [rename-keys]]
            [datascript.core        :as d]
            [re-frame.core          :refer [reg-sub path] :as rf]))

(reg-sub
 :tool/show-tool-selector?
 (fn [db [_ io]]
   (when (= io :input)
     (let [state (get-in db [:state :sidebar :*tools-or-settings])]
       (= state :tools)))))

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

(defn- enrich-subtool-variable [subtool-variable]
  (let [variable-data (rename-keys (first (:variable/_subtool-variables subtool-variable))
                                   {:bp/uuid :variable/uuid})]
    (-> subtool-variable
        (dissoc :variable/_subtool-variables)
        (merge variable-data)
        (dissoc :variable/subtool-variables)
        (update :variable/kind keyword))))

(reg-sub
 :subtool/encriched-subtool-variables
 (fn [_ [_ subtool-uuid]]
   (let [subtool (d/pull @@s/vms-conn '[* {:subtool/variables
                                           [* {:variable/_subtool-variables
                                               [* {:variable/list [* {:list/options [*]}]}]}]}]
                         [:bp/uuid subtool-uuid])]
     (->> (:subtool/variables subtool)
          (mapv enrich-subtool-variable)
          (sort-by :subtool-variable/order)))))

(reg-sub
 :subtool/input-variables
 (fn [[_ subtool-eid]]
   (rf/subscribe [:subtool/encriched-subtool-variables subtool-eid]))
 (fn [variables _]
   (filter #(= (:subtool-variable/io %) :input) variables)))

(reg-sub
 :subtool/output-variables
 (fn [[_ subtool-eid]]
   (rf/subscribe [:subtool/encriched-subtool-variables subtool-eid]))
 (fn [variables _]
   (filter #(= (:subtool-variable/io %) :output) variables)))

(reg-sub
 :tool/input-value
 (fn [db [_ tool-uuid subtool-uuid subtool-variable-uuid]]
   (get-in db [:state
               :tool
               :data
               tool-uuid
               subtool-uuid
               :tool/inputs
               subtool-variable-uuid
               :input/value])))

(reg-sub
 :tool/input-units
 (fn [db [_ tool-uuid subtool-uuid subtool-variable-uuid]]
   (get-in db [:state
               :tool
               :data
               tool-uuid
               subtool-uuid
               :tool/inputs
               subtool-variable-uuid
               :input/units-uuid])))

(reg-sub
 :tool/output-value
 (fn [db [_ tool-uuid subtool-uuid subtool-variable-uuid]]
   (get-in db [:state
               :tool
               :data
               tool-uuid
               subtool-uuid
               :tool/outputs
               subtool-variable-uuid])))

(reg-sub
 :tool/all-inputs
 (fn [db [_ tool-uuid subtool-uuid]]
   (get-in db [:state
               :tool
               :data
               tool-uuid
               subtool-uuid
               :tool/inputs])))

(reg-sub
 :tool/all-output-uuids
 (fn [_ [_ subtool-uuid]]
   (d/q '[:find [?output-uuids ...]
          :in $ ?uuid
          :where
          [?s  :bp/uuid ?uuid]
          [?s  :subtool/variables ?sv]
          [?sv :subtool-variable/io :output]
          [?sv :bp/uuid ?output-uuids]]
        @@s/vms-conn subtool-uuid)))

(comment
  (rf/subscribe [:tool/all-inputs
                 "64e5023e-1b70-4b55-8312-6578fc9c64a9"
                 "64e5024c-1841-4fd6-ba62-e6f983608793"])

  (rf/subscribe [:tool/all-output-uuids "64e5024c-1841-4fd6-ba62-e6f983608793"]))
