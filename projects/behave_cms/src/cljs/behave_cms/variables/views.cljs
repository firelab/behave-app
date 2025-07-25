(ns behave-cms.variables.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

;; helpers

(defn- on-select [selected-entity-id state-path]
  #(if (= (:db/id %) selected-entity-id)
     (rf/dispatch [:state/set-state state-path nil])
     (rf/dispatch [:state/set-state state-path
                   @(rf/subscribe [:re-entity (:db/id %)])])))

;; Variables
(defn list-variables-page
  "Page to manage Variables"
  [_]
  (if @(rf/subscribe [:state :loaded?])
    (let [selected-state-path [:selected :tool]
          editor-state-path   [:editors :tool]
          selected-entity     (rf/subscribe [:state selected-state-path])
          entities            (rf/subscribe [:pull-with-attr :variable/name])
          dimension-uuid      (or @(rf/subscribe [:state (conj editor-state-path :variable/dimension-uuid)])
                                  (:variable/dimension-uuid @selected-entity))
          dimension           @(rf/subscribe [:re-entity [:bp/uuid dimension-uuid]])
          units-in-dimension  (:dimension/units dimension)
          units->option       (fn [{unit-name :unit/name short-code :unit/short-code unit-uuid :bp/uuid}]
                                {:value unit-uuid
                                 :label (str unit-name " (" short-code ")")})]
      [:div.container
       {:style {:height "900px"}}
       [table-entity-form
        {:title              "Variables"
         :form-state-path    editor-state-path
         :entity             :variable
         :entities           (sort-by :variable/name @entities)
         :on-select          (on-select (:db/id selected-entity) selected-state-path)
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

                              {:label     "Native Units"
                               :type      :select
                               :field-key :variable/native-unit-uuid
                               :required? false
                               :disabled? (empty? units-in-dimension)
                               :options   (->> units-in-dimension
                                               (map units->option))}

                              {:label     "Native Decimals"
                               :field-key :variable/native-decimals
                               :type      :number
                               :required? false}

                              {:label     "English Units"
                               :type      :select
                               :field-key :variable/english-unit-uuid
                               :required? true
                               :disabled? (empty? units-in-dimension)
                               :options   (->> units-in-dimension
                                               (filter #(= (:unit/system %) :english))
                                               (map units->option))}

                              {:label     "English Decimals"
                               :field-key :variable/metric-decimals
                               :type      :number
                               :required? false}

                              {:label     "Metric Units"
                               :type      :select
                               :field-key :variable/english-unit-uuid
                               :required? false
                               :disabled? (empty? units-in-dimension)
                               :options   (->> units-in-dimension
                                               (filter #(= (:unit/system %) :metric))
                                               (map units->option))}

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
                               :options   [{:value true}]}]}]])
    [:div "Loading..."]))
