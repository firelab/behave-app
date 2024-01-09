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
       :on-change     #(on-click (input-value %))
       :name          "unit-selector"
       :options       (concat [{:label "Select..." :value nil}]
                              (->> units
                                   (map (fn [unit]
                                          {:label (:unit/name unit)
                                           :value (:bp/uuid unit)}))
                                   (sort-by :label)))}]]))

(defn unit-display
  "Displays the units for a continuous variable, and enables unit selection."
  [domain-uuid *unit-uuid dimension-uuid native-unit-uuid english-unit-uuid metric-unit-uuid & [on-change-units]]
  (r/with-let [dimension         (rf/subscribe [:vms/entity-from-uuid dimension-uuid])
               units             (:dimension/units @dimension)
               units-by-uuid     (index-by :bp/uuid units)
               *cached-unit-uuid (rf/subscribe [:settings/cached-unit domain-uuid])
               *cached-unit      (rf/subscribe [:vms/entity-from-uuid @*cached-unit-uuid])
               native-unit       (get units-by-uuid native-unit-uuid)
               english-unit      (get units-by-uuid english-unit-uuid)
               metric-unit       (get units-by-uuid metric-unit-uuid)
               default-unit      (or @*cached-unit native-unit english-unit metric-unit) ;; FIXME: Get from Worksheet settings
               show-selector? (r/atom false)
               on-click       #(do
                                 (on-change-units %)
                                 (reset! show-selector? false))]
    [:div.wizard-input__description
     (str @(<t (bp "units_used")) " " (:unit/short-code (or (get units-by-uuid *unit-uuid) default-unit)))
     [:div.wizard-input__description__units
      (when english-unit
        [:div (str @(<t (bp "english_units")) " " (:unit/short-code english-unit))])
      (when metric-unit
        [:div (str @(<t (bp "metric_units")) " " (:unit/short-code metric-unit))])]

     (when (and on-change-units (< 1 (count units)))
       [c/button {:variant  "secondary"
                  :label    @(<t (bp "change_units"))
                  :disabled? @show-selector?
                  :on-click #(swap! show-selector? not)}])
     (when @show-selector?
       [unit-selector *unit-uuid units on-click])]))
