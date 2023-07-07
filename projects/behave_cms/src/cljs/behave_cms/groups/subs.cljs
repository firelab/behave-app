(ns behave-cms.groups.subs
  (:require [clojure.string :as str]
            [bidi.bidi :refer [path-for]]
            [datascript.core :as d]
            [behave-cms.store :refer [conn]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

;;; Conditionals

(reg-sub
 :submodule/variable-conditionals
 (fn [[_ submodule-id]]
   (subscribe [:query
               '[:find ?c ?name
                 :in  $ ?s
                 :where
                 [?s :submodule/conditionals ?c]
                 [?c :conditional/type :group-variable]
                 [?c :conditional/group-variable-uuid ?gv-uuid]
                 [?gv :bp/uuid ?gv-uuid]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]]
               [submodule-id]]))
 (fn [results]
   (mapv (fn [[id name]]
           (-> @(subscribe [:entity id])
               (assoc :variable/name name))) results)))

(reg-sub
 :submodule/module-conditionals
 (fn [[_ submodule-id]]
   (subscribe [:query
               '[:find ?c
                 :in $ ?s
                 :where
                 [?s :submodule/conditionals ?c]
                 [?c :conditional/type :module]]
               [submodule-id]]))
 (fn [results]
   (mapv (fn [[id]]
           (-> @(subscribe [:entity id])
               (assoc :variable/name "Modules selected"))) results)))

;;; Groups

(reg-sub
  :groups
  (fn [[_ submodule-id]]
    (subscribe [:pull-children :submodule/groups submodule-id]))
  identity)

;;; Groups w/ Subgroups

(defn accumulate-subgroups [acc group & [parents]]
  (let [id       (:db/id group)
        children (:group/children group)
        name     (:group/name group)
        acc      (conj acc {:db/id      id
                            :group/name (if (seq parents)
                                          (str/join " / " (conj parents name))
                                          name)})
        parents  (if (nil? parents) [name] (conj parents name))]
    (if (seq children)
      (reduce (fn [acc cur]
                (accumulate-subgroups acc cur parents))
              acc
              (:group/children (d/pull @@conn '[{:group/children [*]}] id)))
      acc)))

(reg-sub
  :submodule/groups-w-subgroups
  (fn [[_ submodule-id]]
    (subscribe [:pull-children :submodule/groups submodule-id]))
  (fn [groups]
    (reduce
     accumulate-subgroups
     []
     groups)))

;;; Sidebar

(reg-sub
  :sidebar/groups
  (fn [[_ submodule-id]]
    (subscribe [:groups submodule-id]))

  (fn [groups]
    (->> groups
         (map (fn [{id :db/id name :group/name}]
                {:label name
                 :link  (path-for app-routes :get-group :id id)}))
         (sort-by :label))))
