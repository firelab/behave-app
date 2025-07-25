(ns behave-cms.tags.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(defn- on-select [selected-entity-id selected-state-path & [other-state-paths-to-clear]]
  #(if (= (:db/id %) selected-entity-id)
     (do (rf/dispatch [:state/set-state selected-state-path nil])
         (doseq [path other-state-paths-to-clear]
           (rf/dispatch [:state/set-state path nil])))
     (rf/dispatch [:state/set-state selected-state-path
                   @(rf/subscribe [:re-entity (:db/id %)])])))

(defn tags-table [selected-state-path editor-state-path selected-tag-set-path]
  (let [selected-entity  (rf/subscribe [:state selected-state-path])
        selected-tag-set (rf/subscribe [:state selected-tag-set-path])
        entities         (:tag-set/tags @selected-tag-set)]
    [table-entity-form
     {:title              "Tags"
      :form-state-path    editor-state-path
      :entity             :tag-set
      :entities           (sort-by :tag/name entities)
      :on-select          (on-select (:db/id selected-entity) selected-state-path)
      :parent-field       :tag-set/_tags
      :table-header-attrs [:tag/name]
      :order-attr         :tag/order
      :entity-form-fields [{:label     "Name"
                            :required? true
                            :field-key :tag/name}

                           {:label     "Color"
                            :type      :color
                            :disabled? (not (:tag-set/color? @selected-tag-set))
                            :field-key :tag/color}]}]))

(defn- tag-sets-table [selected-state-path editor-state-path & other-state-paths-to-clear]
  (let [selected-entity (rf/subscribe [:state selected-state-path])
        entities        (rf/subscribe [:pull-with-attr :tag-set/name])]
    [table-entity-form
     {:title              "Tag Sets"
      :form-state-path    editor-state-path
      :entity             :tag-set
      :entities           (sort-by :tag-set/name @entities)
      :on-select          (on-select (:db/id @selected-entity) selected-state-path other-state-paths-to-clear)
      :table-header-attrs [:tag-set/name]
      :entity-form-fields [{:label     "Name"
                            :required? true
                            :field-key :tag-set/name}

                           {:label     "Colored Tags?"
                            :type      :checkbox
                            :field-key :tag-set/color?
                            :options   [{:value true}]}]}]))

(defn tags-page
  "Page to manage Tag Sets and Tags"
  [_]
  (let [selected-tag-set-state-path [:selected :tag-set]
        tag-set-editor-path         [:editors  :tag-set]
        selected-tag-state-path     [:selected :tag]
        tag-editor-path             [:editors  :tag]
        selected-tag-set            (rf/subscribe [:state selected-tag-set-state-path])]
    [:div.container
     [:div {:style {:height "500px"}}
      [tag-sets-table selected-tag-set-state-path tag-set-editor-path selected-tag-state-path]]
     (when @selected-tag-set
       [:div {:style {:height "500px"}}
        [tags-table selected-tag-state-path tag-editor-path selected-tag-set-state-path]])]))
