(ns behave-cms.domains.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn domains-page
  "Page to manage Domain Sets and Domains"
  [_]
  (let [selected-domain-set-state-path [:selected :domain-set]
        selected-domain-state-path     [:selected :domain]
        domain-set-editor-path         [:editors  :domain-set]
        domain-editor-path             [:editors  :domain]
        selected-domain-set            (rf/subscribe [:state selected-domain-set-state-path])
        selected-domain                (rf/subscribe [:state selected-domain-state-path])
        domain-sets                    (rf/subscribe [:pull-with-attr :domain-set/name])]
    [:div.container
     [:div {:style {:height "400px"}}
      [table-entity-form
       {:title              "Domain Sets"
        :form-state-path    domain-set-editor-path
        :entity             :domain-set
        :entities           (sort-by :domain-set/name @domain-sets)
        :on-select          #(if (= (:db/id %) (:db/id @selected-domain-set))
                               (do (rf/dispatch [:state/set-state selected-domain-set-state-path nil])
                                   (rf/dispatch [:state/set-state selected-domain-state-path nil]))
                               (rf/dispatch [:state/set-state selected-domain-set-state-path
                                             @(rf/subscribe [:re-entity (:db/id %)])]))
        :table-header-attrs [:domain-set/name]
        :entity-form-fields [{:label     "Name"
                              :required? true
                              :field-key :domain-set/name}]}]]
     (when @selected-domain-set
       (let [domains            (:domain-set/domains @selected-domain-set)
             dimension-uuid     (or @(rf/subscribe [:state (conj domain-editor-path :domain/dimension-uuid)])
                                    (:domain/dimension-uuid @selected-domain))
             dimension          @(rf/subscribe [:re-entity [:bp/uuid dimension-uuid]])
             units-in-dimension (:dimension/units dimension)
             units->option      (fn [{unit-name :unit/name short-code :unit/short-code unit-uuid :bp/uuid}]
                                  {:value unit-uuid
                                   :label (str unit-name " (" short-code ")")})]
         [:div {:style {:height "500px"}}
          [table-entity-form
           {:title              "Domains"
            :form-state-path    domain-editor-path
            :entity             :domain
            :entities           (sort-by :domain/name domains)
            :on-select          #(rf/dispatch [:state/set-state selected-domain-state-path
                                               @(rf/subscribe [:re-entity (:db/id %)])])
            :parent-id          (:db/id @selected-domain-set)
            :parent-field       :domain-set/_domains
            :table-header-attrs [:domain/name]
            :entity-form-fields [{:label     "Name"
                                  :required? true
                                  :field-key :domain/name}

                                 {:label     "Decimals"
                                  :type      :number
                                  :field-key :domain/decimals}

                                 {:label     "Dimension"
                                  :type      :select
                                  :field-key :domain/dimension-uuid
                                  :options   (for [dimension @(rf/subscribe [:dimensions])]
                                               {:label (:dimension/name dimension)
                                                :value (:bp/uuid dimension)})}

                                 {:label     "Native Units"
                                  :type      :select
                                  :field-key :domain/native-unit-uuid
                                  :required? true
                                  :disabled? (empty? units-in-dimension)
                                  :options   (->> units-in-dimension
                                                  (map units->option))}

                                 {:label     "English Units"
                                  :type      :select
                                  :field-key :domain/english-unit-uuid
                                  :required? true
                                  :disabled? (empty? units-in-dimension)
                                  :options   (->> units-in-dimension
                                                  (filter #(= (:unit/system %) :english))
                                                  (map units->option))}

                                 {:label     "Metric Units"
                                  :type      :select
                                  :field-key :domain/english-unit-uuid
                                  :required? true
                                  :disabled? (empty? units-in-dimension)
                                  :options   (->> units-in-dimension
                                                  (filter #(= (:unit/system %) :metric))
                                                  (map units->option))}

                                 {:label     "Filtered Units"
                                  :type      :set
                                  :required? false
                                  :field-key :domain/filtered-unit-uuids
                                  :disabled? (empty? units-in-dimension)
                                  :options   (->> units-in-dimension
                                                  (map units->option))}]}]]))]))
