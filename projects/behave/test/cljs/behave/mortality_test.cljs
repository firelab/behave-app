(ns behave.mortality-test
  (:require [behave.lib.enums                :as enums]
            [behave.lib.mortality            :as mortality]
            [behave.lib.species-master-table :as species-master-table]
            [clojure.string                  :as str]
            [csv-parser.interface            :refer [parse-csv]]
            [cljs.test                       :refer [is deftest testing]])
  (:require-macros [behave.macros :refer [inline-resource]]))

(defonce failing-tests (atom ["line,expected,observed,difference"]))

(defn within? [precision a b]
  (> precision (Math/abs (- a b))))

(def within-four-percent? (partial within? 4.0))

(defn- clean-values [row]
  (into {}
        (map (fn remove-quotes[[key val]]
               (if (string? val)
                 [key (str/replace val "\"" "")]
                 [key val])))
        row))

(def equation-type-lookup
  {"CRNSCH" "crown_scorch"
   "CRCABE" "crown_damage"
   "BOLCHR" "bole_char"})

(defn not-blank? [s]
  (not (str/blank? s)))

(defn- run-test [row-idx row]
  (let[module        (mortality/init (species-master-table/init))
       equation-type (if (get row "EquationType")
                       (->> (get row "EquationType")
                            (get equation-type-lookup)
                            enums/equation-type)
                       -1)
       species-code  (get row "TreeSpecies")]

    (mortality/setRegion module (enums/region-code "south_east"))

    (mortality/setEquationType module  equation-type)

    (mortality/setSpeciesCode module species-code)

    (mortality/updateInputsForSpeciesCodeAndEquationType module species-code equation-type)

    (let [FS        (get row "FS")
          FlLe-ScHt (get row "FlLe/ScHt")]
      (cond
        (empty? FS)
        (mortality/setSurfaceFireFlameLength module 4 (enums/length-units "Feet"))

        (and (= FS "S") (not-blank? FlLe-ScHt))
        (mortality/setSurfaceFireScorchHeight module FlLe-ScHt (enums/length-units "Feet"))

        (not-blank? FlLe-ScHt)
        (mortality/setSurfaceFireFlameLength module FlLe-ScHt (enums/length-units "Feet"))))

    (let [TreeExpansionFactor (get row "TreeExpansionFactor")]
      (when (not-blank? TreeExpansionFactor)
        (mortality/setTreeDensityPerUnitArea module TreeExpansionFactor (enums/area-units "Acres"))))

    (let [Diameter (get row "Diameter")]
      (when (not-blank? Diameter)
        (mortality/setDBH module Diameter (enums/length-units "Inches"))))

    (let [TreeHeight (get row "TreeHeight")]
      (when (not-blank? TreeHeight)
        (mortality/setTreeHeight module TreeHeight (enums/length-units "Feet"))))

    (let [CrownRatio (get row "CrownRatio")]
      (when (not-blank? CrownRatio)
        (mortality/setCrownRatio module (/ CrownRatio 100))))

    (let [CrownScorch (get row "CrownScorch%")]
      (when (not-blank? CrownScorch)
        (mortality/setCrownDamage module CrownScorch)))

    (let [CKR (get row "CKR")]
      (when (not-blank? CKR)
        (mortality/setCambiumKillRating module CKR)))

    (let [BeetleDamage (get row "BeetleDamage")]
      (when (not-blank? BeetleDamage)
        (mortality/setBeetleDamage module (enums/beetle-damage (str/lower-case BeetleDamage)))))

    (let [BoleCharHeight (get row "BoleCharHeight")]
      (when (not-blank? BoleCharHeight)
        (mortality/setBoleCharHeight module BoleCharHeight (enums/length-units "Feet"))))

    (let [line-number (+ row-idx 2)]
      (testing (str "csv line #:" line-number)
        (let [expected (get row "MortAvgPercent")
              observed (mortality/calculateMortality module (enums/probability-units "Percent"))]
          (when (not (within-four-percent? expected observed))
            (swap! failing-tests
                   conj
                   (str/join "," [line-number expected observed (Math/abs (- expected observed))])))

          (is (within-four-percent? expected observed)
              (str "Expected: " expected "  Observed: " observed)))))))

(deftest mortality-test
  (let [rows (->> (inline-resource "public/csv/mortality.csv")
                  (parse-csv)
                  (map clean-values))]
    (doall
     (map-indexed (fn [idx row-data]
                    (run-test idx row-data))
                  rows))
    #_(download (str/join "\n" @failing-tests)
                "tests-errors.csv"
                "application/text")))
