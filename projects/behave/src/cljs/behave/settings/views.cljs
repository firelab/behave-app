(ns behave.settings.views
  (:require
   [behave.components.core :as c]
   [behave.settings.events]
   [behave.translate       :refer [<t bp]]
   [dom-utils.interface    :refer [input-value]]
   [reagent.core           :as r]
   [re-frame.core          :as rf]))

;;==============================================================================
;; Fuel Model Set Selection Tab
;;==============================================================================

(defn- fuel-model-tab []
  [:div "Coming soon!"])

;;==============================================================================
;; Moisture Scenario Set Selection Tab
;;==============================================================================

(defn- moisture-scenario-tab []
  [:div "Coming soon!"])

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
                   (concat [{:label     @*unit-short-code
                             :selected? true
                             :value     cur-selected-unit-uuid}]
                           (->> units
                                (map (fn [unit]
                                       {:label (:unit/short-code unit)
                                        :value (:bp/uuid unit)}))
                                (sort-by :label))))}]]))

(defn- build-rows [ws-uuid domain-set domain-unit-settings]
  (let [cached-units-system @(rf/subscribe [:settings/application-units-system])]
    (map
     (fn [[domain-uuid {:keys [domain-name
                               domain-dimension-uuid
                               domain-cached-unit-uuid
                               domain-native-unit-uuid
                               domain-english-unit-uuid
                               domain-metric-unit-uuid
                               domain-decimals]}]]
       {:domain   domain-name
        :units    (if (not= domain-dimension-uuid "N/A")
                    (let [dimension (rf/subscribe [:vms/entity-from-uuid domain-dimension-uuid])
                          units     (:dimension/units @dimension)
                          on-click  #(rf/dispatch-sync [:settings/cache-unit-preference
                                                        domain-set
                                                        domain-uuid
                                                        %
                                                        ws-uuid])]
                      [unit-selector
                       (or domain-cached-unit-uuid
                           (case cached-units-system
                             :english domain-english-unit-uuid
                             :metric  domain-metric-unit-uuid
                             domain-native-unit-uuid))
                       units on-click])
                    [:div @(rf/subscribe [:vms/units-uuid->short-code domain-native-unit-uuid])])
        :decimals (when (not= domain-decimals "N/A")
                    (let [decimal-atom (r/atom domain-decimals)]
                      [c/number-input {:value-atom decimal-atom
                                       :on-change  #(reset! decimal-atom (input-value %))
                                       :on-blur    #(rf/dispatch-sync [:settings/cache-decimal-preference
                                                                       domain-set domain-uuid @decimal-atom])}]))})
     domain-unit-settings)))

(defn- general-units-tab [{:keys [ws-uuid]}]
  (r/with-let [_ (rf/dispatch [:settings/load-units-from-local-storage])]
    (let [*state-settings (rf/subscribe [:settings/get :units])
          domain-sets     (sort-by first @*state-settings)]
      [:div.settings__general-units
       [:div.settings__general-units__units-system-selection
        [c/toggle
         {:label       @(<t (bp "units_system"))
          :left-label  @(<t (bp "english"))
          :right-label @(<t (bp "metric"))
          :checked?    (= @(rf/subscribe [:settings/application-units-system]) :metric)
          :on-change   #(rf/dispatch [:settings/set-units-system
                                      (if (= @(rf/subscribe [:settings/application-units-system]) :english)
                                        :metric
                                        :english)])}]]
       [:div.settings__general-units__table
        (c/accordion {:accordion-items (for [[domain-set-name domain-unit-settings] domain-sets]
                                         ^{:key domain-sets}
                                         {:label   domain-set-name
                                          :content (c/table {:headers [@(<t (bp "variable_domain"))
                                                                       @(<t (bp "units"))
                                                                       @(<t (bp "decimals"))]
                                                             :columns [:domain :units :decimals]
                                                             :rows    (build-rows
                                                                       ws-uuid
                                                                       domain-set-name
                                                                       (sort-by
                                                                        (fn [[_ domain]]
                                                                          (:domain-name domain))
                                                                        domain-unit-settings))})})})]])))

;;==============================================================================
;; Root Component
;;==============================================================================

(defn settings-page [params]
  (let [*tab-selected (rf/subscribe [:state [:settings :units :current-tab]])]
    [:div.settings
     [c/tab-group {:variant  "secondary"
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
        :general-units     [general-units-tab params]
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
                 :on-click      #(when (js/confirm @(<t (bp "are_you_sure_you_want_to_reset_your_unit_preferences?")))
                                   (rf/dispatch [:settings/reset-custom-unit-preferences]))}]]]))
