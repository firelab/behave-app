(ns behave-cms.domains.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form on-select]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn- domains-table [selected-state-path editor-state-path selected-domain-set-path]
  (let [selected-entity     (rf/subscribe [:state selected-state-path])
        selected-domain-set (rf/subscribe [:state selected-domain-set-path])
        domains             (:domain-set/domains @selected-domain-set)
        dimension-uuid      (or @(rf/subscribe [:state (conj editor-state-path :domain/dimension-uuid)])
                                (:domain/dimension-uuid @selected-entity))
        dimension           @(rf/subscribe [:re-entity [:bp/uuid dimension-uuid]])
        units-in-dimension  (:dimension/units dimension)
        units->option       (fn [{unit-name :unit/name short-code :unit/short-code unit-uuid :bp/uuid}]
                              {:value unit-uuid
                               :label (str unit-name " (" short-code ")")})]
    [:div {:style {:height "500px"}}
     [table-entity-form
      {:title              "Domains"
       :form-state-path    editor-state-path
       :entity             :domain
       :entities           (sort-by :domain/name domains)
       :on-select          (on-select selected-state-path)
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
                                             (map units->option))}]}]]))

(defn- domain-sets-table [selected-state-path editor-state-path & other-state-paths-to-clear]
  (let [entities (rf/subscribe [:pull-with-attr :domain-set/name])]
    [table-entity-form
     {:title              "Domain Sets"
      :form-state-path    editor-state-path
      :entity             :domain-set
      :entities           (sort-by :domain-set/name @entities)
      :on-select          (on-select selected-state-path other-state-paths-to-clear)
      :table-header-attrs [:domain-set/name]
      :entity-form-fields [{:label     "Name"
                            :required? true
                            :field-key :domain-set/name}]}]))

(defn domains-page
  "Page to manage Domain Sets and Domains"
  [_]
  (let [selected-domain-set-state-path [:selected :domain-set]
        domain-set-editor-path         [:editors  :domain-set]
        selected-domain-state-path     [:selected :domain]
        domain-editor-state-path       [:editors  :domain]
        selected-domain-set            (rf/subscribe [:state selected-domain-set-state-path])]
    [:div.container
     [:div {:style {:height "400px"}}
      [domain-sets-table
       selected-domain-set-state-path
       domain-set-editor-path
       selected-domain-state-path]]
     (when @selected-domain-set
       [:div {:style {:height "500px"}}
        [domains-table
         selected-domain-state-path
         domain-editor-state-path
         selected-domain-set-state-path]])]))
