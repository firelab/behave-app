(ns behave-cms.variables.views
  (:require [reagent.core                      :as r]
            [re-frame.core                     :as rf]
            [behave-cms.components.common      :refer [simple-table
                                                       labeled-text-input
                                                       labeled-float-input
                                                       labeled-integer-input]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.utils                  :as u]
            [behave-cms.events]
            [behave-cms.subs]))

(def columns [:variable/name :variable/kind])

(defn variables-table []
  (r/with-let [variables (rf/subscribe [:variables])
               on-select #(do
                            (rf/dispatch [:state/set-state :variable (:db/id %)]))
               on-delete #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable_name %) "?"))
                            (rf/dispatch [:api/delete-entity %]))]
    [simple-table
     columns
     (->> @variables (vals) (sort-by :variable_name))
     on-select
     on-delete]))

(def continuous-variable-properties
  [{:label "Default Value  " :field-key :variable/default-value    :type :float}
   {:label "English Decimal" :field-key :variable/english-decimals :type :int}
   {:label "English Units"   :field-key :variable/english-units    :type :str}
   {:label "Maximum"         :field-key :variable/maximum          :type :float}
   {:label "Minimum"         :field-key :variable/minimum          :type :float}
   {:label "Metric Decimals" :field-key :variable/metric-decimals  :type :int}
   {:label "Metric Units"    :field-key :variable/metric-units     :type :str}
   {:label "Native Decimals" :field-key :variable/native-decimals  :type :int}
   {:label "Native Units"    :field-key :variable/native-units     :type :str}])

(defmulti properties-form (fn [kind _ _] (keyword kind)))

(defmethod properties-form
  :continuous
  [_ props update-props]
  [:div
   (for [{:keys [label field-key type]} continuous-variable-properties]
     [:div.mb-3 {:keys field-key}
      [:label.form-label label]
      (condp = type
        :float
        [:input.form-control {:type      "number"
                              :value     (get @props field-key)
                              :on-change #(update-props {field-key (u/input-float-value %)})}]

        :int
        [:input.form-control {:type      "number"
                              :value     (get @props field-key)
                              :on-change #(update-props {field-key (u/input-int-value %)})}]

        ; default
        [:input.form-control {:type      "text"
                              :value     (get @props field-key)
                              :on-change #(update-props {field-key (u/input-value %)})}])])])

(defmethod properties-form
  :discrete
  [_ props update-props]
  [:div.mb-3
   [:label.form-label "List"]
   [:input.form-control {:type      "text"
                         :value     (get @props :list)
                         :on-change #(update-props {:variable/list (u/input-value %)})}]])

(defn variable-form [id]
  (let [variable     (rf/subscribe [:entity id])
        var-editor   (rf/subscribe [:state [:editors :variables]])
        update-props #(rf/dispatch [:state/merge [:editors :variables :properties] %])
        props        (rf/subscribe [:state [:editors :variables :properties]])]
    [:<>
     [:div.row
      [:h3 (if id
             (str "Edit " (:variable/name @variable))
             "Add Variable")]]
     [:div.row
      [:div.col-6
       [entity-form {:entity :variables
                     :id     id
                     :fields [{:label     "Name"
                               :required? true
                               :field-key :variable/name}
                              {:label     "Kind"
                               :field-key :variable/kind
                               :required? true
                               :type      :radio
                               :options   [{:label "Continuous" :value "continuous"}
                                           {:label "Discrete" :value "discrete"}
                                           {:label "Text" :value "text"}]}]}]]

      [:div.col-6
       (when (#{"continuous" "discrete"} (:variable/kind @var-editor))
         [:div
          [:h6 "Properties"]
          [properties-form (:variable/kind @var-editor) props update-props]])]]]))

(defn list-variables-page [_]
  (let [loaded?   (rf/subscribe [:state :loaded?])
        *variable (rf/subscribe [:state :variable])]
    (if @loaded?
      [:div.container
       [:div.row.my-3
        [:div.col-12
         [:h3 "Variables"]
         [:div
          {:style {:height "400px" :overflow-y "scroll"}}
          [variables-table]]]]
       [variable-form @*variable]]
      [:div "Loading..."])))
