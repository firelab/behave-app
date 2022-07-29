(ns user
  (:require [datascript.core :as d]
            [re-frame.core :as rf]
            [re-posh.core :as rp]
            [re-frisk.core :as re-frisk]))

(re-frisk/enable)

(comment

  (require '[re-frame.core :as rf])
  (rf/dispatch [:initialize])
  (rf/dispatch [:navigate "/worksheets/1/modules/contain/output/fire"])
  (rf/dispatch [:navigate "/worksheets/1/modules/contain/input/fire"])
  (rf/dispatch [:navigate "/worksheets/1/review"])
  (rf/dispatch [:navigate "/worksheets/1/results"])
  (rf/dispatch [:navigate "/worksheets/1/results/derp"])
  (rf/dispatch [:navigate "/settings/depr"])
  (rf/dispatch [:navigate "/tools/derp"])

  @(rf/subscribe [:route])
  (rf/subscribe [:handler])

  (require '[behave-routing.main :refer [routes]])
  (require '[bidi.bidi :refer [match-route]])

  (match-route routes @(rf/subscribe [:route]))

  (rf/subscribe [:entity 78 '[* {:application/modules [:module/name]}]])

  (rf/subscribe [:applications])
  (rf/subscribe [:modules 78])
  (rf/subscribe [:submodules 65])

  (rf/subscribe [:module-ids 78])

  (rf/subscribe [:ids-with-attr :application/name])

  (rf/subscribe [:pull-with-attr :application/name])

  (rf/subscribe [:children-ids :application/modules 78])

  (rf/subscribe [:pull-children :application/modules 78])


  (rf/dispatch [:api/delete-entity {:db/id 82}])

  (rf/subscribe [:pull-children :module/submodules 65])

  (rf/subscribe [:pull-many '[{:module/_submodules [*]}] (map first @(rf/subscribe [:ids-with-attr :submodule/name]))])

  (rf/subscribe [:sidebar/modules 78])

  (d/transact @store/conn [{:db/id 78
                            :application/modules {:db/id -1
                                                  :module/name "Mortality"
                                                  :module/order 1}}])

  (rf/subscribe [:pull '[*] 65])

  (d/transact @store/conn [{:db/id 65
                            :module/submodules [{:db/id -1
                                                 :submodule/name "Fire"
                                                 :submodule/io   :input}]}])

  (coll? [1])

  (rf/dispatch [:transact [{:db/id -1
                            :module/_submodules 65
                            :submodule/name "Supression"
                            :submodule/io   :input}]])

  (rf/dispatch [:transact [{:db/id -1
                            :variable/name "Fire Spread Rate"}]])


  (rf/subscribe [:pull '[*] 78])

  (rf/subscribe [:pull-many '[{:application/_modules [*]}] [64 65]])

  (rf/subscribe [:query '[:find ?e ?name :where [?e :user/first-name ?name]]])

  (sort-by :application/name @(rf/subscribe [:applications]))


  (def modules @(rf/subscribe [:pull-children :application/modules 78]))

  (map (juxt :module/name :module/order) (sort-by :module/order modules))

  ;; all-entities entity order-field direction
  (def contain (first @(rf/subscribe [:query '[:find [?e] :where [?e :module/name "Contain"]]])))
contain
(rf/dispatch [:api/update-entity {:db/id contain :module/order 0}])

contain

(rf/dispatch [:reorder modules contain :module/order :up])
;; :tx-data [[67 :module/order 3 536870953 false] [67 :module/order 2 536870953 true] [65 :module/order 2 536870953 false] [65 :module/order 3 536870953 true]]}

(d/conn? @store/conn)

(d/schema @@store/conn)

(d/squuid)

)
