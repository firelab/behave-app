(ns behave-cms.lists.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.common      :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.events]
            [behave-cms.subs]
            [behave-cms.components.translations :refer [all-translations]]
            [clojure.string :as str]))

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

(defn- color-tags-table [llist]
  (let [color-tags (->> (rf/subscribe [:pull-children :list/color-tags (:db/id llist)])
                        deref
                        (map (fn [option]
                               (update option :list-option/color-tag
                                       #(:color-tag/id @(rf/subscribe [:entity (:db/id %)]))))))
        on-select  #(rf/dispatch [:state/set-state :color-tag %])
        on-delete  #(when (js/confirm (str "Are you sure you want to delete the list " (:color-tag/id %) "?"))
                      (rf/dispatch [:api/delete-entity %])
                      (rf/dispatch [:state/set-state :color-tag nil]))]
    [simple-table
     [:color-tag/id]
     color-tags
     {:on-select on-select
      :on-delete on-delete}]))

(defn- color-tag-form [llist]
  (let [*color-tag (rf/subscribe [:state :color-tag])]
    [:<>
     [:h3 (if @*color-tag "Edit Color Tag" "Add Color Tag")]
     [entity-form {:entity       :color-tags
                   :parent-field :list/_color-tags
                   :parent-id    (:db/id list)
                   :id           (:db/id @*color-tag)
                   :on-create    #(-> %
                                      (assoc :color-tag/translation-key (str "behaveplus:list:" (str/lower-case (:list/name llist)) ":color-tag:" (name (:color-tag/id %)))))
                   :fields       [{:label     "Name"
                                   :required? true
                                   :type      :keyword
                                   :field-key :color-tag/id}]}]
     [:h4 "Translation"]
     [all-translations (:color-tag/translation-key @*color-tag)]]))

(defn- list-options-table [llist]
  (let [list-options (->> (rf/subscribe [:pull-children :list/options (:db/id llist)])
                          deref
                          (map (fn [option]
                                 (update option :list-option/color-tag
                                         #(:color-tag/id @(rf/subscribe [:entity (:db/id %)]))))))
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
        color-tag-options (mapv (fn [{eid :db/id}]
                                  (let [color-tag @(rf/subscribe [:entity eid])]
                                    {:value eid
                                     :label (:color-tag/id color-tag)}))
                                (:list/color-tags llist))]
    [:<>
     [:h3 (if @*list-option "Edit Option" "Add Option")]
     [entity-form {:entity       :list-options
                   :parent-field :list/_options
                   :parent-id    (:db/id list)
                   :id           (:db/id @*list-option)
                   :on-create    #(-> %
                                      (assoc :list-option/order (count @list-options))
                                      (assoc :list-option/translation-key (str "behaveplus:list-option:" (:list/name llist) ":" (:list-option/name %)))
                                      (assoc :list-option/result-translation-key (str "behaveplus:list-option:result:" (:list/name llist) ":" (:list-option/name %))))
                   :fields       [{:label     "Name"
                                   :required? true
                                   :field-key :list-option/name}
                                  {:label     "Index"
                                   :required? true
                                   :field-key :list-option/value}
                                  {:label     "Filter Tags"
                                   :type      :keywords
                                   :field-key :list-option/tags}
                                  {:label     "Color Tag"
                                   :type      :ref-select
                                   :options   color-tag-options
                                   :field-key :list-option/color-tag}
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
     [all-translations (:list-option/result-translation-key @*list-option)]]))

(defn- list-form [llist]
  [:<>
   [:div.row
    [:h3 (if llist (str "Edit " (:list/name llist)) "Add List")]]
   [:div.row
    [:div.col-3
     [entity-form {:entity :lists
                   :id     (when llist (:db/id llist))
                   :fields [{:label     "Name"
                             :required? true
                             :field-key :list/name}]}]]]
   [:div.row
    [:div.col-4
     [:h4 "Color Tags"]
     [:div
      {:style {:height "300px"}}
      [color-tags-table llist]]]
    [:div.col-8
     [color-tag-form llist]]]
   [:div.row
    [:div.col-8
     [:h4 "All Options"]
     [:div {:style {:height "800px"}}
      [list-options-table llist]]]
    [:div.col-4
     [list-option-form llist]]]])

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
