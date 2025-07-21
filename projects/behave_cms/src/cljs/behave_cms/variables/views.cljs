(ns behave-cms.variables.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn list-variables-page [_]
  (let [loaded? (rf/subscribe [:state :loaded?])]
    (if @loaded?
      [:div.container
       [table-entity-form
        {:title              "Variables"
         :entity             :variable
         :entities           (sort-by :variable/name
                                      @(rf/subscribe [:pull-with-attr :variable/name]))
         :table-header-attrs [:variable/name
                              :variable/domain-uuid
                              :variable/bp6-label
                              :variable/bp6-code]
         :entity-form-fields [{:label     "Name"
                               :required? true
                               :field-key :variable/name}

                              {:label     "Kind"
                               :field-key :variable/kind
                               :required? true
                               :type      :radio
                               :options   [{:label "Continuous" :value :continuous}
                                           {:label "Discrete" :value :discrete}
                                           {:label "Text" :value :text}]}

                              {:label     "Domain"
                               :field-key :variable/domain-uuid
                               :type      :select
                               :required? false
                               :options   (for [domain @(rf/subscribe [:domains])]
                                            {:label (:domain/name domain)
                                             :value (:bp/uuid domain)})}

                              {:label     "Dimension"
                               :field-key :variable/dimension-uuid
                               :type      :select
                               :required? false
                               :options   (for [dimension @(rf/subscribe [:dimensions])]
                                            {:label (:dimension/name dimension)
                                             :value (:bp/uuid dimension)})}

                              {:label          "English Units"
                               :field-key      :variable/english-units-uuid
                               :type           :units
                               :dimension-attr :variable/dimension-uuid
                               :unit-system    :english}

                              {:label     "English Decimals"
                               :field-key :variable/metric-decimals
                               :type      :number
                               :required? false}

                              {:label          "Native Units"
                               :field-key      :variable/native-units-uuid
                               :type           :units
                               :dimension-attr :variable/dimension-uuid}

                              {:label     "Native Decimals"
                               :field-key :variable/native-decimals
                               :type      :number
                               :required? false}

                              {:label          "Metric Units"
                               :field-key      :variable/metric-units-uuid
                               :type           :units
                               :dimension-attr :variable/dimension-uuid
                               :unit-system    :metric}

                              {:label     "Metric Decimals"
                               :field-key :variable/metric-decimals
                               :type      :number
                               :required? false}

                              {:label     "Maximum"
                               :field-key :variable/maximum
                               :type      :number
                               :required? false}

                              {:label     "Minimum"
                               :field-key :variable/minimum
                               :type      :number
                               :required? false}

                              {:label     "BP6 Label"
                               :disabled? true
                               :field-key :variable/bp6-label}

                              {:label     "BP6 Code"
                               :disabled? true
                               :field-key :variable/bp6-code}



                              {:label     "Map Units Convertible?"
                               :field-key :variable/map-units-convertible?
                               :type      :checkbox
                               :options   [{:value true}]}
                              ]}]]
      [:div "Loading..."])))
