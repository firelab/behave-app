(ns behave-cms.lists.views
  (:require
   [clojure.set                        :refer [rename-keys]]
   [re-frame.core                      :as rf]
   [behave-cms.components.common       :refer [simple-table]]
   [behave-cms.components.entity-form  :refer [entity-form]]
   [behave-cms.events]
   [behave-cms.subs]
   [behave-cms.components.translations :refer [all-translations]]
   [string-utils.interface             :refer [->kebab]]))

(defn- lists-table []
  (let [lists     (rf/subscribe [:pull-with-attr :list/name '[* {:list/options [*]}]])
        on-select #(do
                     (rf/dispatch [:state/set-state :list %])
                     (rf/dispatch [:state/set-state :list-option nil]))
        on-delete #(when (js/confirm (str "Are you sure you want to delete the list " (:list/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))]
    [simple-table
     [:list/name]
     (sort-by :list/name @lists)
     {:on-select on-select
      :on-delete on-delete}]))

(defn- list-options-table [llist]
  (let [list-options (->> (rf/subscribe [:pull-children :list/options (:db/id llist)])
                          deref
                          (map (fn [option]
                                 (assoc option
                                        :list-option/tags @(rf/subscribe [:list-option/tags (:db/id option)])
                                        :list-option/color-tag @(rf/subscribe [:list-option/color-tag (:db/id option)])))))
        on-select    #(rf/dispatch [:state/set-state :list-option %])
        on-delete    #(when (js/confirm (str "Are you sure you want to delete the list " (:list-option/name %) "?"))
                        (rf/dispatch [:api/delete-entity %])
                        (rf/dispatch [:state/set-state :list-option nil]))]
    [simple-table
     [:list-option/name :list-option/value :list-option/order
      :list-option/default :list-option/tags :list-option/color-tag]
     (sort-by :list-option/order list-options)
     {:on-select   on-select
      :on-delete   on-delete
      :on-increase #(rf/dispatch [:api/reorder % list-options :list-option/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % list-options :list-option/order :dec])}]))

(defn- list-option-form [llist]
  (let [list-options      (rf/subscribe [:pull-children :list/options (:db/id llist)])
        *list-option      (rf/subscribe [:state :list-option])
        tag-options       (rf/subscribe [:list-option/tags-to-select (:db/id llist)])
        color-tag-options (rf/subscribe [:list-option/color-tags-to-select (:db/id llist)])]
    [:<>
     [:h3 (if @*list-option "Edit Option" "Add Option")]
     [entity-form {:entity       :list-options
                   :parent-field :list/_options
                   :parent-id    (:db/id llist)
                   :id           (:db/id @*list-option)
                   :on-create    #(-> %
                                      (assoc :list-option/order (count @list-options))
                                      (assoc :list-option/translation-key (->kebab (str "behaveplus:list-option:" (:list/name llist) ":" (:list-option/name %))))
                                      (assoc :list-option/result-translation-key (->kebab (str "behaveplus:list-option:result:" (:list/name llist) ":" (:list-option/name %))))
                                      (assoc :list-option/export-translation-key (->kebab (str "behaveplus:list-option:export:" (:list/name llist) ":" (:list-option/name %)))))
                   :fields       [{:label     "Name"
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
                                   :disabled? (nil? (:list/color-tag-set llist))
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
                                               {:label "True" :value true}]}]}]
     [:h4 "Worksheet Translation"]
     [all-translations (:list-option/translation-key @*list-option)]
     [:h4 "Result Translation"]
     [all-translations (:list-option/result-translation-key @*list-option)]
     [:h4 "Export Translation"]
     [all-translations (:list-option/export-translation-key @*list-option)]
     [:h4 "English Translation"]
     [all-translations (:list-option/english-units-translation-key @*list-option)]
     [:h4 "Metric Translation"]
     [all-translations (:list-option/metric-units-translation-key @*list-option)]]))

(defn- list-form [llist]
  (let [tag-sets        (rf/subscribe [:pull-with-attr :tag-set/name])
        xform-tag-set   #(rename-keys % {:tag-set/name :label :db/id :value})
        color-tag-sets  (map xform-tag-set (filter :tag-set/color? @tag-sets))
        filter-tag-sets (map xform-tag-set (remove :tag-set/color? @tag-sets))]
    [:<>
     [:div.row
      [:h3 (if llist (str "Edit " (:list/name llist)) "Add List")]]
     [:div.row
      [:div.col-3
       [entity-form {:entity :lists
                     :id     (when llist (:db/id llist))
                     :fields [{:label     "Name"
                               :required? true
                               :field-key :list/name}
                              {:label     "Filter Tag Set"
                               :type      :ref-select
                               :options   filter-tag-sets
                               :field-key :list/tag-set}
                              {:label     "Color Tag Set"
                               :type      :ref-select
                               :options   color-tag-sets
                               :field-key :list/color-tag-set}]}]]]
     [:div.row
      [:div.col-8
       [:h4 "All Options"]
       [:div {:style {:height "800px"}}
        [list-options-table llist]]]
      [:div.col-4
       [list-option-form llist]]]]))

(defn list-lists-page [_]
  (let [loaded? (rf/subscribe [:state :loaded?])
        *list   (rf/subscribe [:state :list])]
    (if @loaded?
      [:div.container
       [:div.row.my-3
        [:div.col-12
         [:h3 "Lists"]
         [:div
          {:style {:height "400px"}}
          [lists-table]]]]
       [list-form @*list]]
      [:div "Loading..."])))
