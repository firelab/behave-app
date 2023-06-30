(ns behave.wizard.subs
  (:require [clojure.string         :as str]
            [clojure.set            :refer [rename-keys]]
            [re-frame.core          :refer [reg-sub subscribe] :as rf]
            [string-utils.interface :refer [->kebab]]
            [datascript.core :as d]
            [behave.vms.store :refer [vms-conn]]))

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
  :wizard/submodules-io-output-only
  (fn [[_ module-id]]
    (subscribe [:wizard/submodules module-id]))

  (fn [submodules _]
    (filter (fn [submodule] (= (:submodule/io submodule) :output)) submodules)))

(reg-sub
  :wizard/*submodule

  (fn [[_ module-id _ _]]
    (subscribe [:wizard/submodules module-id]))

  (fn [submodules [_ _ slug io]]
    (let [[inputs outputs] (partition-by :submodules/io submodules)]
      (or (first (filter (partial matching-submodule? io slug) submodules))
          (first (if (= :input io) inputs outputs))))))

(defn edit-groups [group]
  (when group
    (cond-> group
      (seq (:group/group-variables group))
      (assoc :group/group-variables
             (mapv #(let [variable-data (rename-keys (first (:variable/_group-variables %))
                                                     {:bp/uuid :variable/uuid})]
                      (-> %
                          (dissoc :variable/_group-variables)
                          (merge variable-data)
                          (dissoc :variable/group-variables)
                          (update :variable/kind keyword)))
                   (:group/group-variables group)))

      (seq (:group/children group))
      (assoc :group/children
             (map edit-groups (:group/children group))))))

(reg-sub
 :wizard/groups
 (fn [[_ submodule-id]]
   (subscribe [:vms/pull-children
               :submodule/groups
               submodule-id
               '[* {:group/group-variables [* {:variable/_group-variables [*]}]}
                 {:group/children 6}]])) ;; recursively apply pattern up to 6 levels deep

 (fn [groups]
   (mapv edit-groups groups)))

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
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet [_ _ws-uuid submodule-uuid]]
   (let [notes (:worksheet/notes worksheet)]
     (cond->> notes
       submodule-uuid (filter (fn [{s-uuid :note/submodule}]
                                (= s-uuid submodule-uuid)))
       :always        (map (fn resolve-uuid [{id      :db/id
                                              name    :note/name
                                              content :note/content
                                              s-uuid  :note/submodule}]
                             (into   [id name content]
                                     @(subscribe [:wizard/submodule-name+io s-uuid]))))))))

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
 :wizard/results-tab-selected
 (fn [_ _]
  (subscribe [:state [:worksheet :results :tab-selected]]))
 (fn [tab-selected _]
   tab-selected))

(reg-sub
 :wizard/worksheet-date

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet _]
   (let [created-date (:worksheet/created worksheet)
         d            (js/Date.)]
     (.setTime d created-date)
     (.toLocaleDateString d))))

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

(def str->bool
  {"true"  true
   "false" false})

(reg-sub
 :wizard/resolve-conditional
 (fn [[_ _ conditional-id]]
   (subscribe [:vms/entity-from-eid conditional-id]))

 (fn [{gv-uuid          :conditional/group-variable-uuid
       op               :conditional/operator
       condition-values :conditional/values
       :as              conditional}
      [_ ws-uuid _conditional-id]]
   (let [value @(subscribe [:worksheet/output-enabled? ws-uuid gv-uuid])]
     (println "conditional:" (into {} conditional))
     (case op
       :equal     (= (first condition-values) (str value))
       :not-equal (not= (first condition-values) (str value))
       :in        (contains? condition-values (str value))))))

(reg-sub
 :wizard/show-group?
 (fn [[_ _ws-uuid group-id]]
   (subscribe [:vms/entity-from-eid group-id]))

 (fn [group [_ ws-uuid _group-id]]
   (println "==========================")
   (println "show-group? processing:" (:group/name group))
   (println "group:" (into {} group))
   (println "group-conditonals:" (:group/conditionals group))
   (if-let [conditional-ids (->> (:group/conditionals group)
                                 (map :db/id)
                                 seq)]
     (let [resolved-conditions (map #(deref (subscribe [:wizard/resolve-conditional ws-uuid %]))
                                    conditional-ids)]
       (if (= (:group/conditionals-operator group) :or)
         (some true? resolved-conditions)
         (every? true? resolved-conditions)))
     true)))

(comment
  ;; Debug why there's a ghost conditonal on this group

  ;; OUTPUT Fire Size - at resource arrival time
  (def group-uuid "5e66e41a-236a-41c7-98d7-54edd15659a9")

  (def group-entity @(subscribe [:vms/entity-from-uuid group-uuid]))

  (:group/name group-entity)
  ;;=> Fire Size - at resource arrival time

  ;; This should be 0 entries but theres two.
  (map #(into {} %) (:group/conditionals group-entity))

  )
