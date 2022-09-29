(ns behave.wizard.subs
  (:require [clojure.string         :as str]
            [clojure.set            :refer [rename-keys]]
            [re-frame.core          :refer [reg-sub subscribe]]
            [string-utils.interface :refer [->kebab]]))

;;; Helpers

(defn- matching-submodule? [io slug submodule]
  (and (= io (:submodule/io submodule))
       (= slug (:slug submodule))))

;;; Subscriptions

(reg-sub
  :wizard/*module
  (fn [_]
    (subscribe [:vms/pull-with-attr :module/name]))
  (fn [modules [_ selected-module]]
    (first (filter (fn [{m-name :module/name}]
              (= selected-module (str/lower-case m-name))) modules))))

(reg-sub
  :wizard/submodules
  (fn [[_ module-id]]
    (subscribe [:vms/pull-children
                :module/submodules
                module-id]))
  (fn [submodules _]
    (map #(assoc % :slug (-> % (:submodule/name) (->kebab))) submodules)))

(reg-sub
  :wizard/*submodule

  (fn [[_ module-id _ _]]
    (subscribe [:wizard/submodules module-id]))

  (fn [submodules [_ _ slug io]]
    (let [[inputs outputs] (partition-by :submodules/io submodules)]
      (or (first (filter (partial matching-submodule? io slug) submodules))
          (first (if (= :input io) inputs outputs))))))

(reg-sub
  :wizard/groups
  (fn [[_ submodule-id]]
    (subscribe [:vms/pull-children
                   :submodule/groups
                   submodule-id
                   '[* {:group/group-variables [* {:variable/_group-variables [*]}]}]]))

  (fn [groups]
    (mapv (fn [group]
            (assoc group
                   :group/group-variables
                   (mapv #(let [variable-data (rename-keys (first (:variable/_group-variables %))
                                                           {:db/id :variable/id})]
                            (-> %
                                (dissoc :variable/_group-variables)
                                (merge variable-data)
                                (dissoc :variable/group-variables)
                                (update :variable/kind keyword))) (:group/group-variables group)))) groups)))
