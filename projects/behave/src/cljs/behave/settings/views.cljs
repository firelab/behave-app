(ns behave.settings.views
  (:require [re-frame.core    :as rf]
            [goog.string :as gstring]
            [reagent.core            :as r]
            [behave.components.core  :as c]
            [dom-utils.interface     :refer [input-value]]))

(defn- unit-selector [prev-unit-uuid units on-click]
  (r/with-let [*unit-uuid (r/atom prev-unit-uuid)]
    (let [*prev-unit-short-code (rf/subscribe [:vms/units-uuid->short-code prev-unit-uuid])]
      [c/dropdown
       {:id            "unit-selector"
        :default-value @*unit-uuid
        :on-change     #(on-click (input-value %))
        :name          "unit-selector"
        :options       (distinct
                        (concat [{:label @*prev-unit-short-code
                                  :value prev-unit-uuid}]
                                (->> units
                                     (map (fn [unit]
                                            {:label (:unit/short-code unit)
                                             :value (:bp/uuid unit)}))
                                     (sort-by :label))))}])))

(defn- load-settings-from-local-storage! []
  (let [*units-settings (rf/subscribe [:settings/all-units+decimals])]
    (doseq [[category settings]                                   @*units-settings
            [_ v-name v-uuid v-dimension-uuid unit-uuid decimals] settings]
      (rf/dispatch-sync [:settings/set [:units category v-uuid]
                         {:v-name           v-name
                          :v-dimension-uuid v-dimension-uuid
                          :unit-uuid        unit-uuid
                          :decimals         decimals}]))))

(defn custom-unit-preferences-page [params]
  (load-settings-from-local-storage!)
  (r/with-let [*state-settings (rf/subscribe [:settings/get :units])]
    [:div (for [[category settings] (sort-by first @*state-settings)]
            [:div.settings-table category
             (c/table {:title   "Custom Unit Preferences"
                       :headers ["Variable" "Units" "Decimals"]
                       :columns [:variable :units :decimals]
                       :rows    (map
                                 (fn [[v-uuid {:keys [v-name v-dimension-uuid unit-uuid decimals]}]]
                                   {:variable v-name
                                    :units    (let [dimension (rf/subscribe [:vms/entity-from-uuid v-dimension-uuid])
                                                    units     (:dimension/units @dimension)
                                                    on-click  #(do
                                                                 (rf/dispatch [:settings/set [:units category v-uuid :unit-uuid] %])
                                                                 (rf/dispatch [:local-storage/update-in [:units v-uuid :unit-uuid] %]))]
                                                [unit-selector unit-uuid units on-click])
                                    :decimals (let [decimal-atom (r/atom decimals)]
                                                [c/number-input {:value-atom decimal-atom
                                                                 :on-change  #(reset! decimal-atom (input-value %))
                                                                 :on-blur    #(do
                                                                                (rf/dispatch [:settings/set [:units category v-uuid :decimals] @decimal-atom])
                                                                                (rf/dispatch [:local-storage/update-in [:units v-uuid :decimals] @decimal-atom]))}])})
                                 settings)})])
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
