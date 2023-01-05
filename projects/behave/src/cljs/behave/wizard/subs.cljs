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
    (map (fn [submodule]
           (-> submodule
               (assoc :slug (-> submodule (:submodule/name) (->kebab)))
               (assoc :submodule/groups @(subscribe [:wizard/groups (:db/id submodule)]))))
         submodules)))

(reg-sub
  :wizard/submodules-io-input-only
  (fn [[_ module-id]]
    (subscribe [:wizard/submodules module-id]))

  (fn [submodules _]
    (filter (fn [submodule] (= (:submodule/io submodule) :input)) submodules)))

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
                                                           {:bp/uuid :variable/uuid})]
                            (-> %
                                (dissoc :variable/_group-variables)
                                (merge variable-data)
                                (dissoc :variable/group-variables)
                                (update :variable/kind keyword))) (:group/group-variables group)))) groups)))


(defn- dfs-walk [k->v cur-depth]
  (when-let [map-entries (seq k->v)]
    (if (zero? cur-depth)
      (map val map-entries)
      (into (dfs-walk (-> map-entries first val) (dec cur-depth))
            (dfs-walk (rest map-entries) cur-depth)))))

(reg-sub
 :wizard/multi-value-input-count

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet/all-input-values ws-uuid]))

 (fn [all-input-values _query]
   (count (filter (fn multiple-values? [value]
                    (str/includes? value ","))
                  all-input-values))))

;; TODO Might want to set this in a config file to the application
(def ^:const continuous-input-limit 3)

(reg-sub
  :wizard/multi-value-input-limit
  (fn [_db _query]
    continuous-input-limit))

(reg-sub
  :wizard/warn-limit?
  (fn [_query]
    (subscribe [:wizard/multi-value-input-count]))

  (fn [multi-value-input-count _query]
    (> multi-value-input-count continuous-input-limit)))
