(ns behave-cms.lists.views
  (:require [clojure.set :refer [rename-keys]]
            [re-frame.core                     :as rf]
            [behave-cms.events]
            [behave-cms.subs]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [reagent.core                       :as r]))

(defn list-lists-page [_]
  (r/with-let [selected-list-atom (r/atom nil)
               selected-list-option-atom (r/atom nil)]
    (let [loaded?         (rf/subscribe [:state :loaded?])
          tag-sets        (rf/subscribe [:pull-with-attr :tag-set/name])
          xform-tag-set   #(rename-keys % {:tag-set/name :label :db/id :value})
          color-tag-sets  (map xform-tag-set (filter :tag-set/color? @tag-sets))
          filter-tag-sets (map xform-tag-set (remove :tag-set/color? @tag-sets))]
      (if @loaded?
        [:div.container
         [:div {:style {:height "500px"}}
          [table-entity-form
           {:title              "Lists"
            :entity             :list
            :entities           (sort-by :list/name
                                         @(rf/subscribe [:pull-with-attr :list/name]))
            :on-select          #(reset! selected-list-atom @(rf/subscribe [:entity (:db/id %)]))
            :table-header-attrs [:list/name]
            :entity-form-fields [{:label     "Name"
                                  :required? true
                                  :field-key :list/name}
                                 {:label     "Filter Tag Set"
                                  :type      :ref-select
                                  :options   filter-tag-sets
                                  :field-key :list/tag-set}
                                 {:label     "Color Tag Set"
                                  :type      :ref-select
                                  :options   color-tag-sets
                                  :field-key :list/color-tag-set}]}]]
         (when @selected-list-atom
           (let [list-options                  (->> @selected-list-atom
                                                    :list/options
                                                    (map #(deref (rf/subscribe [:entity (:db/id %)]))))
                 tag-options                   (rf/subscribe [:list-option/tags-to-select (:db/id @selected-list-atom)])
                 color-tag-options             (rf/subscribe [:list-option/color-tags-to-select (:db/id @selected-list-atom)])
                 refresh-selected-list-atom-fn #(reset! selected-list-atom @(rf/subscribe [:entity (:db/id @selected-list-atom)]))]
             [:div {:style {:height "500px"}}
              [table-entity-form
               {:title              "List Options"
                :entity             :list-option
                :entities           list-options
                :on-select          #(reset! selected-list-option-atom @(rf/subscribe [:entity (:db/id %)]))
                :parent-id          (:db/id @selected-list-atom)
                :parent-field       :list/_options
                :on-create          refresh-selected-list-atom-fn
                :on-delete          refresh-selected-list-atom-fn
                :table-header-attrs [:list-option/name
                                     :list-option/value
                                     :list-option/order
                                     :list-option/default
                                     :list-option/tags
                                     :list-option/color-tag]
                :entity-form-fields [{:label     "Name"
                                      :required? true
                                      :field-key :list-option/name}
                                     {:label     "Order"
                                      :required? true
                                      :type      :number
                                      :field-key :list-option/order}
                                     {:label     "Index"
                                      :required? true
                                      :field-key :list-option/value}
                                     {:label     "Filter Tags"
                                      :type      :set
                                      :is-ref?   true
                                      :options   @tag-options
                                      :field-key :list-option/tag-refs}
                                     {:label     "Color Tag"
                                      :type      :ref-select
                                      :disabled? (nil? (:list/color-tag-set @selected-list-atom))
                                      :options   @color-tag-options
                                      :field-key :list-option/color-tag-ref}
                                     {:label     "Hide Option?"
                                      :type      :checkbox
                                      :field-key :list-option/hide?
                                      :options   [{:value true}]}
                                     {:label     "Default"
                                      :type      :radio
                                      :field-key :list-option/default
                                      :options   [{:label "False" :value false}
                                                  {:label "True" :value true}]}]}]]))]
        [:div "Loading..."]))))
