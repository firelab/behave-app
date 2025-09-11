(ns behave.solver.diagrams
  (:require [behave.lib.contain :as contain]
            [behave.lib.enums   :as enums]
            [behave.lib.surface :as surface]
            [clojure.string     :as str]
            [re-frame.core      :as rf]))

(defmulti build-event-vector (fn [{:keys [diagram]}] (:diagram/type diagram)))

(defmethod build-event-vector :contain
  [{:keys [ws-uuid row-id diagram module]}]
  (let [gv-uuid  (get-in diagram [:diagram/group-variable :bp/uuid])
        x-points (contain/getFirePerimeterX module)
        y-points (contain/getFirePerimeterY module)]
    [:worksheet/add-contain-diagram
     ws-uuid
     "Containment"
     gv-uuid
     row-id
     (->> (map #(.get x-points %) (range (.size x-points)))
          (take-nth 10)
          (str/join ","))
     (->> (map #(.get y-points %) (range (.size y-points)))
          (take-nth 10)
          (str/join ","))
     (contain/getLengthToWidthRatio module)
     (contain/getFireBackAtReport module)
     (contain/getFireHeadAtReport module)
     (contain/getFireBackAtAttack module)
     (contain/getFireHeadAtAttack module)
     (contain/getContainmentStatus module)]))

(defmethod build-event-vector :fire-shape
  [{:keys [ws-uuid row-id diagram module]}]
  (let [gv-uuid (get-in diagram [:diagram/group-variable :bp/uuid])]
    [:worksheet/add-surface-fire-shape-diagram
     ws-uuid
     "Fire Shape"
     gv-uuid
     row-id
     (surface/getEllipticalA module (enums/length-units "Chains"))
     (surface/getEllipticalB module (enums/length-units "Chains"))
     (surface/getDirectionOfMaxSpread module)
     (surface/getWindDirection module)
     (surface/getWindSpeed module
                           (enums/speed-units "ChainsPerHour")
                           (surface/getWindHeightInputMode module))
     (surface/getElapsedTime module (enums/time-units "Hours"))]))

(defmethod build-event-vector :wind-slope-spread-direction
  [{:keys [ws-uuid row-id diagram module]}]
  (let [gv-uuid (get-in diagram [:diagram/group-variable :bp/uuid])]
    [:worksheet/add-wind-slope-spread-direction-diagram
     ws-uuid
     "Wind/Slope/Spread Direction"
     gv-uuid
     row-id
     (surface/getDirectionOfMaxSpread module)
     (surface/getHeadingSpreadRate module (enums/speed-units "ChainsPerHour"))
     (= (surface/getSurfaceRunInDirectionOf module) (enums/surface-run-in-direction-of "DirectionOfInterest"))
     (surface/getDirectionOfInterest module)
     (surface/getSpreadRateInDirectionOfInterest module (enums/speed-units "ChainsPerHour"))
     (surface/getDirectionOfFlanking module)
     (surface/getFlankingSpreadRate module (enums/speed-units "ChainsPerHour"))
     (surface/getDirectionOfBacking module)
     (surface/getBackingSpreadRate module (enums/speed-units "ChainsPerHour"))
     (surface/getWindDirection module)
     (surface/getWindSpeed module (enums/speed-units "ChainsPerHour")
                           (surface/getWindHeightInputMode module))]))

(defn store-all-diagrams! [{:keys [ws-uuid row-id module diagrams]}]
  (let [all-outputs @(rf/subscribe [:worksheet/all-output-uuids ws-uuid])]
   (doseq [diagram diagrams]
     (let [group-variable-uuid (get-in diagram [:diagram/group-variable :bp/uuid])]
       (when (some #{group-variable-uuid} all-outputs)
         (rf/dispatch (build-event-vector {:ws-uuid ws-uuid
                                           :diagram diagram
                                           :row-id  row-id
                                           :module  module})))))))
