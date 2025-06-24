(ns behave-cms.components.table-entity-form
  (:require
   [behave-cms.components.common       :refer [simple-table]]
   [behave-cms.components.entity-form  :refer [entity-form]]
   [re-frame.core                      :as rf]
   [reagent.core                       :as r]))

(defn table-entity-form
  "A component that has simple table with a togglable entity-form. Use this whenever a parent entity has an attribute that has many refs to component entities."
  [{:keys [parent-id entities order-attr title entity-form-fields]}]
  (r/with-let [search-table-column-id-atom (r/atom nil)
               show-entity-form? (r/atom false)]
    [:div {:style {:display "flex"}}
     [simple-table
      [:variable/name]
      entities
      (cond-> {:caption               title
               :add-group-variable-fn #(swap! show-entity-form? not)
               :on-delete             #(rf/dispatch [:api/delete-entity (:db/id %)])
               :on-select             #(do
                                         (swap! show-entity-form? not)
                                         (reset! search-table-column-id-atom (:db/id %)))})
      order-attr (merge {:on-increase           #(rf/dispatch [:api/reorder % parent-id order-attr :inc])
                         :on-decrease           #(rf/dispatch [:api/reorder % parent-id order-attr :dec])})]
     (when @show-entity-form?
       [entity-form {:title        title
                     :id           @search-table-column-id-atom
                     :entity       :search-table-column
                     :parent-field :search-table/_columns
                     :parent-id    parent-id
                     :fields       entity-form-fields
                     :on-update    #(do (reset! search-table-column-id-atom nil) %)
                     :on-create    #(do
                                      (reset! search-table-column-id-atom nil)
                                      (swap! show-entity-form? not)
                                      (when order-attr
                                       (let [next-order (count entities)]
                                         (assoc % order-attr next-order))))}])]))
