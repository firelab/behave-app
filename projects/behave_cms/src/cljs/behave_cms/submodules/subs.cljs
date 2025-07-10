(ns behave-cms.submodules.subs
  (:require [bidi.bidi         :refer [path-for]]
            [re-frame.core     :refer [reg-sub subscribe]]
            [behave-cms.routes :refer [app-routes]]))

(reg-sub
  :submodules
  (fn [[_ module-id]]
    (subscribe [:pull-children :module/submodules module-id]))
  identity)

(reg-sub
  :sidebar/submodules
  (fn [[_ module-id]]
    (subscribe [:submodules module-id]))

  (fn [submodules _]
    (->> submodules
         (map (fn [{nid    :bp/nid
                    s-name :submodule/name
                    io     :submodule/io}]
                {:label (str s-name " (" (name io) ")")
                 :link  (path-for app-routes :get-submodule :nid nid)}))
         (sort-by :label))))


(reg-sub
 :pivot-table/fields
 (fn [[_ pivot-table-id]]
   (subscribe [:query
               '[:find ?c ?name
                 :in  $ ?p
                 :where
                 [?p :pivot-table/columns ?c]
                 [?c :pivot-column/type :field]
                 [?c :pivot-column/group-variable-uuid ?gv-uuid]
                 [?gv :bp/uuid ?gv-uuid]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]]
               [pivot-table-id]]))
 (fn [results]
   (->> results
        (mapv (fn [[eid v-name]]
                (-> @(subscribe [:entity eid])
                    (assoc :variable/name v-name))))
        (sort-by :pivot-column/order))))

(reg-sub
 :pivot-table/values
 (fn [[_ pivot-table-id]]
   (subscribe [:query
               '[:find ?c ?name
                 :in  $ ?p
                 :where
                 [?p :pivot-table/columns ?c]
                 [?c :pivot-column/type :value]
                 [?c :pivot-column/group-variable-uuid ?gv-uuid]
                 [?gv :bp/uuid ?gv-uuid]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]]
               [pivot-table-id]]))
 (fn [results]
   (mapv (fn [[id name]]
           (-> @(subscribe [:entity id])
               (assoc :variable/name name))) results)))


(reg-sub
 :search-table/columns
 (fn [[_ search-table-eid]]
   (subscribe [:query
               '[:find ?c ?name
                 :in  $ ?s
                 :where
                 [?s :search-table/columns ?c]
                 [?c :search-table-column/group-variable ?gv]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]]
               [search-table-eid]]))
 (fn [results]
   (sort-by :search-table-column/order
            (mapv (fn [[id nname]]
                    (-> @(subscribe [:entity id])
                        (assoc :variable/name nname))) results))))

(reg-sub
 :search-table/filters
 (fn [[_ search-table-eid]]
   (subscribe [:query
               '[:find ?c ?name
                 :in  $ ?s
                 :where
                 [?s :search-table/filters ?c]
                 [?c :search-table-filter/group-variable ?gv]
                 [?v :variable/group-variables ?gv]
                 [?v :variable/name ?name]]
               [search-table-eid]]))
 (fn [results]
   (sort-by :search-table-column/order
            (mapv (fn [[id nname]]
                    (-> @(subscribe [:entity id])
                        (assoc :variable/name nname))) results))))
