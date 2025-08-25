(ns behave-cms.group-variables.subs
  (:require [clojure.string     :as str]
            [datascript.core    :as d]
            [behave-cms.store   :refer [conn]]
            [behave-cms.queries :refer [rules]]
            [behave.schema.rules :refer [all-rules]]
            [re-posh.core       :as rp]
            [re-frame.core      :refer [reg-sub subscribe]]))

;;; Links

(rp/reg-query-sub
 :group-variable/_source-links
 '[:find  ?l ?v-name ?gv-dst-id
   :in $ ?gv-src-id
   :where
   [?l :link/source ?gv-src-id]
   [?l :link/destination ?gv-dst-id]
   [?v :variable/group-variables ?gv-dst-id]
   [?v :variable/name ?v-name]])

(reg-sub :group-variable/source-links
         (fn [[_ group-variable-id]]
           (subscribe [:group-variable/_source-links group-variable-id]))
         (fn [links]
           (let [results (mapv #(apply assoc {} (interleave [:db/id :variable/name :group-variable/id] %)) links)]
             (println links results)
             results)))

(rp/reg-query-sub
 :group-variable/_destination-links
 '[:find  ?l ?v-name ?gv-src-id
   :in $ ?gv-dst-id
   :where
   [?l :link/destination ?gv-dst-id]
   [?l :link/source ?gv-src-id]
   [?v :variable/group-variables ?gv-src-id]
   [?v :variable/name ?v-name]])

(reg-sub :group-variable/destination-links
         (fn [[_ group-variable-id]]
           (subscribe [:group-variable/_destination-links group-variable-id]))
         (fn [links]
           (let [results (mapv #(apply assoc {} (interleave [:db/id :variable/name :group-variable/id] %)) links)]
             (println links results)
             results)))

;;; Applications, Modules, Submodules

(reg-sub
 :group-variable/is-output?
 (fn [_ [_ group-variable-id]]
   (d/q '[:find ?is-output .
          :in $ % ?gv
          :where
          [?g :group/group-variables ?gv]
          (submodule-root ?sm ?g)
          [?sm :submodule/io ?io]
          [(= ?io :output) ?is-output]]
        @@conn rules group-variable-id)))

(reg-sub
 :group-variable/kind
 (fn [_ [_ group-variable-uuid]]
   (d/q '[:find ?kind .
          :in $ ?gv-uuid
          :where
          [?gv :bp/uuid ?gv-uuid]
          [?v :variable/group-variables ?gv]
          [?v :variable/kind ?kind]]
        @@conn group-variable-uuid)))

(reg-sub
 :group-variable/module-submodule-group
 (fn [_ [_ group-variable-id]]
   (d/q '[:find [?m ?sm ?g]
          :in $ % ?gv
          :where
          [?g :group/group-variables ?gv]
          (submodule-root ?sm ?g)
          [?m :module/submodules ?sm]]
        @@conn rules group-variable-id)))

(reg-sub
 :group-variable/_app-module-id
 (fn [_ [_ group-variable-id]]
   (d/q '[:find ?a .
          :in $ % ?gv
          :where
          [?g :group/group-variables ?gv]
          (app-root ?a ?g)]
        @@conn rules group-variable-id)))

(reg-sub
 :group-variable/app-modules
 (fn [[_ group-variable-id]]
   (subscribe [:group-variable/_app-module-id group-variable-id]))
 (fn [app-id]
   @(subscribe [:pull-children :application/modules app-id])))

(reg-sub
 :group-variable/_submodule-groups-and-subgroups
 (fn [_ [_ submodule-id]]
   (d/q '[:find [?g ...]
          :in $ % ?sm
          :where (submodule-root ?sm ?g)]
        @@conn rules submodule-id)))

(reg-sub
 :group-variable/submodule-groups-and-subgroups
 (fn [[_ submodule-id]]
   (subscribe [:group-variable/_submodule-groups-and-subgroups submodule-id]))
 (fn [group-ids]
   @(subscribe [:entities group-ids])))

(reg-sub
 :group-variable/output?
 (fn [_ [_ group-variable-id]]
   (d/q '[:find ?is-output .
          :in $ % ?gv
          :where (io ?gv ?io)
          [(= ?io :output) ?is-output]]
        @@conn all-rules group-variable-id)))

(reg-sub
 :group-variable/conditionals
 (fn [[_ gv-id conditionals-attr]]
   (subscribe [:query
               '[:find ?c ?name
                 :in  $ ?s ?conditional-attr
                 :where
                 [?s ?conditional-attr ?c]
                 [?c :conditional/type :group-variable]
                 [?c :conditional/group-variable-uuid ?gv-uuid]
                 [?gv :bp/uuid ?gv-uuid]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]]
               [gv-id conditionals-attr]]))
 (fn [results]
   (mapv (fn [[id name]]
           (-> @(subscribe [:entity id])
               (assoc :variable/name name))) results)))
