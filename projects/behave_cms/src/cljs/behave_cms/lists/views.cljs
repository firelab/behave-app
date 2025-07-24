(ns behave-cms.lists.views
  (:require [clojure.set :refer [rename-keys]]
            [re-frame.core                     :as rf]
            [behave-cms.events]
            [behave-cms.subs]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]))

(defn list-lists-page
  "Page for managing Lists and List Options"
  [_]
  (if @(rf/subscribe [:state :loaded?])
    (let [selected-list-state-path        [:selected :list]
          selected-list-option-state-path [:selected :list-option]
          list-editor-path                [:editors  :list]
          list-option-editor-path         [:editors  :list-option]
          selected-list                   (rf/subscribe [:state selected-list-state-path])
          selected-list-option            (rf/subscribe [:state selected-list-option-state-path])
          tag-sets                        (rf/subscribe [:pull-with-attr :tag-set/name])
          xform-tag-set                   #(rename-keys % {:tag-set/name :label :db/id :value})
          color-tag-sets                  (map xform-tag-set (filter :tag-set/color? @tag-sets))
          filter-tag-sets                 (map xform-tag-set (remove :tag-set/color? @tag-sets))]
      [:div.container
       [:div {:style {:height "500px"}}
        [table-entity-form
         {:title              "Lists"
          :form-state-path    list-editor-path
          :entity             :list
          :entities           (sort-by :list/name
                                       @(rf/subscribe [:pull-with-attr :list/name]))
          :on-select          #(if (= (:db/id %) (:db/id @selected-list))
                                 (do (rf/dispatch [:state/set-state selected-list-state-path nil])
                                     (rf/dispatch [:state/set-state selected-list-state-path nil]))
                                 (rf/dispatch [:state/set-state selected-list-state-path
                                               @(rf/subscribe [:re-entity (:db/id %)])]))
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
       (when @selected-list
         (let [list-options      (->> (rf/subscribe [:pull-children :list/options (:db/id @selected-list)])
                                      deref
                                      (map (fn [option]
                                             (assoc option
                                                    :list-option/tags      @(rf/subscribe [:list-option/tags (:db/id option)])
                                                    :list-option/color-tag @(rf/subscribe [:list-option/color-tag (:db/id option)])))))
               tag-options       (rf/subscribe [:list-option/tags-to-select (:db/id @selected-list)])
               color-tag-options (rf/subscribe [:list-option/color-tags-to-select (:db/id @selected-list)])]
           [:div {:style {:height "500px"}}
            [table-entity-form
             {:title              "List Options"
              :form-state-path    list-option-editor-path
              :entity             :list-option
              :entities           list-options
              :on-select          #(rf/dispatch [:state/set-state selected-list-option
                                                 @(rf/subscribe [:re-entity (:db/id %)])])
              :parent-id          (:db/id @selected-list)
              :parent-field       :list/_options
              :order-attr         :list-option/order
              :table-header-attrs [:list-option/name
                                   :list-option/value
                                   :list-option/default
                                   :list-option/tags
                                   :list-option/color-tag]
              :entity-form-fields [{:label     "Name"
                                    :required? true
                                    :field-key :list-option/name}
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
                                    :disabled? (nil? (:list/color-tag-set @selected-list))
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
                                                {:label "True" :value true}]}]}]]))])
    [:div "Loading..."]))
