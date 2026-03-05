(ns behave-cms.components.conditionals.subs
  (:require [re-frame.core      :refer [reg-sub subscribe]]
            [datascript.core :as d]
            [behave-cms.store   :refer [conn]]))

(reg-sub
 :conditionals/all-conditionals
 (fn [_ [_ eid cond-attr]]
   (let [group-variable-conditionals (d/q '[:find ?c ?name
                                            :in  $ ?s ?conditional-attr
                                            :where
                                            [?s ?conditional-attr ?c]
                                            [?c :conditional/type :group-variable]
                                            [?c :conditional/group-variable-uuid ?gv-uuid]
                                            [?gv :bp/uuid ?gv-uuid]
                                            [?v :variable/group-variables ?gv]
                                            [?v :variable/name ?name]]
                                          @@conn
                                          eid
                                          cond-attr)
         module-conditionals         (d/q '[:find ?c
                                            :in $ ?g ?conditional-attr
                                            :where
                                            [?g ?conditionals-attr ?c]
                                            [?c :conditional/type :module]]
                                          @@conn
                                          eid
                                          cond-attr)]
     (concat (mapv (fn [[id name]]
                     (-> @(subscribe [:entity id])
                         (assoc :variable/name name))) group-variable-conditionals)
             (mapv (fn [[id]]
                     (-> @(subscribe [:entity id])
                         (assoc :variable/name "Modules selected"))) module-conditionals)))))
