(ns behave-cms.domains.views
  (:require [re-frame.core                     :as rf]
            [reagent.core                       :as r]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn domains-page
  "Page to manage Domain Sets and Domains"
  [_]
  (r/with-let [selected-domain-set-atom (r/atom nil)
               selected-domain-atom (r/atom nil)]
    (let [domain-set                          @(rf/subscribe [:pull-with-attr :domain-set/name])
          refersh-selected-domain-set-atom-fn #(reset! selected-domain-set-atom
                                                       @(rf/subscribe [:touch-entity (:db/id @selected-domain-set-atom)]))]
      [:div.container
       [:div {:style {:height "500px"}}
        [table-entity-form
         {:title              "Domain Sets"
          :entity             :domain-set
          :entities           (sort-by :domain-set/name domain-set)
          :on-select          #(reset! selected-domain-set-atom @(rf/subscribe [:touch-entity (:db/id %)]))
          :table-header-attrs [:domain-set/name]
          :entity-form-fields [{:label     "Name"
                                :required? true
                                :field-key :domain-set/name}]}]]
       (when @selected-domain-set-atom
         (let [domains            (:domain-set/domains @selected-domain-set-atom)
               dimension-uuid     (or @(rf/subscribe [:state (cond-> [:editors
                                                                      :domain
                                                                      (:db/id @selected-domain-set-atom)
                                                                      (:db/id @selected-domain-atom)
                                                                      :domain/dimension-uuid])])
                                      (:domain/dimension-uuid @selected-domain-atom))
               dimension          @(rf/subscribe [:touch-entity [:bp/uuid dimension-uuid]])
               units-in-dimension (:dimension/units dimension)
               units->option      (fn [{unit-name :unit/name short-code :unit/short-code unit-uuid :bp/uuid}]
                                    {:value unit-uuid
                                     :label (str unit-name " (" short-code ")")})]
           [:div {:style {:height "500px"}}
            [table-entity-form
             {:title              "Domains"
              :entity             :domain
              :entities           (sort-by :domain/name domains)
              :on-select          #(reset! selected-domain-atom @(rf/subscribe [:touch-entity (:db/id %)]))
              :parent-id          (:db/id @selected-domain-set-atom)
              :parent-field       :domain-set/_domains
              :on-create          refersh-selected-domain-set-atom-fn
              :on-delete          refersh-selected-domain-set-atom-fn
              :table-header-attrs [:domain/name]
              :entity-form-fields [{:label     "Name"
                                    :required? true
                                    :field-key :domain/name}

                                   {:label     "Decimals"
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
                                                    (map units->option))}]}]]))])))
