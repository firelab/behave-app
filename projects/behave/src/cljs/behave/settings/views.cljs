(ns behave.settings.views
  (:require [re-frame.core    :as rf]
            [reagent.core            :as r]
            [behave.components.core  :as c]
            [behave.settings.events]
            [dom-utils.interface     :refer [input-value]]))

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

(defn- build-rows [category settings]
  (map
   (fn [[v-uuid {:keys [v-name v-dimension-uuid unit-uuid decimals]}]]
     {:variable v-name
      :units    (let [dimension (rf/subscribe [:vms/entity-from-uuid v-dimension-uuid])
                      units     (:dimension/units @dimension)
                      on-click  #(rf/dispatch-sync [:setting/cache-unit-preference category v-uuid %])]
                  [unit-selector unit-uuid units on-click])
      :decimals (let [decimal-atom (r/atom decimals)]
                  [c/number-input {:value-atom decimal-atom
                                   :on-change  #(reset! decimal-atom (input-value %))
                                   :on-blur    #(rf/dispatch-sync [:setting/cache-decimal-preference
                                                                   category v-uuid @decimal-atom])}])})
   settings))

(defn- general-units-tab []
  (r/with-let [_ (rf/dispatch [:load-units-from-local-storage])]
    (let [*state-settings (rf/subscribe [:settings/get :units])
          categories      (sort-by first @*state-settings)]
      [:div.settings__general-units
       (c/accordion {:accordion-items (for [[category settings] categories]
                                        ^{:key category}
                                        {:label   category
                                         :content (c/table {:headers ["Variable" "Units" "Decimals"]
                                                            :columns [:variable :units :decimals]
                                                            :rows    (build-rows category settings)})})})
       [:div.wizard-navigation
        [c/button {:label    "Back"
                   :variant  "secondary"
                   :on-click #(.back js/history)}]
        [c/button {:label         "Reset Default Settings"
                   :variant       "highlight"
                   :icon-name     "arrow2"
                   :icon-position "right"
                   :on-click      #(rf/dispatch [:settings/reset-custom-unit-preferences])}]]])))

;;==============================================================================
;; Root Component
;;==============================================================================

(defn settings-page [_params]
  (let [*tab-selected (rf/subscribe [:state [:settings :units :current-tab]])]
    [:div.settings
     [c/tab-group {:variant  "outline-secondary"
                   :on-click #(rf/dispatch [:state/set [:settings :units :current-tab] (:tab %)])
                   :tabs     [{:label     "General Units"
                               :tab       :general-units
                               :selected? (= @*tab-selected :general-units)}
                              {:label     "Fuel Model Selection"
                               :tab       :fuel-model
                               :selected? (= @*tab-selected :fuel-model)}
                              {:label     "Moisture Scenario Set Selection"
                               :tab       :moisture-scenario
                               :selected? (= @*tab-selected :moisture-scenario)}]}]
     (case @*tab-selected
       :general-units     [general-units-tab]
       :fuel-model        [fuel-model-tab]
       :moisture-scenario [moisture-scenario-tab]
       nil                [:div "no page"])]))
