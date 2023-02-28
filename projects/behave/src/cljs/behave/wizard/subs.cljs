(ns behave.wizard.subs
  (:require [clojure.string         :as str]
            [clojure.set            :refer [rename-keys]]
            [re-frame.core          :refer [reg-sub subscribe]]
            [string-utils.interface :refer [->kebab]]
            [re-frame.core :as rf]))

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
 :wizard/submodule-name+io
 (fn [_ [_ submodule-uuid]]
   @(subscribe [:vms/query '[:find [?s-name ?io]
                             :in    $ ?uuid
                             :where
                             [?s :bp/uuid ?uuid]
                             [?s :submodule/name ?s-name]
                             [?s :submodule/io ?io]]
                submodule-uuid])))

;; returns a collection of [note-id note-name note-content submodule-name submodule-io]
;; Optionally filter notes using submodule-uuid
(reg-sub
 :wizard/notes

 (fn [[_id ws-uuid]]
   (subscribe [:worksheet/notes ws-uuid]))

 (fn [notes [_ _ws-uuid submodule-uuid]]
   (cond->> notes
     submodule-uuid (filter (fn [[_id _name _content s-uuid]]
                              (= s-uuid submodule-uuid)))
     :always        (map (fn resolve-uuid [[id name content s-uuid]]
                           (into   [id name content]
                                   @(subscribe [:wizard/submodule-name+io s-uuid])))))))

(reg-sub
 :wizard/edit-note?
 (fn [{:keys [state]} [_ note-id]]
   (true? (get-in state [:worksheet :notes note-id :edit?]))))


(reg-sub
 :wizard/show-notes?
 (fn [{:keys [state]} _]
   (true? (get-in state [:worksheet :show-notes?]))))

(reg-sub
 :wizard/show-add-note-form?
 (fn [{:keys [state]} _]
   (true? (get-in state [:worksheet :show-add-note-form?]))))


(reg-sub
 :wizard/first-module+submodule
 (fn [_]
   (subscribe [:worksheet/latest])) ;TODO Get uuid as a param

 (fn [ws-uuid [_ io]]
   (let [worksheet @(subscribe [:worksheet ws-uuid])]
     (when-let [module-kw (first (:worksheet/modules worksheet))]
       (let [module          @(subscribe [:wizard/*module (name module-kw)])
             module-id       (:db/id module)
             submodules      @(subscribe [:wizard/submodules module-id])
             [i-subs o-subs] (->> submodules
                                  (sort-by :submodule/order)
                                  (partition-by #(:submodule/io %)))
             submodules      (if (= io :input) i-subs o-subs)]

         [(name module-kw) (:slug (first submodules))])))))
