(ns behave-cms.domains.views
  (:require [re-frame.core                     :as rf]
            [reagent.core                       :as r]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn domains-page [_]
  (r/with-let [selected-entity-atom (r/atom nil)]
    (let [domain-set                      @(rf/subscribe [:pull-with-attr :domain-set/name])
          refersh-selected-entity-atom-fn #(reset! selected-entity-atom
                                                   @(rf/subscribe [:entity (:db/id @selected-entity-atom)]))]
      [:div.container
       [:div {:style {:height "500px"}}
        [table-entity-form
         {:title              "Domain Sets"
          :entity             :domain-set
          :entities           (sort-by :domain-set/name domain-set)
          :on-select          #(reset! selected-entity-atom @(rf/subscribe [:entity (:db/id %)]))
          :table-header-attrs [:domain-set/name]
          :entity-form-fields [{:label     "Name"
                                :required? true
                                :field-key :domain-set/name}]}]]
       (when @selected-entity-atom
         (let [domains (->> @selected-entity-atom
                            :domain-set/domains
                            (map #(deref (rf/subscribe [:entity (:db/id %)]))))]
           [:div {:style {:height "500px"}}
            [table-entity-form
             {:title              "Domains"
              :entity             :domain
              :entities           (sort-by :domain/name domains)
              :parent-id          (:db/id @selected-entity-atom)
              :parent-field       :domain-set/_domains
              :on-create          refersh-selected-entity-atom-fn
              :on-delete          refersh-selected-entity-atom-fn
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

                                   {:label          "Native Unit"
                                    :type           :units
                                    :required?      true
                                    :field-key      :domain/native-unit-uuid
                                    :dimension-attr :domain/dimension-uuid}

                                   {:label          "English Unit"
                                    :type           :units
                                    :required?      true
                                    :field-key      :domain/english-unit-uuid
                                    :dimension-attr :domain/dimension-uuid
                                    :unit-system    :english}

                                   {:label          "Metric Unit"
                                    :type           :units
                                    :required?      true
                                    :field-key      :domain/metric-unit-uuid
                                    :dimension-attr :domain/dimension-uuid
                                    :unit-system    :metric}

                                   {:label          "Filtered Units"
                                    :type           :units-set
                                    :required?      false
                                    :field-key      :domain/filtered-unit-uuids
                                    :dimension-attr :domain/dimension-uuid}]}]]))])))
