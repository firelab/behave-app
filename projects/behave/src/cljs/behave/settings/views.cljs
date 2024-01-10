(ns behave.settings.views
  (:require [behave.components.core  :as c]
            [behave.settings.events]
            [behave.translate        :refer [<t bp]]
            [dom-utils.interface     :refer [input-value]]
            [reagent.core            :as r]
            [re-frame.core           :as rf]))

;;==============================================================================
;; Fuel Model Set Selection Tab
;;==============================================================================


(defn- fuel-model-tab []
  [:div "TODO Fuel Model Set Selection"])

;;==============================================================================
;; Moisture Scenario Set Selection Tab
;;==============================================================================

(defn- moisture-scenario-tab []
  [:div "TODO Moisture Scenario Set Selection"])

;;==============================================================================
;; General Units Tab
;;==============================================================================

(defn- unit-selector [cur-selected-unit-uuid units on-click]
  (let [*unit-short-code (rf/subscribe [:vms/units-uuid->short-code cur-selected-unit-uuid])]
    [:div.settings-table_unit-selector
     [c/dropdown
      {:id        "unit-selector"
       :on-change #(on-click (input-value %))
       :name      "unit-selector"
       :options   (distinct
                   (concat [{:label @*unit-short-code
                             :value cur-selected-unit-uuid}]
                           (->> units
                                (map (fn [unit]
                                       {:label (:unit/short-code unit)
                                        :value (:bp/uuid unit)}))
                                (sort-by :label))))}]]))

(defn- build-rows [domain-set domain-unit-settings]
  (map
   (fn [[domain-uuid {:keys [domain-name
                             domain-dimension-uuid
                             domain-native-unit-uuid
                             domain-decimals]}]]
     {:domain   domain-name
      :units    (if (not= domain-dimension-uuid "N/A")
                  (let [dimension (rf/subscribe [:vms/entity-from-uuid domain-dimension-uuid])
                        units     (:dimension/units @dimension)
                        on-click  #(rf/dispatch-sync [:settings/cache-unit-preference domain-set domain-uuid %])]
                    [unit-selector domain-native-unit-uuid units on-click])
                  [:div @(rf/subscribe [:vms/units-uuid->short-code domain-native-unit-uuid])])
      :decimals (when (not= domain-decimals "N/A")
                  (let [decimal-atom (r/atom domain-decimals)]
                    [c/number-input {:value-atom decimal-atom
                                     :on-change  #(reset! decimal-atom (input-value %))
                                     :on-blur    #(rf/dispatch-sync [:settings/cache-decimal-preference
                                                                     domain-set domain-uuid @decimal-atom])}]))})
   domain-unit-settings))

(defn- general-units-tab []
  (r/with-let [_ (rf/dispatch [:settings/load-units-from-local-storage])]
    (let [*state-settings (rf/subscribe [:settings/get :units])
          domain-sets     (sort-by first @*state-settings)]
      [:div.settings__general-units
       (c/accordion {:accordion-items (for [[domain-set-name domain-unit-settings] domain-sets]
                                        ^{:key domain-sets}
                                        {:label   domain-set-name
                                         :content (c/table {:headers [@(<t (bp "variable_domain"))
                                                                      @(<t (bp "units"))
                                                                      @(<t (bp "decimals"))]
                                                            :columns [:domain :units :decimals]
                                                            :rows    (build-rows domain-set-name
                                                                                 (sort-by
                                                                                  (fn [[_ domain]]
                                                                                    (:domain-name domain))
                                                                                  domain-unit-settings))})})})])))


;;==============================================================================
;; Root Component
;;==============================================================================

(defn settings-page [_params]
  (let [*tab-selected (rf/subscribe [:state [:settings :units :current-tab]])]
    [:div.settings
     [c/tab-group {:variant  "outline-secondary"
                   :on-click #(rf/dispatch [:state/set [:settings :units :current-tab] (:tab %)])
                   :tabs     [{:label     @(<t (bp "general_units"))
                               :tab       :general-units
                               :selected? (= @*tab-selected :general-units)}
                              {:label     @(<t (bp "fuel_model_selection"))
                               :tab       :fuel-model
                               :selected? (= @*tab-selected :fuel-model)}
                              {:label     @(<t (bp "moisture_scenario_selection"))
                               :tab       :moisture-scenario
                               :selected? (= @*tab-selected :moisture-scenario)}]}]
     [:div.settings__body
      (case @*tab-selected
        :general-units     [general-units-tab]
        :fuel-model        [fuel-model-tab]
        :moisture-scenario [moisture-scenario-tab]
        nil                [:div "no page"])]
     [:div.wizard-navigation
      [c/button {:label    @(<t (bp "back"))
                 :variant  "secondary"
                 :on-click #(.back js/history)}]
      [c/button {:label         @(<t (bp "reset_to_defaults"))
                 :variant       "highlight"
                 :icon-name     "arrow2"
                 :icon-position "right"
                 :on-click      #(rf/dispatch [:settings/reset-custom-unit-preferences])}]]]))
