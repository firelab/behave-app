(ns behave-cms.group-variables.views
  (:require [behave-cms.components.common                  :refer [accordion
                                                                   checkbox
                                                                   dropdown
                                                                   simple-table
                                                                   window]]
            [behave-cms.components.conditionals.views      :refer [conditionals-graph manage-conditionals]]
            [behave-cms.components.cpp-editor              :refer [cpp-editor-form]]
            [behave-cms.components.group-variable-selector :refer [group-variable-selector]]
            [behave-cms.components.sidebar                 :refer [sidebar sidebar-width]]
            [behave-cms.components.table-entity-form       :refer [table-entity-form table-entity-form-on-select]]
            [behave-cms.components.translations            :refer [all-translations]]
            [behave-cms.directions.subs]
            [behave-cms.help.views                         :refer [help-editor]]
            [behave-cms.routes                             :refer [app-routes]]
            [behave-cms.utils                              :as u]
            [bidi.bidi                                     :refer [path-for]]
            [re-frame.core                                 :as rf]))

;;; Constants

(def ^:private cpp-attrs {:cpp-class :group-variable/cpp-class
                          :cpp-fn    :group-variable/cpp-function
                          :cpp-ns    :group-variable/cpp-namespace
                          :cpp-param :group-variable/cpp-parameter})

;; Links Editor/Table

(defn links-table
  "Displays the Source & Destination links for the group variable."
  [gv-id]
  (let [is-output? @(rf/subscribe [:group-variable/is-output? gv-id])
        src-links  (rf/subscribe [:group-variable/source-links gv-id])
        dest-links (rf/subscribe [:group-variable/destination-links gv-id])]

    [:div.col-6
     [:div
      [:h4 "Source Links"]
      [:p "Data will be copied TO these variables."]
      [simple-table
       [:variable/name]
       (sort-by :variable/name @src-links)
       {:on-select #(rf/dispatch [:state/set-state :link (:db/id %)])
        :on-delete #(when (js/confirm (str "Are you sure you want to delete the link " (:variable/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))}]]

     (when-not is-output?
       [:div.mt-5.pt-3
        [:h4 "Destination Links"]
        [:p "Data will be copied FROM these variables."]
        [simple-table
         [:variable/name]
         (sort-by :variable/name @dest-links)
         {:on-select #(rf/dispatch [:state/set-state :link (:db/id %)])
          :on-delete #(when (js/confirm (str "Are you sure you want to delete the link " (:variable/name %) "?"))
                        (rf/dispatch [:api/delete-entity %]))}]])]))

;;; Settings

(defn- bool-setting [label attr entity]
  (let [{id :db/id} entity
        *value?     (atom (get entity attr))
        update!     #(rf/dispatch [:api/update-entity {:db/id id attr @*value?}])]
    [:div.mt-1
     [checkbox
      label
      @*value?
      #(do (swap! *value? not)
           (update!))]]))

(defn- direction-ref-setting [entity]
  (let [{id :db/id} entity
        directions  (rf/subscribe [:directions])
        selected    (get-in entity [:group-variable/direction-ref :direction/id])]
    [:div.mt-1
     [dropdown {:label     "Direction"
                :selected  selected
                :options   (map (fn [d] {:label (name (:direction/id d)) :value (:direction/id d)}) @directions)
                :on-select #(let [kw (u/input-keyword %)]
                              (if (= kw (keyword "Select Direction..."))
                                (rf/dispatch [:api/retract-entity-attr entity :group-variable/direction-ref])
                                (rf/dispatch [:api/update-entity {:db/id id :group-variable/direction-ref [:direction/id kw]}])))}]]))

(defn- settings [group-variable]
  [:div.row.mt-2
   [bool-setting "Research Variable?" :group-variable/research? group-variable]
   [bool-setting "Discrete Multiple?" :group-variable/discrete-multiple? group-variable]
   [bool-setting "Conditionally Set?" :group-variable/conditionally-set? group-variable]
   [bool-setting "Hide from Results?" :group-variable/hide-result? group-variable]
   [bool-setting "Hide from CSV Export?" :group-variable/hide-csv? group-variable]
   [bool-setting "Hide from Graphs?" :group-variable/hide-graph? group-variable]
   [direction-ref-setting group-variable]])

;;; Public Views

(defn- direction-variables-table [gv-id direction-variables]
  [:div.col-6
   [simple-table
    [:variable/name :group-variable/direction]
    (map (fn [dv]
           {:db/id                    (:db/id dv)
            :bp/nid                   (:bp/nid dv)
            :variable/name            (get-in dv [:variable/_group-variables 0 :variable/name])
            :group-variable/direction (:group-variable/direction dv)})
         direction-variables)
    {:on-select #(rf/dispatch [:navigate (path-for app-routes :get-group-variable :nid (:bp/nid %))])
     :on-delete #(when (js/confirm "Are you sure you want to remove this direction variable?")
                   (rf/dispatch [:api/retract-entity-attr-value
                                 gv-id :group-variable/direction-variables (:db/id %)]))}]])

