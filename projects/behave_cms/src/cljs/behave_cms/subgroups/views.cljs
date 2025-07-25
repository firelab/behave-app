(ns behave-cms.subgroups.views
  (:require [clojure.set   :refer [difference]]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [string-utils.interface :refer [->kebab]]
            [behave-cms.components.common          :refer [accordion checkbox simple-table window]]
            [behave-cms.components.conditionals    :refer [conditionals-graph manage-conditionals]]
            [behave-cms.help.views                 :refer [help-editor]]
            [behave-cms.components.sidebar         :refer [sidebar sidebar-width]]
            [behave-cms.components.translations    :refer [all-translations]]
            [behave-cms.components.variable-search :refer [variable-search]]
            [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.utils :as u]
            [behave-cms.subs]
            [behave-cms.events]))

;;; helpers

(defn- on-select [selected-entity-id selected-state-path & [other-state-paths-to-clear]]
  #(if (= (:db/id %) selected-entity-id)
     (do (rf/dispatch [:state/set-state selected-state-path nil])
         (doseq [path other-state-paths-to-clear]
           (rf/dispatch [:state/set-state path nil])))
     (rf/dispatch [:state/set-state selected-state-path
                   @(rf/subscribe [:re-entity (:db/id %)])])))

;;; Private Views
(defn- subgroups-table [group-id]
  (let [selected-state-path [:selected :group]
        editor-state-path   [:editors :group]
        selected-entity     (rf/subscribe [:state selected-state-path])
        groups              (rf/subscribe [:group/subgroups group-id])]
    [:div.col-12
     [table-entity-form
      {:entity             :group
       :form-state-path    editor-state-path
       :entities           (sort-by :group/order @groups)
       :on-select          (on-select (:db/id @selected-entity) selected-state-path)
       :parent-id          group-id
       :parent-field       :group/_children
       :table-header-attrs [:group/name]
       :order-attr         :group/order
       :entity-form-fields [{:label     "Name"
                             :required? true
                             :field-key :group/name}]}]]))

(defn- variables-table [group-id]
  (let [group-variables (rf/subscribe [:group/variables group-id])]
    [simple-table
     [:variable/name]
     (sort-by :group-variable/order @group-variables)
     {:on-increase #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :inc])
      :on-decrease #(rf/dispatch [:api/reorder % @group-variables :group-variable/order :dec])
      :on-select   #(rf/dispatch [:subgroups/edit-variables (first (:variable/_group-variables %))])}]))

(defn- add-variable [group-id]
  (let [translation-key  (rf/subscribe [:entity-attr group-id :group/translation-key])
        group-variables  (rf/subscribe [:group/variables group-id])
        query            (rf/subscribe [:state [:search :variables]])
        all-variables    (rf/subscribe [:group/search-variables @query])
        all-variable-ids (set (map :db/id @all-variables))
        gv-ids           (set (map #(get-in % [:variable/_group-variables 0 :db/id]) @group-variables))
        remaining-ids    (difference all-variable-ids gv-ids)
        remaining        (filter #(-> % (:db/id) (remaining-ids)) @all-variables)]
    [:div.row
     [:h4 "Add Variable:"]
     [variable-search
      {:results   remaining
       :on-change (u/debounce #(rf/dispatch [:state/set-state [:search :variables] %]) 1000)
       :on-select #(let [variable @(rf/subscribe [:pull '[:variable/name] %])]
                     (rf/dispatch [:api/create-entity
                                   {:group/_group-variables                group-id
                                    :variable/_group-variables             %
                                    :group-variable/translation-key        (str @translation-key ":" (->kebab (:variable/name variable)))
                                    :group-variable/result-translation-key (-> (str/replace @translation-key #":input:|:output:" ":result:")
                                                                               (str ":" (->kebab (:variable/name variable))))
                                    :group-variable/help-key               (str @translation-key ":" (->kebab (:variable/name variable)) ":help")
                                    :group-variable/order                  (count @group-variables)}]))
       :on-blur   #(rf/dispatch [:state/set-state [:search :variables] nil])}]]))

;;; Settings

(defn- bool-setting [label attr group]
  (let [{id :db/id} group
        *value?     (atom (get group attr))
        update!     #(rf/dispatch [:api/update-entity
                                   {:db/id id attr @*value?}])]
    [:div.mt-1
     [checkbox
      label
      @*value?
      #(do (swap! *value? not)
           (update!))]]))

(defn- group-settings [group]
  [:div.row.mt-2
   [bool-setting "Repeat Group?" :group/repeat? group]
   [bool-setting "Research Group?" :group/research? group]
   [bool-setting "Hide Group?" :group/hidden? group]
   [bool-setting "Single Select Group?" :group/single-select? group]])

;;; Public Views

(defn list-subgroups-page
  "Renders the subgroups page. Takes in a group UUID."
  [{:keys [nid]}]
  (let [group               (rf/subscribe [:entity [:bp/nid nid] '[* {:submodule/_groups [:db/id :submodule/name :bp/nid]}]])
        id                  (:db/id @group)
        parent-group        (rf/subscribe [:subgroup/parent id])
        parent-submodule    (:submodule/_groups @group)
        group-variables     (rf/subscribe [:sidebar/variables id])
        subgroups           (rf/subscribe [:sidebar/subgroups id])
        var-conditionals    (rf/subscribe [:group/variable-conditionals id])
        module-conditionals (rf/subscribe [:group/module-conditionals id])]
    [:div
     {:id (str id)}
     [sidebar
      "Variables"
      @group-variables
      (if @parent-group
        (:group/name @parent-group)
        (str (:submodule/name parent-submodule) " Groups"))
      (if @parent-group
        (str "/groups/" (:bp/nid @parent-group))
        (str "/submodules/" (:bp/nid parent-submodule)))
      "Subgroups"
      (when (seq @subgroups) @subgroups)]
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
         [variables-table id]]
        [:div.col-6
         [add-variable id]]]
       [:hr]
       ^{:key "subgroups"}
       [accordion
        "Subgroups"
        [subgroups-table id]]
       [:hr]
       ^{:key "conditionals"}
       [accordion
        "Conditionals"
        [:div.col-9
         [conditionals-graph id id (concat @var-conditionals @module-conditionals) :group/conditionals :group/conditionals-operator]]
        [:div.col-3
         [manage-conditionals id :group/conditionals]]]
       [:hr]
       ^{:key "translations"}
       [accordion
        "Translations"
        [:h5 "Worksheet Translations"]
        [all-translations (:group/translation-key @group)]
        [:h5 "Result Translations"]
        [all-translations (:group/result-translation-key @group)]]
       [:hr]
       ^{:key "help"}
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:group/help-key @group)]]]
       [:hr]
       ^{:key "settings"}
       [accordion
        "Settings"
        [:div.col-12
         [group-settings @group]]]]]]))
