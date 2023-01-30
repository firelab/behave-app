(ns user
  (:require [re-frame.core :as rf]
            [datascript.core :as d]
            [behave.schema.variable :as variable]))

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

  (let [*ws-uuid (rf/subscribe [:worksheet/latest])]
    (rf/subscribe [:worksheet/get-attr @*ws-uuid :worksheet/run-description]))

  (let [*ws-uuid (rf/subscribe [:worksheet/latest])]
    (rf/dispatch [:worksheet/update-attr @*ws-uuid :worksheet/run-description "blah"]))
  )

(comment
  ;; Use this to test the sub :worksheet/notes with stubbed data
  ;; First you must Use the UI to create a worksheet. Independent Workflow > Surface & Contain
  ;; You can skip all output and input entries. Continue until you see the results page
  ;; Run the following code to input some dummy notes.
  (do
    (ns user)
    (require '[re-frame.core    :as rf])
    (require '[behave.store :as s])
    (require '[datascript.core :as d])

    (def ws-uuid @(rf/subscribe [:worksheet/latest]))

    (def ws-id (first (d/q '[:find [?w]
                             :in    $ ?ws-uuid
                             :where [?w :worksheet/uuid ?ws-uuid]]
                           @@s/conn ws-uuid)))

    (def module @(rf/subscribe [:wizard/*module "contain"]))

    (def submodules @(rf/subscribe [:wizard/submodules (:db/id module)]))

    (def submodule-uuids (map :bp/uuid submodules))

    (letfn [(build-name [submodule]
              (str (:submodule/name submodule) " " (:submodule/io submodule)))]
      (let [content "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo."]
        (d/transact @s/conn [{:db/id           ws-id
                              :worksheet/notes [{:note/submodule (first submodule-uuids)
                                                 :note/name      (build-name (first submodules))
                                                 :note/content   content}
                                                {:note/submodule (second submodule-uuids)
                                                 :note/name      (build-name (second submodules))
                                                 :note/content   content}
                                                {:note/submodule (nth submodule-uuids 2)
                                                 :note/name      (build-name (nth submodules 2))
                                                 :note/content   content}]}])))
    @(rf/subscribe [:wizard/notes ws-uuid])))

(comment
  ;; Use this to test the table tab in the results page
  ;; 1. Create WS
  ;; 2. navigate to settings page. You do not have to enter any output or inputs.
  ;; 3. check "Display Table Results"
  ;; 4. navigate to results page
  ;; 5. run the following code to insert some dummy data:
  ;; 6. Check that you see a table with 2 rows and 5 columns.
  (do
    (ns user)
    (require '[re-frame.core    :as rf])
    (require '[datascript.core :as d])
    (require '[behave.store :as s])

    (def ws-uuid @(rf/subscribe [:worksheet/latest]))

    (def ws-id (:db/id (d/entity @@s/conn [:worksheet/uuid ws-uuid])))


    (def module @(rf/subscribe [:wizard/*module "contain"]))

    (def submodules @(rf/subscribe [:wizard/submodules (:db/id module)]))

    (def submodule-outputs (filter #(= (:submodule/io %) :output) submodules))

    (def submodule-inputs (filter #(= (:submodule/io %) :input) submodules))

    ;; sample input group-variable-uuids to use. See submodule-outputs
    (def output-name->uuid
      {"Fire Perimeter - at resource arrival time" "b7873139-659e-4475-8d41-0cf6c36da893"
       "Fire Area at Initial Attack"               "7eaf10d0-1dae-445d-b8ad-257f431894aa"
       "Time from Report"                          "0e9457cb-33cb-4fce-a4b6-165fa1fd60a5"
       "Contain Status"                            "7fe3de90-6207-4200-9b0c-2112d6e5cf09"})

    ;; sample input group-variable-uuids to use. See submodule-inputs
    (def input-name->uuid
      {"Contain Surface Fire Rate of Spread (maximum)" "fbbf73f6-3a0e-4fdd-b913-dcc50d2db311"
       "Fire Size at Report"                           "30493fc2-a231-41ee-a16a-875f00cf853f"
       "Length-to-Width Ratio"                         "41503286-dfe4-457a-9b68-41832e049cc9"})

    ;; insert table with 4 columns and 1 row of data
    (d/transact @s/conn
                [{:db/id                  ws-id
                  :worksheet/result-table {:db/id                -1
                                           :result-table/headers [{:db/id                             -2
                                                                   :result-header/order               0
                                                                   :result-header/group-variable-uuid (get input-name->uuid "Contain Surface Fire Rate of Spread (maximum)")
                                                                   :result-header/units               "ft"}
                                                                  {:db/id                             -3
                                                                   :result-header/order               1
                                                                   :result-header/group-variable-uuid (get input-name->uuid "Fire Size at Report")
                                                                   :result-header/units               "ft"}
                                                                  {:db/id                             -4
                                                                   :result-header/order               1
                                                                   :result-header/group-variable-uuid (get input-name->uuid "Length-to-Width Ratio")
                                                                   :result-header/units               "ft"}
                                                                  {:db/id                             -5
                                                                   :result-header/order               2
                                                                   :result-header/group-variable-uuid (get output-name->uuid "Fire Perimeter - at resource arrival time")
                                                                   :result-header/units               "ft"}
                                                                  {:db/id                             -6
                                                                   :result-header/order               3
                                                                   :result-header/group-variable-uuid (get output-name->uuid "Fire Area at Initial Attack")
                                                                   :result-header/units               "ft"}]
                                           :result-table/rows    [{:result-row/id    0
                                                                   :result-row/cells [{:result-cell/header -2
                                                                                       :result-cell/value  "100"}
                                                                                      {:result-cell/header -3
                                                                                       :result-cell/value  "101"}
                                                                                      {:result-cell/header -4
                                                                                       :result-cell/value  "102"}
                                                                                      {:result-cell/header -5
                                                                                       :result-cell/value  "103"}
                                                                                      {:result-cell/header -6
                                                                                       :result-cell/value  "104"}]}
                                                                  {:result-row/id    1
                                                                   :result-row/cells [{:result-cell/header -2
                                                                                       :result-cell/value  "105"}
                                                                                      {:result-cell/header -3
                                                                                       :result-cell/value  "106"}
                                                                                      {:result-cell/header -4
                                                                                       :result-cell/value  "107"}
                                                                                      {:result-cell/header -5
                                                                                       :result-cell/value  "108"}
                                                                                      {:result-cell/header -6
                                                                                       :result-cell/value  "109"}]}]}}])))
