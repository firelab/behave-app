(ns behave.diagram-state-test
  (:require [behave.diagram.core :as sut]
            [behave.diagram.events]
            [behave.diagram.subs]
            [behave.events]
            [cljs.test           :refer [deftest is testing] :include-macros true]
            [day8.re-frame.test  :as rf-test]
            [re-frame.core       :as rf]
            [re-frame.db         :as rf-db]))

;; The legend order the diagram's color scale produces.
(def ^:private domain ["Heading Fire" "Flanking Fire" "Backing Fire"])

(deftest diagram-key-test
  (testing "the same diagram yields an equal key"
    (is (= (sut/diagram-key "ws" 0 "gv") (sut/diagram-key "ws" 0 "gv"))))

  (testing "row, worksheet and group-variable all distinguish diagrams"
    (is (not= (sut/diagram-key "ws" 0 "gv") (sut/diagram-key "ws" 1 "gv")))
    (is (not= (sut/diagram-key "ws" 0 "gv") (sut/diagram-key "ws2" 0 "gv")))
    (is (not= (sut/diagram-key "ws" 0 "gv") (sut/diagram-key "ws" 0 "gv2")))))

(deftest resolve-visible-test
  (testing "never toggled (nil) falls back to the default-hidden set, in legend order"
    (is (= ["Heading Fire" "Backing Fire"]
           (sut/resolve-visible domain nil #{"Flanking Fire"}))))

  (testing "never toggled with no default-hidden set shows everything"
    (is (= domain (sut/resolve-visible domain nil nil)))
    (is (= domain (sut/resolve-visible domain nil #{}))))

  (testing "an explicit set wins over the default-hidden set, and keeps legend order"
    (is (= ["Heading Fire" "Flanking Fire"]
           (sut/resolve-visible domain #{"Flanking Fire" "Heading Fire"} #{"Flanking Fire"}))))

  (testing "an explicit empty set means hide everything, not fall back"
    (is (= [] (sut/resolve-visible domain #{} #{"Flanking Fire"}))))

  (testing "labels no longer in the legend are dropped"
    (is (= ["Heading Fire"]
           (sut/resolve-visible domain #{"Heading Fire" "Renamed Series"} nil)))))

(deftest set-visible-series-test
  (rf-test/run-test-sync
   (let [k  (sut/diagram-key "ws" 0 "gv")
         *v (rf/subscribe [:diagram/visible-series k])]

     (testing "an untouched diagram has no tracked state"
       (is (nil? @*v)))

     (testing "a toggle is tracked"
       (rf/dispatch [:diagram/set-visible-series k #{"Heading Fire"}])
       (is (= #{"Heading Fire"} @*v)))

     (testing "hiding everything is tracked as an explicit empty set, not as nil"
       (rf/dispatch [:diagram/set-visible-series k #{}])
       (is (= #{} @*v)))

     (testing "another diagram is unaffected"
       (is (nil? @(rf/subscribe [:diagram/visible-series (sut/diagram-key "ws" 1 "gv")]))))

     (testing "re-setting the same value is a no-op (the Vega echo guard)"
       (let [db-before @rf-db/app-db]
         (rf/dispatch [:diagram/set-visible-series k #{}])
         (is (identical? db-before @rf-db/app-db)))))))

(deftest clear-worksheet-test
  (let [m {["ws" 0 "gv"]  #{"Heading Fire"}
           ["ws" 1 "gv"]  #{}
           ["ws2" 0 "gv"] #{"Backing Fire"}}]

    (testing "only the given worksheet's entries are dropped"
      (is (= {["ws2" 0 "gv"] #{"Backing Fire"}}
             (sut/clear-worksheet m "ws"))))

    (testing "an explicit empty set is dropped like any other entry"
      (is (not (contains? (sut/clear-worksheet m "ws") ["ws" 1 "gv"]))))

    (testing "a worksheet with no entries leaves the map alone"
      (is (= m (sut/clear-worksheet m "ws3"))))

    (testing "nil and empty both give an empty map"
      (is (= {} (sut/clear-worksheet nil "ws")))
      (is (= {} (sut/clear-worksheet {} "ws"))))

    (testing "a malformed key is kept rather than throwing"
      (is (= {:junk #{"x"}} (sut/clear-worksheet {:junk #{"x"}} "ws"))))))

(deftest clear-visible-series-test
  (rf-test/run-test-sync
   (let [k    (sut/diagram-key "ws" 0 "gv")
         k2   (sut/diagram-key "ws" 1 "gv")
         kept (sut/diagram-key "ws2" 0 "gv")
         *v   (rf/subscribe [:diagram/visible-series k])
         *v2  (rf/subscribe [:diagram/visible-series k2])
         *ok  (rf/subscribe [:diagram/visible-series kept])]

     (rf/dispatch [:diagram/set-visible-series k #{"Heading Fire"}])
     (rf/dispatch [:diagram/set-visible-series k2 #{}])
     (rf/dispatch [:diagram/set-visible-series kept #{"Backing Fire"}])

     (testing "a re-run drops every entry for that worksheet"
       (rf/dispatch [:diagram/clear-visible-series "ws"])
       (is (nil? @*v))
       (is (nil? @*v2)))

     (testing "another worksheet's entries survive"
       (is (= #{"Backing Fire"} @*ok)))

     (testing "clearing a worksheet with nothing stored is harmless"
       (rf/dispatch [:diagram/clear-visible-series "ws"])
       (is (nil? @*v))
       (is (= #{"Backing Fire"} @*ok))))))
