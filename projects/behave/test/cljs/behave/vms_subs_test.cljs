(ns behave.vms-subs-test
  (:require [behave.vms.subs :refer [hide-table-filter-entity?]]
            [cljs.test       :refer [deftest is testing]]
            [datascript.core :as d]))

;; A directional parent flagged `hide-table-filter?` plus its Heading/Backing
;; children, and one unflagged non-directional output for contrast.
(def ^:private test-db
  (-> (d/create-conn {:bp/uuid                            {:db/unique :db.unique/identity}
                      :group-variable/direction-variables {:db/valueType   :db.type/ref
                                                           :db/cardinality :db.cardinality/many}})
      (doto (d/transact!
             [{:db/id   -1
               :bp/uuid "crown-length-scorched-heading"}
              {:db/id   -2
               :bp/uuid "crown-length-scorched-backing"}
              {:db/id                              -3
               :bp/uuid                            "crown-length-scorched"
               :group-variable/hide-table-filter?  true
               :group-variable/direction-variables [-1 -2]}
              {:bp/uuid "spread-rate"}]))
      deref))

(defn- entity [gv-uuid]
  (d/entity test-db [:bp/uuid gv-uuid]))

(deftest hide-table-filter-entity?-test
  (testing "a flagged group variable opts out of table shading filters"
    (is (true? (hide-table-filter-entity? (entity "crown-length-scorched")))))

  (testing "direction children inherit the flag from their parent"
    (is (true? (hide-table-filter-entity? (entity "crown-length-scorched-heading"))))
    (is (true? (hide-table-filter-entity? (entity "crown-length-scorched-backing")))))

  (testing "unflagged group variables keep their filter"
    (is (false? (hide-table-filter-entity? (entity "spread-rate")))))

  (testing "an unknown group variable is not flagged"
    (is (false? (hide-table-filter-entity? nil)))))
