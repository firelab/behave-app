(ns behave.mortality-test
  (:require [behave.lib.enums                :as enums]
            [behave.lib.mortality            :as mortality]
            [behave.lib.species-master-table :as species-master-table]
            [clojure.string                  :as str]
            [csv-parser.interface            :refer [parse-csv]]
            [cljs.test                       :refer [is deftest testing]])
  (:require-macros [behave.macros :refer [inline-resource]]))

(defn download
  "Function to download data to a file"
  [data filename type]
  (let [body (.-body js/document)
        file (js/Blob. #js [data] #js {:type type})
        a    (.createElement js/document "a")
        url  (.createObjectURL js/URL file)]
    (set! (.-href a) url)
    (set! (.-download a) filename)
    (.appendChild body a)
    (.click a)
    (js/setTimeout
     #(do (.removeChild body a)
          (js/URL.revokeObjectURL url)) 0)))

(defonce failing-tests (atom ["line,expected,observed,difference"]))

(defn within? [precision a b]
  (> precision (Math/abs (- a b))))

(def within-a-percent? (partial within? 1.0))

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
       species-code  (get row "TreeSpecies")
       FS            (if (= (get row "FS") "S")
                       (enums/flame-length-or-scorch-height-switch "scorch_height")
                       (enums/flame-length-or-scorch-height-switch "flame_length"))]

    ;; (println "TreeSpecies: " (get row "TreeSpecies"))
    ;; (println "TreeExpansionFactor: " (get row "TreeExpansionFactor"))
    ;; (println "Diameter: " (get row "Diameter"))
    ;; (println "TreeHeight: " (get row "TreeHeight"))
    ;; (println "CrownRatio: " (get row "CrownRatio"))
    ;; (println "EquationType: " (->> (get row "EquationType")
    ;;                                (get equation-type-lookup)))
    ;; (println "FS:" FS)


    (mortality/setRegion module (enums/region-code "south_east"))

    (mortality/setEquationType module  equation-type)

    (mortality/setSpeciesCode module species-code)

    (mortality/updateInputsForSpeciesCodeAndEquationType module species-code equation-type)

    (mortality/setFlameLengthOrScorchHeightSwitch module FS)

    (let [FS (get row "FS")]
      (when (empty? FS)
        (mortality/setFlameLengthOrScorchHeightValue module 4 (enums/length-units "Feet"))))

    (let [FlLe-ScHt (get row "FlLe/ScHt")]
      (when (not-blank? FlLe-ScHt)
        (mortality/setFlameLengthOrScorchHeightValue module FlLe-ScHt (enums/length-units "Feet"))))

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

    (testing (str "csv line #:" (+ row-idx 2))
      (let [expected (get row "MortAvgPercent")
            observed (mortality/calculateMortality module (enums/probability-units "Percent"))]
        (when (not (within-a-percent? expected observed))
          (println (str (+ row-idx 2) "," expected "," observed)))

        (when (not (within-a-percent? expected observed))
          (swap! failing-tests
                 conj
                 (str/join "," [(+ row-idx 2) expected observed (Math/abs (- expected observed))])))

        (is (within-a-percent? expected observed)
            (str "Expected: " expected "  Observed: " observed))))))

(deftest mortality-test
  (let [rows (->> (inline-resource "public/csv/mortality.csv")
                  (parse-csv)
                  (map clean-values))]
    (doall
     (map-indexed (fn [idx row-data]
                    (run-test idx row-data))
                  rows))
    (download (str/join "\n" @failing-tests)
              "tests-errors.csv"
              "application/text")))
