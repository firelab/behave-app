(ns behave-cms.tags.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn tags-page
  "Page to manage Tag Sets and Tags"
  [_]
  (let [selected-tag-set-state-path [:selected :tag-set]
        selected-tag-state-path     [:selected :tag]
        tag-set-editor-path         [:editors  :tag-set]
        tag-editor-path             [:editors  :tag]
        selected-tag-set            (rf/subscribe [:state selected-tag-set-state-path])
        tag-sets                    (rf/subscribe [:pull-with-attr :tag-set/name])]
    [:div.container
     [:div {:style {:height "500px"}}
      [table-entity-form
       {:title              "Tag Sets"
        :form-state-path    tag-set-editor-path
        :entity             :tag-set
        :entities           (sort-by :tag-set/name @tag-sets)
        :on-select          #(if (= (:db/id %) (:db/id @selected-tag-set))
                               (do (rf/dispatch [:state/set-state selected-tag-set-state-path nil])
                                   (rf/dispatch [:state/set-state selected-tag-state-path nil]))
                               (rf/dispatch [:state/set-state selected-tag-set-state-path
                                             @(rf/subscribe [:re-entity (:db/id %)])]))
        :table-header-attrs [:tag-set/name]
        :entity-form-fields [{:label     "Name"
                              :required? true
                              :field-key :tag-set/name}

                             {:label     "Colored Tags?"
                              :type      :checkbox
                              :field-key :tag-set/color?
                              :options   [{:value true}]}]}]]
     (when @selected-tag-set
       (let [tags (:tag-set/tags @selected-tag-set)]
         [:div {:style {:height "500px"}}
          [table-entity-form
           {:title              "Tags"
            :form-state-path    tag-editor-path
            :entity             :tag-set
            :entities           (sort-by :tag/name tags)
            :on-select          #(rf/dispatch [:state/set-state selected-tag-state-path
                                               @(rf/subscribe [:re-entity (:db/id %)])])
            :parent-id          (:db/id @selected-tag-set)
            :parent-field       :tag-set/_tags
            :table-header-attrs [:tag/name]
            :order-attr         :tag/order
            :entity-form-fields [{:label     "Name"
                                  :required? true
                                  :field-key :tag/name}

                                 {:label     "Color"
                                  :type      :color
                                  :disabled? (not (:tag-set/color? @selected-tag-set))
                                  :field-key :tag/color}]}]]))]))
