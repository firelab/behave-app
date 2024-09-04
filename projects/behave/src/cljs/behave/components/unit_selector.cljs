(ns behave.components.unit-selector
  (:require [reagent.core            :as r]
            [re-frame.core           :as rf]
            [behave.components.core  :as c]
            [dom-utils.interface     :refer [input-value]]
            [behave.translate        :refer [<t bp]]))

;;; Helpers

(defn index-by
  "Indexes collection by key or fn."
  [k-or-fn coll]
  (persistent! (reduce
                (fn [acc cur] (assoc! acc (k-or-fn cur) cur))
                (transient {})
                coll)))

;;; Components

(defn- unit-selector [prev-unit-uuid units on-click]
  (r/with-let [*unit-uuid (r/atom prev-unit-uuid)]
    [:div.wizard-input__unit-selector
     [c/dropdown
      {:id            "unit-selector"
       :default-value @*unit-uuid
       :selected      @*unit-uuid
       :on-change     #(on-click (input-value %))
       :name          "unit-selector"
       :options       (->> units
                           (map (fn [unit]
                                  (cond->
                                      {:label (:unit/name unit)
                                       :value (:bp/uuid unit)}
                                    (= @*unit-uuid (:bp/uuid unit))
                                    (assoc :selected? true))))
                           (sort-by :label))}]]))

(defn unit-display
  "Displays the units for a continuous variable, and enables unit selection."
  [domain-uuid *unit-uuid dimension-uuid native-unit-uuid english-unit-uuid metric-unit-uuid & [on-change-units]]
  (r/with-let [dimension         (rf/subscribe [:vms/entity-from-uuid dimension-uuid])
               units             (:dimension/units @dimension)
               units-by-uuid     (index-by :bp/uuid units)
               *cached-unit-uuid (rf/subscribe [:settings/cached-unit domain-uuid])
               *cached-unit      (rf/subscribe [:vms/entity-from-uuid @*cached-unit-uuid])
               native-unit       @(rf/subscribe [:vms/entity-from-uuid native-unit-uuid])
               english-unit      @(rf/subscribe [:vms/entity-from-uuid english-unit-uuid])
               metric-unit       @(rf/subscribe [:vms/entity-from-uuid metric-unit-uuid])
               default-unit      (or @*cached-unit native-unit english-unit metric-unit) ;; FIXME: Get from Worksheet settings
               show-selector? (r/atom false)
               on-click       #(do
                                 (on-change-units %)
                                 (reset! show-selector? false))]
    [:div.wizard-input__units
     (if (or (>= 1 (count units)) (nil? @dimension))
      [:div.wizard-input__units__text
       (str @(<t (bp "units_used")) " " (:unit/short-code (or (get units-by-uuid *unit-uuid) default-unit)))]
      [unit-selector *unit-uuid units on-click])]))
