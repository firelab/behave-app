(ns behave-cms.tags.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.common      :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.events]
            [behave-cms.subs]
            [string-utils.interface :refer [->kebab]]))

;;; Helpers

(defn- upsert-translation [t-key translation]
  (let [english-eid @(rf/subscribe [:language/english-eid])]
    {:translation/key         t-key
     :language/_translation   english-eid
     :translation/translation translation}))

(defn- tag-editor [tag-set-eid tag-eid]
  (let [tag-set (rf/subscribe [:entity tag-set-eid])
        tags    (rf/subscribe [:pull-children :tag-set/tags tag-set-eid])]
    [:<>
     [:h3 (if tag-eid "Edit Tag" "Add Tag")]
     [entity-form {:entity       :tag
                   :parent-field :tag-set/_tags
                   :parent-id    tag-set-eid
                   :id           tag-eid
                   :fields       [{:label     "Name"
                                   :required? true
                                   :field-key :tag/name}

                                  {:label     "Color"
                                   :type      :color
                                   :disabled? (not (:tag-set/color? @tag-set))
                                   :field-key :tag/color}]
                   :on-create    (fn [data]
                                   (let [translation (upsert-translation (:tag/translation-key data) (:tag/name data))]
                                     (rf/dispatch [:api/create-entity translation])
                                     (merge data {:tag/order (count @tags)})))
                   :on-update    (fn [data]
                                   (if (:tag/name data)
                                     (let [translation (upsert-translation (:tag/translation-key data) (:tag/name data))]
                                       (rf/dispatch [:api/create-entity translation])
                                       data)
                                     data))}]]))

(defn- tags-table [tag-set-eid]
  (when tag-set-eid
    (let [tags      (rf/subscribe [:pull-children :tag-set/tags tag-set-eid])
          on-select #(rf/dispatch [:state/select :tags (:db/id %)])
          on-delete
          #(when (js/confirm (str "Are you sure you want to delete the tag " (:tag/name %) "?"))
             (rf/dispatch [:api/delete-entity %]))]
      [:div
       {:style {:height "400px"}}
       [simple-table
        [:tag/name]
        (sort-by :tag/order @tags)
        {:on-select on-select
         :on-delete on-delete
         :on-increase #(rf/dispatch [:api/reorder % @tags :tag/order :inc])
         :on-decrease #(rf/dispatch [:api/reorder % @tags :tag/order :dec])}]])))

;;; Dimension

(defn- tag-set-editor [tag-set-eid tag-eid]
  [:<>
   [:div.row
    [:h3 (str (if tag-set-eid "Edit" "Add") " Tag-Set")]]
   [:div.row
    [:div.col-6
     [entity-form {:entity    :tag-sets
                   :id        tag-set-eid
                   :disabled? (boolean tag-eid)
                   :fields    [{:label     "Name"
                                :required? true
                                :field-key :tag-set/name}

                               {:label     "Colored Tags?"
                                :type      :checkbox
                                :field-key :tag-set/color?
                                :options   [{:value true}]}]
                   :on-create (fn [data]
                                (let [translation-key (str "behaveplus:tags:" (->kebab (:tag-set/name data)))
                                      translation     (upsert-translation translation-key (:tag-set/name data))]
                                  (rf/dispatch [:api/create-entity translation])
                                  (merge data {:tag-set/translation-key translation-key})))}]]]])

(defn- tag-sets-table []
  (let [tag-sets (rf/subscribe [:pull-with-attr :tag-set/name])
        on-select  #(rf/dispatch [:state/select :tag-sets (:db/id %)])
        on-delete  #(when (js/confirm (str "Are you sure you want to delete the tag set " (:tag-set/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))]
    [:div
     {:style {:height "400px"}}
     [simple-table
      [:tag-set/name]
      (sort-by :tag-set/name @tag-sets)
      {:on-select on-select
       :on-delete on-delete}]]))

(defn tags-page [_]
  (let [*tag-set (rf/subscribe [:selected :tag-sets])
        *tag      (rf/subscribe [:selected :tags])]
    [:div.container
     [:div.row.mt-3
      [:h3 "Tag-Sets / Tags"]
      [:div.row
       [:div.col-6
        [tag-sets-table]]
       [:div.col-6
        [tags-table @*tag-set]]]]
     [:div.row.mt-3
      [:div.col-6
       [tag-set-editor @*tag-set @*tag]]
      [:div.col-6
       (when @*tag-set
         [tag-editor @*tag-set @*tag])]]]))
