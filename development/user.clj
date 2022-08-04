(ns user
  (:require ;[fig-repl :as r]
            [datascript.core :as d]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [behave.schema.core :refer [all-schemas]]))

(comment
  (+ 1 1)

  (def conn (d/create-conn {:worksheet/inputs {:db/valueType   :db.type/ref
                                               :db/cardinality :db.cardinality/many}}))

  (d/transact conn [{:name "Bob" :age 26}])
  (d/transact conn [{:worksheet/name "Test WOrksheet"}])

  (d/transact conn [{:db/id 2 :worksheet/inputs [{:input/value 15 :input/units "m"}]}])

  (d/transact conn [{:db/id 2 :worksheet/inputs [{:input/value 15 :input/units "m"}]}])

  (d/q '[:find ?v ?units :where [?e :input/value ?v][?e :input/units ?units]] @conn)

  (d/q '[:find ?v ?units :where [?e :input/value ?v][?e :input/units ?units]] @conn)

  (d/datoms @conn :eavt)

  #_(b/init!)

  #_(r/start-figwheel!)

  ;; Connect to 1337
  #_(r/start-repl!)

  #_(config/load-config "deps.edn")

  #_(config/get-config :aliases)

  #_(m/new-component "storage")

  #_(m/new-base "behave-routing")
  #_(m/new-project "behave")
  #_(m/new-project "behave-cms")

  #_(load "manage")
  #_(load-file "development/manage.clj")
  )
