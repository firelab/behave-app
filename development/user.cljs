(ns user
  (:require [re-frame.core :as rf]))

(.-location js/window)

(defn clear! [k]
  (rf/clear-sub k)
  (rf/clear-subscription-cache!))

(comment

  (clear! :vms/pull-children)

  (rf/subscribe [:vms/pull-children :module/name])
  (rf/subscribe [:query '[:find ?e ?name
                          :where [?e :submodule/name ?name]]])
  (rf/subscribe [:pull '[* {:submodule/groups [* {:group/group-variables [* {:variable/_group_variables [*]}]}]}] 2727])

  (rf/subscribe [:query '[:find ?e :where [?e :variable/group-variables]]])

  (rf/subscribe [:pull '[* {:variable/_group-variables [*]}] 2908])
  (rf/subscribe [:query '[:find ?e ?key
                          :where [?e :help/key ?key]]])

  (rf/subscribe [:query '[:find ?e ?name
                          :where
                          [?e :variable/native-units "per ac"]
                          [?e :variable/name ?name]]])

  (rf/subscribe [:query '[:find ?e ?name
                          :where
                          [?e :variable/native-units "per ac"]
                          [?e :variable/name ?name]]])

  (rf/subscribe [:pull '[*] 2694])

  (rf/subscribe [:pull '[*] 2684])

  (rf/subscribe [:pull '[*] 273])

  (rf/subscribe [:query '[:find ?e ?fn-name
                          :where [?e :class/name "SIGContainAdapter"]
                                 [?e :class/functions ?f]
                                 [?f :function/name ?fn-name]]])

  (rf/subscribe [:query '[:find ?e ?p-name ?p-order
                          :where [?e :class/name "SIGContainAdapter"]
                                 [?e :class/functions ?f]
                                 [?f :function/name "addResource"]
                                 [?f :function/parameters ?p]
                                 [?p :parameter/name ?p-name]
                                 [?p :parameter/order ?p-order]]])


  (rf/subscribe [:pull '[*] 119])

  (rf/subscribe [:query '[:find ?v ?v-name ?fn-name ?p-name ?p-type
                          :where [?e :module/name "Contain"]
                                 [?e :module/submodules ?s]
                                 [?s :submodule/io :input]
                                 [?s :submodule/name ?s-name]
                                 [?s :submodule/groups ?g]
                                 [?g :group/group-variables ?gv]
                                 [?v :variable/group-variables ?gv]
                                 [?v :variable/name ?v-name]
                                 [?gv :group-variable/cpp-namespace ?ns-id]
                                 [?gv :group-variable/cpp-class ?class-id]
                                 [?gv :group-variable/cpp-function ?fn-id]
                                 [?ns :bp/uuid ?ns-id]
                                 [?ns :namespace/name ?ns-name]
                                 [?class :bp/uuid ?class-id]
                                 [?class :class/name ?class-name]
                                 [?fn :bp/uuid ?fn-id]
                                 [?fn :function/name ?fn-name]
                                 [?fn :function/parameters ?p]
                                 [?p :parameter/name ?p-name]
                                 [?p :parameter/type ?p-type]]])

  (rf/subscribe [:query '[:find ?v ?v-name ?ns-name ?class-name ?fn-name
                          :where [?e :module/name "Contain"]
                                 [?e :module/submodules ?s]
                                 [?s :submodule/io :input]
                                 [?s :submodule/name ?s-name]
                                 [?s :submodule/groups ?g]
                                 [?g :group/group-variables ?gv]
                                 [?v :variable/group-variables ?gv]
                                 [?v :variable/name ?v-name]
                                 [?gv :group-variable/cpp-namespace ?ns-id]
                                 [?gv :group-variable/cpp-class ?class-id]
                                 [?gv :group-variable/cpp-function ?fn-id]
                                 [?ns :bp/uuid ?ns-id]
                                 [?ns :namespace/name ?ns-name]
                                 [?class :bp/uuid ?class-id]
                                 [?class :class/name ?class-name]
                                 [?fn :bp/uuid ?fn-id]
                                 [?fn :function/name ?fn-name]
                                 [?fn :function/parameters ?p]]])


  (rf/subscribe [:query '[:find [?unit ...]
                          :where [?e :variable/metric-units ?unit]]])

  (def contain (first @(rf/subscribe [:query '[:find [?e] :where [?e :module/name "Contain"]]])))


  (rf/subscribe [:query '[:find  ?content
                          :in    $ ?key
                          :where [?e :help/key ?key]
                                 [?e :help/content ?content]]
                 ["behaveplus:contain:help"]])

  (rf/subscribe [:help/content "behaveplus:contain:help"])
  (rf/subscribe [:help/content "behaveplus:contain:input:fire:help"])
  (rf/subscribe [:help/content "behaveplus:contain:input:fire:help"])

)
