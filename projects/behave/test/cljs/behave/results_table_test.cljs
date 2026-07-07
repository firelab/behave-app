(ns behave.results-table-test
  (:require [behave.components.results.table :as sut]
            [cljs.test                       :refer [deftest is testing]]))

;; A directional output (spread rate) split into Heading/Backing/Flanking
;; children, plus a non-directional output (reaction intensity).
(def directional-uuids #{"heading-sr" "backing-sr" "flanking-sr"})
(def heading-uuids     #{"heading-sr"})

(deftest shades-row?-heading-rule-test
  (testing "non-directional columns always strike their run's row"
    (is (true? (sut/shades-row? directional-uuids heading-uuids "rxn-intensity"))))

  (testing "the Heading directional column strikes the row"
    (is (true? (sut/shades-row? directional-uuids heading-uuids "heading-sr"))))

  (testing "Backing/Flanking columns never strike the row"
    (is (false? (sut/shades-row? directional-uuids heading-uuids "backing-sr")))
    (is (false? (sut/shades-row? directional-uuids heading-uuids "flanking-sr"))))

  (testing "with no Heading children, only non-directional columns strike"
    (is (true?  (sut/shades-row? directional-uuids #{} "rxn-intensity")))
    (is (false? (sut/shades-row? directional-uuids #{} "heading-sr")))))
