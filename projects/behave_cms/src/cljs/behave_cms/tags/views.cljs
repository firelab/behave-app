(ns behave-cms.tags.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]
            [reagent.core :as r]))

(defn tags-page
  "Page to manage Tag Sets and Tags"
  [_]
  (r/with-let [selected-tag-set-atom (r/atom nil)
               selected-tag-atom (r/atom nil)]
    (let [tag-sets                      @(rf/subscribe [:pull-with-attr :tag-set/name])
          refresh-selected-list-atom-fn #(reset! selected-tag-set-atom
                                                 @(rf/subscribe [:re-entity (:db/id @selected-tag-set-atom)]))]
      [:div.container
       [:div {:style {:height "500px"}}
        [table-entity-form
         {:title              "Tag Sets"
          :entity             :tag-set
          :entities           (sort-by :tag-set/name tag-sets)
          :on-select          #(if (= (:db/id %) (:db/id @selected-tag-set-atom))
                                 (reset! selected-tag-set-atom nil)
                                 (reset! selected-tag-set-atom @(rf/subscribe [:re-entity (:db/id %)])))
          :table-header-attrs [:tag-set/name]
          :entity-form-fields [{:label     "Name"
                                :required? true
                                :field-key :tag-set/name}

                               {:label     "Colored Tags?"
                                :type      :checkbox
                                :field-key :tag-set/color?
                                :options   [{:value true}]}]}]]
       (when @selected-tag-set-atom
         (let [tags (:tag-set/tags @selected-tag-set-atom)]
           [:div {:style {:height "500px"}}
            [table-entity-form
             {:title              "Tags"
              :entity             :tag-set
              :entities           (sort-by :tag/name tags)
              :on-select          #(reset! selected-tag-atom @(rf/subscribe [:re-entity (:db/id %)]))
              :parent-id          (:db/id @selected-tag-set-atom)
              :parent-field       :tag-set/_tags
              :on-create          refresh-selected-list-atom-fn
              :on-delete          refresh-selected-list-atom-fn
              :table-header-attrs [:tag/name]
              :order-attr         :tag/order
              :entity-form-fields [{:label     "Name"
                                    :required? true
                                    :field-key :tag/name}

                                   {:label     "Color"
                                    :type      :color
                                    :disabled? (:tag-set/color? @selected-tag-set-atom)
                                    :field-key :tag/color}]}]]))])))
