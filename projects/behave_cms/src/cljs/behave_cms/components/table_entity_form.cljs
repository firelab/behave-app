(ns behave-cms.components.table-entity-form
  (:require
   [behave-cms.components.common       :refer [simple-table]]
   [behave-cms.components.entity-form  :refer [entity-form]]
   [behave-cms.components.translations :refer [all-translations]]
   [re-frame.core                      :as rf]
   [reagent.core                       :as r]))

(defn table-entity-form-on-select
  "On select function to be passed into table-entity-form. Returns a function that expects an entity. Function will also update the `selected-state-path` as well as clearing the state of any related paths (`other-state-paths-to-clear`)"
  [selected-state-path & other-state-paths-to-clear]
  #(let [selected-entity-id (:db/id @(rf/subscribe [:state selected-state-path]))]
     (if (= (:db/id %) selected-entity-id)
       (do (rf/dispatch [:state/set-state selected-state-path nil])
           (doseq [path other-state-paths-to-clear]
             (rf/dispatch [:state/set-state path nil])))
       (rf/dispatch [:state/set-state selected-state-path
                     @(rf/subscribe [:re-entity (:db/id %)])]))))

(defn- create-translation! [data entity {:keys [key-fn text-fn]}]
  (let [t-key       (if (keyword? key-fn) (get data key-fn) (key-fn data))
        t-text      (if (keyword? text-fn) (get data text-fn) (text-fn data))
        english-eid @(rf/subscribe [:language/english-eid])
        key-attr    (keyword (name entity) "translation-key")]
    (rf/dispatch [:api/create-entity {:translation/key         t-key
                                      :language/_translation   english-eid
                                      :translation/translation t-text}])
    (assoc data key-attr t-key)))

(defn table-entity-form
  "A component that has simple table with a togglable entity-form. Use this
  whenever a parent entity has an attribute that has many refs to component
  entities.

  Takes:
  `title` - (optional) title of the table

  `entity` - keyword of the entity (i.e. :submodule). Primarly used as a prefix
  to construct additional key attributes (i.e. :submodule/translation-key)

  `entities` - list of entities that already exists. This will populate the table.

  `table-header-attrs` - (optional) list of attributes of the entities to use to
  construct the columns of the table. Defaults to all attributes if ommitted.

  `entity-form-fields` - list of field params as specified in `behave-cms.components.common/simple-table`

  `parent-id` - Entity id of the parent

  `parent-field` - parent field (i.e. :module/_submodules)

  `order-attr` - (optional) if entities have an order attribute this will
  display buttons to re order the list in the table.

  `translation-config` - (optional) map with `:key-fn` and `:text-fn` (each a keyword
  or fn from entity data -> string). When present, a translation entity is automatically
  created on entity creation and `:<entity>/translation-key` is assoc'd onto the data.

  `translation-attrs` - (optional) vector of `{:label \"...\" :attr :entity/translation-key}`
  maps. When an existing entity is selected, renders an [all-translations] table for
  each attr that has a non-nil value on the entity.

  "
  [{:keys [title entity entities table-header-attrs entity-form-fields parent-id parent-field order-attr form-state-path on-select translation-config translation-attrs]}]
  (r/with-let [entity-id-atom    (r/atom nil)
               show-entity-form? (r/atom false)]
    [:div {:style {:display "flex"
                   :height  "100%"
                   :padding "30px"}}
     [:div {:style {:padding-right "10px"
                    :width         "100%"}}
      [simple-table
       (if (seq table-header-attrs)
         table-header-attrs
         (map :field-key entity-form-fields))
       (if order-attr (sort-by order-attr entities) entities)
       (cond-> {:add-entity-fn #(do (when (nil? @entity-id-atom) (swap! show-entity-form? not))
                                    (rf/dispatch [:state/set-state :editors {}])
                                    (reset! entity-id-atom nil)
                                    (when on-select (on-select %)))
                :on-delete     #(when (js/confirm (str "Are you sure you want to delete this "
                                                       (name entity)))
                                  (rf/dispatch-sync [:api/delete-entity (:db/id %)]))
                :on-select     #(if (and @show-entity-form? (= @entity-id-atom (:db/id %)))
                                  (do (reset! entity-id-atom nil)
                                      (reset! show-entity-form? false)
                                      (rf/dispatch [:state/set-state :editors {}])
                                      (when on-select (on-select %)))
                                  (do
                                    (reset! show-entity-form? true)
                                    (reset! entity-id-atom (:db/id %))
                                    (when on-select (on-select %))))}
         title      (assoc :caption title)
         order-attr (merge {:on-increase #(rf/dispatch [:api/reorder % entities order-attr :inc])
                            :on-decrease #(rf/dispatch [:api/reorder % entities order-attr :dec])}))]]
     (when @show-entity-form?
       [:div {:style {:height     "100%"
                      :overflow-y "auto"}}
        [entity-form {:title        title
                      :state-path   form-state-path
                      :id           @entity-id-atom
                      :entity       entity
                      :parent-field parent-field
                      :parent-id    parent-id
                      :fields       entity-form-fields
                      :on-update    #(do (reset! entity-id-atom nil) %)
                      :on-create    #(do
                                       (reset! entity-id-atom nil)
                                       (swap! show-entity-form? not)
                                       (cond-> %
                                         translation-config (create-translation! entity translation-config)
                                         order-attr         (assoc order-attr (count entities))))}]
        (when (and (seq translation-attrs) @entity-id-atom)
          (let [entity-data @(rf/subscribe [:re-entity @entity-id-atom])]
            [:<>
             (for [{:keys [label attr]} translation-attrs
                   :let                 [t-key (get entity-data attr)]
                   :when                t-key]
               ^{:key (str attr)}
               [:<>
                [:h4 label]
                [all-translations t-key]])]))])]))
