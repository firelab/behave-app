(ns behave-cms.subgroups.views
  (:require [clojure.set   :refer [difference]]
            [reagent.core  :as r]
            [re-frame.core :as rf]
            [data-utils.interface :refer [parse-int]]
            [string-utils.interface :refer [->kebab]]
            [behave-cms.components.common          :refer [accordion dropdown simple-table window]]
            [behave-cms.components.entity-form     :refer [entity-form]]
            [behave-cms.help.views                 :refer [help-editor]]
            [behave-cms.components.sidebar         :refer [sidebar sidebar-width]]
            [behave-cms.components.translations    :refer [all-translations]]
            [behave-cms.components.variable-search :refer [variable-search]]
            [behave-cms.utils :as u]
            [behave-cms.subs]
            [behave-cms.events]))

;;; Private Views

(defn- subgroup-form [group-id subgroup-id]
  [entity-form {:entity        :group
                :parent-field  :group/_children
                :parent-id     group-id
                :id            subgroup-id
                :fields        [{:label     "Name"
                                 :required? true
                                 :field-key :group-name}]}])

(defn- manage-subgroup [{id :db/id}]
  (let [subgroup (rf/subscribe [:state :subgroup])]
    [subgroup-form id @subgroup]))

(defn- subgroups-table [{group-id :db/id}]
  (r/with-let [subgroups (rf/subscribe [:group/subgroups group-id])]
    [simple-table
     [:group/name]
     (sort-by :group/name @subgroups)
     {:on-select #(rf/dispatch [:state/set-state :subgroup (:db/id %)])
      :on-delete #(when (js/confirm (str "Are you sure you want to delete the subgroup " (:group/name %) "?"))
                    (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @subgroups :group/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @subgroups :group/order :dec])}]))

(defn- variables-table [{group-id :db/id translation-key :group/translation-key}]
  (r/with-let [group-variables (rf/subscribe [:group/variables group-id])]
    [simple-table
     [:variable/name]
     (sort-by :group-variable/order @group-variables)
     {:on-delete   #(when (js/confirm (str "Are you sure you want to delete the variable " (:variable/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))
      :on-increase #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :dec])}]))

(defn- add-variable [{id :db/id group-variables :group/group-variables translation-key :group/translation-key}]
  (let [query            (rf/subscribe [:state [:search :variables]])
        all-variables    (rf/subscribe [:group/search-variables @query])
        all-variable-ids (set (map :db/id @all-variables))
        gv-ids           (set (map #(get-in % [:variable/_group-variable 0 :db/id]) group-variables))
        remaining-ids    (difference all-variable-ids gv-ids)
        remaining        (filter #(-> % (:db/id) (remaining-ids)) @all-variables)]
    [:div.row
     [:h4 "Add Variable:"]
     [variable-search
      remaining
      (u/debounce #(rf/dispatch [:state/set-state [:search :variables] %]) 1000)
      #(let [variable @(rf/subscribe [:pull '[:variable/name] %])]
         (rf/dispatch [:api/create-entity
                       {:group/_group-variable          id
                        :variable/_group-variable       %
                        :group-variable/translation-key (str translation-key ":" (->kebab (:variable/name variable)))
                        :group-variable/help-key        (str translation-key ":" (->kebab (:variable/name variable)) ":help")
                        :group-variable/order           (count group-variables)}]))
      #(rf/dispatch [:state/set-state [:search :variables] nil])]]))

;;; Conditionals

(defn- conditionals-table [{group-id :db/id}]
  (r/with-let [group-variables (rf/subscribe [:group/conditionals group-id])]
    [simple-table
     [:variable/name :conditional/op :conditional/values]
     (sort-by :variable/name @group-variables)
     {:on-delete   #(when (js/confirm (str "Are you sure you want to delete the conditional " (:variable/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))}]))

(defn- manage-conditionals [{id :db/id} selected-conditional-id]
  (let [modules    (rf/subscribe [:subgroup/app-modules id])
        *module    (r/atom nil)
        submodules (rf/subscribe [:pull-children :module/submodule @*module])
        *submodule (r/atom nil)
        groups     (rf/subscribe [:pull-children :submodule/group @*submodule])
        *group     (r/atom nil)
        variables  (rf/subscribe [:pull-children :group/group-variable @*group])
        *variable  (r/atom nil)
        operation  (r/atom nil)]
    (println @modules @*module @submodules @*submodule)
    [:div.row
     [:h4 "Manage Conditionals:"]
     [:form
      ;; TODO: Move this above the table
      [dropdown
       "Top Operation:"
       #(reset! operation (u/input-value %))
       [{:value :and :label "AND"}
        {:value :and :label "OR"}]]

      [dropdown
       "Module:"
       #(reset! *module (u/input-int-value %))
       (map (fn [{value :db/id label :module/name}]
              {:value value :label label}) @modules)]

      [dropdown
       "Submodule:"
       #(reset! *submodule (u/input-int-value %))
       (map (fn [{value :db/id label :submodule/name}]
              {:value value :label label}) @submodules)]

      [dropdown
       "Group:"
       #(reset! *group (u/input-int-value %))
       (map (fn [{value :db/id label :group/name}]
              {:value value :label label}) @groups)]

      [dropdown
       "Variable:"
       #(reset! *variable (u/input-int-value %))
       (map (fn [{value :db/id label :variable/name}]
              {:value value :label label}) @variables)]

      [dropdown
       "Operation:"
       #(reset! operation (u/input-value %))
       [{:value :equal :label "="}
        {:value :not-equal :label "!="}
        {:value :in :label "IN"}]]

      [:button.btn.btn-sm.btn-outline-primary.mt-4
       {:type "submit"}
       "Save"]]]))

;;; Public Views

(defn list-subgroups-page
  "Renders the subgroups page. Takes in a group UUID."
  [{:keys [id]}]
  (let [loaded? (rf/subscribe [:state :loaded?])]
    (if (not @loaded?)
      [:div "Loading..."]
      (let [group           (rf/subscribe [:entity id '[* {:submodule/_group     [*]
                                                           :group/group-variable [* {:variable/_group-variable [*]}]}]])
            group-variables (rf/subscribe [:sidebar/variables id])]
        [:<>
         [sidebar
          "Variables"
          @group-variables
          "Groups"
          (str "/submodules/" (get-in @group [:submodule/_group 0 :db/id]))]
         [window
          sidebar-width
          [:div.container
           ^{:key "name"}
           [:div.row.mb-3.mt-4
            [:h2 (:group/name @group)]]
           ^{:key "variables"}
           [accordion
            "Variables"
            [:div.col-6
             [variables-table @group]]
            [:div.col-6
             [add-variable @group]]]
           [:hr]
           ^{:key "conditionals"}
           [accordion
            "Conditionals"
            [:div.col-6
             [conditionals-table @group]]
            [:div.col-6
             [manage-conditionals @group]]]
           [:hr]
           ^{:key "translations"}
           [accordion
            "Translations"
            [all-translations (:group/translation-key @group)]]
           [:hr]
           ^{:key "help"}
           [accordion
            "Help Page"
            [:div.col-12
             [help-editor (:group/help-key @group)]]]]]]))))
