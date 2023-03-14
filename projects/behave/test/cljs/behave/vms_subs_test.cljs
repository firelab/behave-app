(ns behave.vms-sub-test
  (:require
   [cljs.test :refer [use-fixtures deftest is join-fixtures] :include-macros true]
   [re-frame.core :as rf]
   [behave.fixtures :refer [setup-empty-db teardown-db]]))


;; =================================================================================================
;; Test utils and fixtures
;; =================================================================================================

(use-fixtures :each
  {:before (join-fixtures [setup-empty-db])
   :after  (join-fixtures [teardown-db])})

;; =================================================================================================
;; Tests
;; =================================================================================================

(deftest vms-entity-test
  (let [module @(rf/subscribe [:vms/entity-from-uuid "21ddce87-0809-4f9d-b2a7-9ef2d4881a3f"])]
    (is (some? module))))

(deftest vms-pull-with-attr-test
  (let [module @(rf/subscribe [:vms/pull-with-attr :module/name])]
    (is (some? module))))
