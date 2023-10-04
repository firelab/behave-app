(ns behave.test-solver-queries
  (:require [cljs.test            :refer [deftest is join-fixtures testing use-fixtures are] :include-macros true]
            [behave.solver.queries :refer [q-vms]]
            [behave.fixtures :refer [setup-empty-db teardown-db with-new-worksheet]]
            [behave.store :as store]
            [behave.vms.store :refer [vms-conn]]
            [datascript.core :as d]
            [behave.schema.core :refer [rules]]
            [re-frame.core :as rf]))

(use-fixtures :each {:before (join-fixtures [setup-empty-db with-new-worksheet])
                     :after  teardown-db})

(deftest testing-q-vms
  (testing "Can query both VMS and Worksheet"

    (let [query '[:find ?ws ?app-name
                  :in ?ws-uuid ?lower-case-fn
                  :where
                  [$ws ?ws :worksheet/uuid ?ws-uuid]
                  [$ws ?ws :worksheet/modules ?ws-module-kw]
                  [(name ?ws-module-kw) ?ws-module-name]

                  [?m :module/name ?module-name]
                  [(?lower-case-fn ?module-name) ?module-lower-name]
                  [(= ?module-lower-name ?ws-module-name)]
                  [?a :application/modules ?m]
                  [?a :application/name ?app-name]]
          observed (first (q-vms query "test-ws-uuid" clojure.string/lower-case))
          expected [1 "BehavePlus"]]

      (println [:QVMS observed expected])

      (is (= observed expected)))))