(defn group-variable-page
  "Renders the group-variable page. Takes in a group-variable UUID."
  [{nid :nid}]
  (let [group-variable      (rf/subscribe [:entity [:bp/nid nid] '[* {:variable/_group-variables          [*]
                                                                      :group/_group-variables             [*]
                                                                      :group-variable/actions             [*]
                                                                      :group-variable/direction-variables [* {:variable/_group-variables [*]}]
                                                                      :group-variable/direction-ref       [:direction/id]}]])
        gv-id               (:db/id @group-variable)
        is-output?          (rf/subscribe [:group-variable/output? gv-id])
        actions             (:group-variable/actions @group-variable)
        action-id           (rf/subscribe [:editors :action])
        group               (:group/_group-variables @group-variable)
        variable            (get-in @group-variable [:variable/_group-variables 0])
        group-variables     (rf/subscribe [:sidebar/variables (:db/id group)])
        direction-variables (:group-variable/direction-variables @group-variable)
        link-id             (rf/subscribe [:state :link])
        destination-link-id (-> (rf/subscribe [:entity @link-id])
                                deref
                                (get-in [:link/destination :db/id]))]
    [:<>
     [sidebar
      "Variables"
      @group-variables
      (:group/name group)
      (str "/groups/" (:bp/nid group))]
     [window
      sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:variable/name variable)]]
       [accordion
        "Translations"
        [:h5 "Worksheet Translations"]
        [all-translations (:group-variable/translation-key @group-variable)]
        [:h5 "Result Translations"]
        [all-translations (:group-variable/result-translation-key @group-variable)]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:group-variable/help-key @group-variable)]]]

       [:hr]
       [accordion
        "CPP Functions"
        [:div.col-6
         [cpp-editor-form
          (merge cpp-attrs {:id [:bp/nid nid] :editor-key :group-variables})]]]

       [:hr]
       [accordion
        "Links"
        [:div.col-12
         [:div.row
          [links-table gv-id]
          [group-variable-selector
           {:app-id              @(rf/subscribe [:group-variable/_app-module-id gv-id])
            :gv-id               destination-link-id
            :title               "Destination Link"
            :on-submit           #(do
                                    (rf/dispatch [:api/upsert-entity
                                                  (cond-> {:link/source gv-id :link/destination %}
                                                    @link-id
                                                    (assoc :db/id @link-id))])
                                    (rf/dispatch [:state/set-state :link nil]))
            :submodule-filter-fn (let [is-output?  (rf/subscribe [:group-variable/is-output? gv-id])
                                       opposite-io (fn [{io :submodule/io}] (= io (if is-output? :input :output)))]
                                   opposite-io)}]]]]

       [:hr]
       [accordion
        "Direction Variables"
        [:div.col-12
         [:div.row
          [direction-variables-table gv-id direction-variables]
          [group-variable-selector
           {:app-id     @(rf/subscribe [:group-variable/_app-module-id gv-id])
            :state-path [:editors :direction-variable-lookup]
            :title      "Direction Variable"
            :on-submit  #(rf/dispatch [:api/update-entity
                                       {:db/id                              gv-id
                                        :group-variable/direction-variables %}])}]]]]

       [:hr]
       [accordion
        "Actions"
        [:div.row
         [:div.col-12
          [table-entity-form
           {:entity             :action
            :form-state-path    [:editors :action]
            :entities           (sort-by :action/name actions)
            :on-select          (table-entity-form-on-select [:editors :action])
            :parent-id          gv-id
            :parent-field       :group-variable/_actions
            :table-header-attrs [:action/name]
            :form-below?        true
            :entity-form-fields [{:label     "Name"
                                  :required? true
                                  :field-key :action/name}

                                 {:label     "Action Type"
                                  :required? true
                                  :type      :radio
                                  :field-key :action/type
                                  :options   [{:label "Select" :value :select}
                                              {:label "Disable" :value :disable}]}

                                 {:label        "Conditionals"
                                  :type         :conditionals
                                  :field-key    :action/conditionals
                                  :cond-op-attr :action/conditionals-operator}]}]]]]
       [:hr]
       [accordion
        "Hide from Results Conditionals"
        [:div.col-9
         [conditionals-graph
          gv-id
          gv-id
          :group-variable/hide-result-conditionals
          :group-variable/hide-result-conditional-operator]]
        [:div.col-3
         [manage-conditionals gv-id :group-variable/hide-result-conditionals]]]

       [:hr]
       [accordion
        "Hide Range Selector Conditionals"
        [:div.col-9
         [conditionals-graph
          gv-id
          gv-id
          :group-variable/disable-multi-valued-input-conditionals
          :group-variable/disable-multi-valued-input-conditional-operator]]
        [:div.col-3
         [manage-conditionals gv-id :group-variable/disable-multi-valued-input-conditionals]]]

       [:hr]
       [accordion
        "Settings"
        [:div.col-12
         [:div.row
          [settings @group-variable]]]]]]]))
