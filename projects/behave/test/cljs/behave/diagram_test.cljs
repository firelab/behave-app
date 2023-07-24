(ns behave.diagram-test
  (:require [cljs.test            :refer [is deftest testing]]
            [csv-parser.interface :refer [parse-csv]]
            [behave.lib.contain   :as contain]
            [behave.lib.enums     :as enums]
            [behave.lib.units     :refer [get-unit]])
  (:require-macros [behave.macros :refer [inline-resource]]))

(deftest contain-diagram-getter-test
  (let [row    (->> (inline-resource "public/csv/contain.csv")
                    (parse-csv)
                    (first))
        module (contain/init)]

    ;; Setup Run
    (doto module
      (contain/setAttackDistance (get row "attackDistance") (get-unit "ch"))
      (contain/setLwRatio (get row "lwRatio"))
      (contain/setReportRate (get row "reportRate") (get-unit "ch/h"))
      (contain/setReportSize (get row "reportSize") (get-unit "ac"))
      (contain/setTactic (enums/contain-tactic (get row "tactic")))
      (contain/addResource (get row "resourceArrival")
                           (get row "resourceDuration")
                           (get-unit "h")
                           (get row "resourceProduction")
                           (get-unit "ch/h")
                           (get row "resourceDescription")))

    ;; Do Run
    (contain/doContainRun module)

    (testing "Getters for Contain Diagram Parameters Exist"

      ;; Fireline Constructed
      (is (some? (contain/getFirePerimeterX module)))
      (is (some? (contain/getFirePerimeterY module)))
      (is (some? (contain/getAttackDistance module (enums/length-units "Chains"))))

      ;; Perimter at Report Ellipse. The difference between these two number will give us the length
      ;; of the ellipse. Using this length and the length to width ratio we can derrive the width of
      ;; the ellipse.
      (is (some? (contain/getFireBackAtReport module)))
      (is (some? (contain/getFireHeadAtReport module)))

      ;; Perimter at Attack Ellipse
      (is (some? (contain/getFireBackAtAttack module)))
      (is (some? (contain/getFireHeadAtAttack module)))

      ;; Used for both Perimter at Report and Attack
      (is (some? (contain/getLengthToWidthRatio module))))))
