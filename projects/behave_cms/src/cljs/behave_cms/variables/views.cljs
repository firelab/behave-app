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

(def columns [:variable/name :variable/domain-uuid :variable/bp6-label :variable/bp6-code :variable/kind])

(defn variables-table []
  (let [variables (rf/subscribe [:pull-with-attr :variable/name])
        on-select #(do
                     (rf/dispatch [:state/set-state :variable (:db/id %)]))
        on-delete #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable_name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))]
    [simple-table
     columns
     (sort-by :variable/name @variables)
     {:on-select on-select
      :on-delete on-delete}]))

(def continuous-variable-properties
  [{:label "Dimension"       :field-key :variable/dimension-uuid    :type :dimension}
   {:label "English Units"   :field-key :variable/english-unit-uuid :type :unit :system :english}
   {:label "English Decimal" :field-key :variable/english-decimals  :type :int}
   {:label "Metric Units"    :field-key :variable/metric-unit-uuid  :type :unit :system :metric}
   {:label "Metric Decimals" :field-key :variable/metric-decimals   :type :int}
   {:label "Native Units"    :field-key :variable/native-unit-uuid  :type :unit}
   {:label "Native Decimals" :field-key :variable/native-decimals   :type :int}
   {:label "Default Value  " :field-key :variable/default-value     :type :float}
   {:label "Maximum"         :field-key :variable/maximum           :type :float}
   {:label "Minimum"         :field-key :variable/minimum           :type :float}])

(defmulti properties-form (fn [kind _ _] (keyword kind)))

(defmethod properties-form
  :continuous
  [_ get-field set-field]
  (let [dimensions (rf/subscribe [:dimensions])
        *dimension (get-field :variable/dimension-uuid)
        units      (->> @dimensions
                        (filter #(= @*dimension (:bp/uuid %)))
                        (first)
                        (:dimension/units))]
    [:div
     (doall
      (for [{:keys [label field-key type system]} continuous-variable-properties]
        (let [units     (if system (filter #(= (:unit/system %) system) units) units)
              value     (get-field field-key)
              on-change (set-field field-key)]
          (println [:DIM @*dimension :U units])
          ^{:key field-key}
          [:div.mb-3 {:keys field-key}
           [:label.form-label label]
           (condp = type

             :dimension
             [:select.form-select {:default-value @value
                                   :on-change     #(on-change (u/input-value %))}
              (when (nil? @value)
                [:option {:value nil} "Select Dimension..."])
              (doall
               (for [{dim-name :dimension/name dim-uuid :bp/uuid} @dimensions]
                 ^{:key dim-uuid}
                 [:option {:value dim-uuid} dim-name]))]

             :unit
             [:select.form-select {:default-value @value
                                   :disabled      (nil? @*dimension)
                                   :on-change     #(on-change (u/input-value %))}
              (when (nil? @value)
                [:option {:value nil} "Select Units..."])
              (doall
               (for [{unit-name :unit/name short-code :unit/short-code unit-uuid :bp/uuid} units]
                 ^{:key unit-uuid}
                 [:option {:value unit-uuid} (str unit-name " (" short-code ")")]))]

             :float
             [:input.form-control {:type          "number"
                                   :default-value @value
                                   :on-change     #(on-change (u/input-float-value %))}]

             :int
             [:input.form-control {:type          "number"
                                   :default-value @value
                                   :on-change     #(on-change (u/input-int-value %))}]

                                        ; default
             [:input.form-control {:type          "text"
                                   :default-value @value
                                   :on-change     #(on-change (u/input-value %))}])])))]))

(defmethod properties-form
  :discrete
  [_ get-field set-field]
  (let [lists     (rf/subscribe [:lists])
        value     (get-field :variable/list :db/id)
        on-change (set-field :variable/list :db/id)]
    [:div.mb-3
     [:div.form-group.mt-2
      [:label.form-label "List"]
      [:select.form-select
       {:on-change     #(on-change (u/input-int-value %))
        :default-value @value}
       (doall
        (for [list @lists]
          (let [list-name (:list/name list)
                id        (:db/id list)]
            [:option {:key id :value id :selected (= id @value)} list-name])))]]]))

(defn variable-form [id]
  (let [variable    (rf/subscribe [:entity id])
        editor      (rf/subscribe [:state [:editors :variables]])
        get-field   (fn [& fields]
                    (r/track #(or (get-in @editor fields) (get-in @variable fields))))
        set-field   (fn [& fields]
                    (fn [value]
                      (rf/dispatch [:state/set-state (apply conj [:editors :variables] fields) value])))
        kind        (get-field :variable/kind)
        domain-sets (rf/subscribe [:domain-sets])]
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
                              {:label     "Domain Set"
                               :field-key :variable/domain-uuid
                               :type      :select
                               :required? false
                               :options   (for [domain-set @domain-sets]
                                            {:label (:domain-set/name domain-set)
                                             :value (:bp/uuid domain-set)})}
                              {:label     "BP6 Label"
                               :disabled? true
                               :field-key :variable/bp6-label}
                              {:label     "BP6 Code"
                               :disabled? true
                               :field-key :variable/bp6-code}
                              {:label     "Kind"
                               :field-key :variable/kind
                               :required? true
                               :type      :radio
                               :options   [{:label "Continuous" :value :continuous}
                                           {:label "Discrete" :value :discrete}
                                           {:label "Text" :value :text}]}
                              {:label     "Map Units Convertible?"
                               :field-key :variable/map-units-convertible?
                               :type      :checkbox
                               :options   [{:value true}]}]}]]

      [:div.col-6
       (when (#{:continuous :discrete} @kind)
         [:div
          [:h6 "Properties"]
          [properties-form @kind get-field set-field]])]]]))

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
