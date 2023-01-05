(ns behave.solver-test
  (:require [cljs.test          :refer [is deftest]]
            [datascript.core    :as d]
            [re-frame.core      :as rf]
            [behave.lib.contain :as contain]
            [behave.lib.enums   :as enums]
            [behave.lib.units   :refer [get-unit]]
            [behave.solver      :as solver]))

(defn simple-solver [{:keys [inputs outputs]} results]
  (let [module (contain/init)]
    (-> module
        (contain/setAttackDistance
         (js/parseFloat (get-in inputs [:bh.group/Supresssion 0 :fs/LineConstructionOffset]))
         (get-unit "ch"))

        (contain/setLwRatio
         (js/parseFloat (get-in inputs [:bh.group/Fire 0 :fs/LengthToWidthRatio])))

        (contain/setReportRate
         (js/parseFloat (get-in inputs [:bh.group/Fire 0 :fs/SurfaceRateOfSpread]))
         (get-unit "ch/h"))

        (contain/setReportSize
         (js/parseFloat (get-in inputs [:bh.group/Fire 0 :fs/ReportSize]))
         (get-unit "ac"))

        (contain/setTactic
         (enums/contain-tactic
          (get-in inputs [:bh.group/Suppression 0 :fs/SuppressionTactic])))

        (contain/addResource
         (js/parseFloat (get-in inputs [:bh.group/Suppression 0 :fs/ResourceArrivalTime]))
         (js/parseFloat (get-in inputs [:bh.group/Suppression 0 :fs/ResourceProductionRate]))
         (get-unit "h")
         (js/parseFloat (get-in inputs [:bh.group/Suppression 0 :fs/ResourceDuration]))
         (get-unit "ch/h")
         (get-in inputs [:bh.group/Suppression 0 :fs/ResourceName])))

    (contain/doContainRun module)

    (let [module-outputs
          {:fs/FirelineConstructed                (contain/getFinalFireLineLength module (get-unit "ch"))
           :fs/FirePerimeterAtResourceArrivalTime (contain/getPerimeterAtInitialAttack module (get-unit "ch"))
           ;:fs/ (contain/getPerimeterAtContainment module (get-unit "ch"))
           :fs/FireAreaAtResourceArrivalTime      (contain/getFireSizeAtInitialAttack module (get-unit "ac"))
           :fs/ContainedArea                      (contain/getFinalContainmentArea module (get-unit "ac"))
           :fs/TimeFromReport                     (contain/getFinalTimeSinceReport module (get-unit "m"))
           :fs/ContainedStatus                    (contain/getContainmentStatus module)}]

      (reduce (fn [acc [k v]] (if (= true v)
                                (assoc acc k (get module-outputs k))
                                acc))
              results outputs))))

(deftest simple-solver-test
  (is (= {} (simple-solver {:inputs {:bh.group/Fire {0 {:fs/ReportSize "10"
                                                        :fs/LengthToWidthRatio "1"
                                                        :fs/SurfaceFireRateOfSpread "5"}}
                                     :bh.group/Supresssion {0 {:fs/LineConstructionOffset "30"
                                                               :fs/SuppressionTactic "HeadAttack"
                                                               :fs/ResourceArrivalTime "10"
                                                               :fs/ResourceDuration "20"
                                                               :fs/ResourceProductionRate "20"
                                                               :fs/ResourceName "Derp"}}}
                            :outputs {:fs/ContainedArea true
                                      :fs/ContainStatus false}}
                           {}))))

(deftest contain-solver-test
  ;; Arrange
  (let [ws-uuid (str (d/squuid))
        name    "Test Worksheet"
        modules #{:surface :contain}]

    (rf/dispatch-sync [:worksheet/new {:uuid    ws-uuid
                                       :name    name
                                       :modules modules}])

    (let [result (solver/contain-solver ws-uuid {})]

      (is (= {:contain nil} result)))))

(deftest multi-valued-inputs

  )
