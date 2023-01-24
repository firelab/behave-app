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

(reg-sub
 :wizard/multi-value-input-count

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet/all-input-values ws-uuid]))

 (fn [all-input-values _query]
   (count
    (filter (fn multiple-values? [value]
              (> (count (str/split value #",|\s"))
                 1))
            all-input-values))))

(reg-sub
  :wizard/group-variable
  (fn [[_ gv-uuid]]
    (subscribe [:vms/pull '[* {:variable/_group-variables [:variable/name]}] [:bp/uuid gv-uuid]]))

  (fn [group-variable _query]
    (let [variable-data (rename-keys (first (:variable/_group-variables group-variable))
                                     {:db/id :variable/id})]
      (-> group-variable
          (dissoc :variable/_group-variables)
          (merge variable-data)
          (dissoc :variable/group-variables)
          (update :variable/kind keyword)))))

;; TODO Might want to set this in a config file to the application
(def ^:const multi-value-input-limit 3)

(reg-sub
  :wizard/multi-value-input-limit
  (fn [_db _query]
    multi-value-input-limit))

(reg-sub
  :wizard/warn-limit?
  (fn [[_id ws-uuid]]
    (subscribe [:wizard/multi-value-input-count ws-uuid]))

  (fn [multi-value-input-count _query]
    (> multi-value-input-count multi-value-input-limit)))

(reg-sub
 :wizard/module+io+group-name
 (fn [_ [_ group-uuid]]
   @(subscribe [:vms/query '[:find [?m-name ?io ?uuid ?g-name]
                             :in    $ ?uuid
                             :where
                             [?g :bp/uuid ?uuid]
                             [?g :group/name ?g-name]
                             [?s :submodule/groups ?g]
                             [?s :submodule/io ?io]
                             [?m :module/submodules ?s]
                             [?m :module/name ?m-name]]
                group-uuid])))

;; returns a collection of [module-name io group-name note-content]
(reg-sub
 :wizard/notes
 (fn [[_id ws-uuid]]
   (subscribe [:worksheet/notes ws-uuid]))

 (fn [notes _query]
   (map (fn [[group-uuid content]]
          (conj @(subscribe [:wizard/module+io+group-name group-uuid]) content))
        notes)))

(reg-sub
 :wizard/edit-note?
 (fn [{:keys [state]} [_ group-uuid]]
   (true? (get-in state [:worksheet :notes group-uuid :edit?]))))
