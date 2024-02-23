(ns behave-cms.group-variables.views
  (:require [clojure.set                        :refer [rename-keys]]
            [reagent.core                       :as r]
            [re-frame.core                      :as rf]
            [data-utils.interface               :refer [parse-int]]
            [behave-cms.components.common       :refer [accordion
                                                        checkbox
                                                        dropdown
                                                        simple-table
                                                        window]]
            [behave-cms.components.actions      :refer [actions-table manage-action]]
            [behave-cms.components.cpp-editor   :refer [cpp-editor-form]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.utils                   :as u]))

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

(defn- ->option [name-key]
  (fn [m]
    (-> m
        (select-keys [:db/id name-key])
        (rename-keys {:db/id :value name-key :label}))))

(defn links-editor
  "Displays the links editor."
  [gv-id]
  (let [*link       (rf/subscribe [:state :link])
        is-output?  (rf/subscribe [:group-variable/is-output? gv-id])
        opposite-io (fn [{io :submodule/io}] (= io (if is-output? :input :output)))

        ;; Create a link with current group variable as the source.
        ->link      (fn [other-gv-id]
                      {:link/source gv-id :link/destination other-gv-id})
        p           #(conj [:editors :variable-lookup] %)
        get         #(rf/subscribe [:state %])
        set         (fn [path v] (rf/dispatch [:state/set-state path v]))
        modules     (rf/subscribe [:group-variable/app-modules gv-id])
        submodules  (rf/subscribe [:pull-children :module/submodules @(get (p :module))])
        groups      (rf/subscribe [:group-variable/submodule-groups-and-subgroups @(get (p :submodule))])
        variables   (rf/subscribe [:group/variables @(get (p :group))])
        disabled?   (r/track #(some nil? (map (fn [k] @(get (p k))) [:module :submodule :group :variable])))
        on-submit   #(rf/dispatch [:api/upsert-entity
                                   (merge
                                    (->link @(get (p :variable)))
                                    (when @*link {:db/id @*link}))])]
    [:div.col-6
     [:h4 (str (if @*link "Update" "Add") " Destination Link")]

     [:form
      {:on-submit (u/on-submit on-submit)}
      [dropdown {:label     "Module:"
                 :options   (map (->option :module/name) @modules)
                 :on-select #(set (p :module) (u/input-int-value %))}]
      [dropdown {:label     "Submodule:"
                 :options   (map (->option :submodule/name)
                                 (if is-output?
                                   (filter opposite-io @submodules)
                                   @submodules))
                 :on-select #(set (p :submodule) (u/input-int-value %))}]
      [dropdown {:label     "Groups:"
                 :options   (map (->option :group/name) @groups)
                 :on-select #(set (p :group) (u/input-int-value %))}]
      [dropdown {:label     "Variable:"
                 :options   (map (->option :variable/name) @variables)
                 :on-select #(set (p :variable) (u/input-int-value %))}]
      [:button.btn.btn-sm.btn-outline-primary {:type "submit" :disabled @disabled?} "Save"]]]))


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

(defn- settings [group-variable]
  [:div.row.mt-2
   [bool-setting "Research Variable?" :group-variable/research? group-variable]
   [bool-setting "Discrete Multiple?" :group-variable/discrete-multiple? group-variable]])

;;; Public Views

(defn group-variable-page
  "Renders the group-variable page. Takes in a group-variable UUID."
  [{id :id}]
  (let [gv-id           (parse-int id)
        group-variable  (rf/subscribe [:entity gv-id '[* {:variable/_group-variables [*]
                                                          :group/_group-variables    [*]
                                                          :group-variable/actions    [*]}]])
        is-output?      (rf/subscribe [:group-variable/output? gv-id])
        actions         (:group-variable/actions @group-variable)
        action-id       (rf/subscribe [:state :action])
        group           (get-in @group-variable [:group/_group-variables 0])
        variable        (get-in @group-variable [:variable/_group-variables 0])
        group-variables (rf/subscribe [:sidebar/variables (:db/id group)])]
    [:<>
     [sidebar
      "Variables"
      @group-variables
      (:group/name group)
      (str "/groups/" (:db/id group))]
     [window
      sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:variable/name variable)]]
       [accordion
        "Translations"
        [all-translations (:group-variable/translation-key @group-variable)]]
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
          (merge cpp-attrs {:id gv-id :editor-key :group-variables})]]]

       [:hr]
       [accordion
        "Links"
        [:div.col-12
         [:div.row
          [links-table gv-id]
          [links-editor gv-id]]]]

       [:hr]
       [accordion
        "Actions"
        [:div.row
         [:div.col-12
          [actions-table actions]]]
        [:div.row
         [:div.col-12
          [manage-action gv-id @action-id @is-output?]]]]

       [:hr]
       [accordion
        "Settings"
        [:div.col-12
         [:div.row
          [settings @group-variable]]]]]]]))
