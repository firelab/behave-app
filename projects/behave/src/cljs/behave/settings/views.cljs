(ns behave.settings.views
  (:require [re-frame.core    :as rf]
            [goog.string :as gstring]
            [reagent.core            :as r]
            [behave.components.core  :as c]
            [dom-utils.interface     :refer [input-value]]))

(defn- unit-selector [prev-unit-uuid units on-click]
  (r/with-let [*unit-uuid (r/atom prev-unit-uuid)]
    (let [*prev-unit-name (rf/subscribe [:entity-uuid->name prev-unit-uuid])]
      [:div.wizard-input__unit-selector
       [c/dropdown
        {:id            "unit-selector"
         :default-value @*unit-uuid
         :on-change     #(on-click (input-value %))
         :name          "unit-selector"
         :options       (distinct
                         (concat [{:label @*prev-unit-name
                                   :value prev-unit-uuid}]
                                 (->> units
                                      (map (fn [unit]
                                             {:label (:unit/name unit)
                                              :value (:bp/uuid unit)}))
                                      (sort-by :label))))}]])))

(defn- load-settings-from-local-storage! []
  (let [*units-settings (rf/subscribe [:settings/units+decimals])]
    (doseq [[category settings]                                   @*units-settings
            [_ v-name v-uuid v-dimension-uuid unit-uuid decimals] settings]
      (rf/dispatch [:state/set [:settings category v-uuid]
                    {:v-name           v-name
                     :v-dimension-uuid v-dimension-uuid
                     :unit-uuid        unit-uuid
                     :decimals         decimals}]))))

(defn custom-unit-preferences-page [params]
  (load-settings-from-local-storage!)
  (let [*state-settings (rf/subscribe [:state :settings])]
    [:div (for [[category settings] @*state-settings]
            [:div category
             (for [[v-uuid {:keys [v-name v-dimension-uuid unit-uuid decimals]}] settings]
               (let [*unit-name (rf/subscribe [:entity-uuid->name unit-uuid])]
                 [:div (gstring/format "%s %s %d" v-name @*unit-name decimals)
                  (let [dimension (rf/subscribe [:vms/entity-from-uuid v-dimension-uuid])
                        units     (:dimension/units @dimension)
                        on-click  #(do
                                     (rf/dispatch [:state/set [:settings category v-uuid :unit-uuid] %])
                                     (rf/dispatch [:local-storage/update-in
                                                   [:units v-uuid :unit-uuid] %]))]
                    [:div [unit-selector unit-uuid units on-click]])]))])
     [c/button {:label         "Reset Default Settings"
                :variant       "highlight"
                :icon-name     "arrow2"
                :icon-position "right"
                :on-click      #(when (js/confirm (str "Are you sure you want to reset your unit prefereneces?"))
                                  (rf/dispatch [:local-storage/clear])
                                  (load-settings-from-local-storage!))}]]))

(defn root-component [params]
  (case (:page params)
    :units
    (custom-unit-preferences-page params)

    :else
    [:div
     [:h1 (str "Settings - " (get params :page "All"))]]))
