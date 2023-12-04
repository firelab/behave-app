(ns behave.settings.views
  (:require [re-frame.core    :as rf]
            [reagent.core            :as r]
            [behave.components.core  :as c]
            [behave.settings.events]
            [dom-utils.interface     :refer [input-value]]))

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

(defn custom-unit-preferences-page [_]
  (r/with-let [_ (rf/dispatch [:load-settings-from-local-storage])]
    (let [*state-settings (rf/subscribe [:settings/get :units])
          categories      (sort-by first @*state-settings)
          first-tab       (keyword (ffirst categories))
          *tab-selected   (rf/subscribe [:state [:settings :units :current-tab]])]
      (when (nil? @*tab-selected) (rf/dispatch [:state/set [:settings :units :current-tab] first-tab]))
      [:div
       [c/tab-group {:variant  "outline-primary"
                     :on-click #(rf/dispatch [:state/set [:settings :units :current-tab] (:tab %)])
                     :tabs     (mapv (fn [[category _]]
                                       (let [category-kw (keyword category)]
                                         {:label     category
                                          :tab       category-kw
                                          :selected? (= @*tab-selected category-kw)}))
                                     categories)}]
       [:div
        (for [[category settings] categories
              :when               (= @*tab-selected (keyword category))]
          ^{:key category}
          [:div.settings-table
           (c/table {:title   "Custom Unit Preferences"
                     :headers ["Variable" "Units" "Decimals"]
                     :columns [:variable :units :decimals]
                     :rows    (build-rows category settings)})])]
       [:div.wizard-navigation
        [c/button {:label    "Back"
                   :variant  "secondary"
                   :on-click #(.back js/history)}]
        [c/button {:label         "Reset Default Settings"
                   :variant       "highlight"
                   :icon-name     "arrow2"
                   :icon-position "right"
                   :on-click      #(rf/dispatch [:settings/reset-custom-unit-preferences])}]]])))

(defn root-component [params]
  (case (:page params)
    :units
    (custom-unit-preferences-page params)

    :else
    [:div
     [:h1 (str "Settings - " (get params :page "All"))]]))
