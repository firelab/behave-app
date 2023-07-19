(ns behave-cms.group-variables.views
  (:require [clojure.set                        :refer [rename-keys]]
            [reagent.core                       :as r]
            [re-frame.core                      :as rf]
            [data-utils.interface               :refer [parse-int]]
            [behave-cms.components.common       :refer [accordion
                                                        dropdown
                                                        labeled-text-input
                                                        labeled-float-input
                                                        labeled-integer-input
                                                        simple-table
                                                        window]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.utils                   :as u]))

;;; Constants

(def ^:private cpp-class :group-variable/cpp-class)
(def ^:private cpp-fn    :group-variable/cpp-function)
(def ^:private cpp-ns    :group-variable/cpp-namespace)
(def ^:private cpp-param :group-variable/cpp-parameter)
(def ^:private cpp-attrs [cpp-ns cpp-class cpp-fn cpp-param])

;;; Helpers

(defn- save-group-variable! [id]
  (let [state          @(rf/subscribe [:state [:editors :group-variables]])
        group-variable (merge {:db/id id} (select-keys state cpp-attrs))]
    (rf/dispatch [:api/update-entity group-variable])
    (rf/dispatch [:state/set-state :group-variable nil])
    (rf/dispatch [:state/set-state [:editors :group-variables] {}])))

;;; Components

(defn- selector [label *uuid on-change name-attr options disabled?]
  [:div.mb-3
   [:div {:style {:visibility "hidden" :height "0px"}} @*uuid]
   [:label.form-label label]
   [:select.form-select
    {:disabled  disabled?
     :on-change #(on-change (u/input-value %))}
    [:option {:key 0 :value nil} "Select..."]
    (for [{uuid :bp/uuid option-label name-attr} options]
      ^{:key uuid}
      [:option {:value uuid :selected (= @*uuid uuid)} option-label])]])



;;; Variables Editor

(defn- edit-variable [id]
  (let [original   @(rf/subscribe [:entity id])
        get-field  (fn [field]
                     (r/track #(or @(rf/subscribe [:state [:editors :group-variables field]]) (get original field ""))))
        set-field  (fn [field]
                     (fn [new-value] (rf/dispatch [:state/set-state [:editors :group-variables field] new-value])))
        on-submit  #(save-group-variable! id)
        namespaces (rf/subscribe [:cpp/namespaces])
        classes    (rf/subscribe [:cpp/classes @(get-field cpp-ns)])
        functions  (rf/subscribe [:cpp/functions @(get-field cpp-class)])
        parameters (rf/subscribe [:cpp/parameters @(get-field cpp-fn)])]
    [:form
     {:on-submit (u/on-submit on-submit)}
     [selector "Namespace:" (get-field cpp-ns)    (set-field cpp-ns)    :cpp.namespace/name  @namespaces  false]
     [selector "Class:"     (get-field cpp-class) (set-field cpp-class) :cpp.class/name      @classes    (nil? @(get-field cpp-ns))]
     [selector "Function:"  (get-field cpp-fn)    (set-field cpp-fn)    :cpp.function/name   @functions  (nil? @(get-field cpp-class))]
     [selector "Parameter:" (get-field cpp-param) (set-field cpp-param) :cpp.parameter/name  @parameters (nil? @(get-field cpp-fn))]
     [:button.btn.btn-sm.btn-outline-primary {:type "submit"} "Save"]]))

;; Links Editor/Table

(defn links-table [gv-id]
  (let [is-output? @(rf/subscribe [:group-variable/is-output? gv-id])
        links      (rf/subscribe [(if is-output?
                                    :group-variable/source-links
                                    :group-variable/destination-links)
                                  gv-id])]
    [:div.col-6
     [:h3 (str (if is-output? "Destination" "Source") " Links")]
     [simple-table
      [:variable/name]
      (sort-by :variable/name @links)
      {:on-select #(rf/dispatch [:state/set-state :link (:db/id %)])
       :on-delete #(when (js/confirm (str "Are you sure you want to delete the link " (:variable/name %) "?"))

                     (rf/dispatch [:api/delete-entity %]))}]]))

(defn- ->option [name-key]
  (fn [m]
    (-> m
        (select-keys [:db/id name-key])
        (rename-keys {:db/id :value name-key :label}))))

(defn links-editor [gv-id]
  (let [links       (rf/subscribe [:group-variable/links gv-id])
        *link       (rf/subscribe [:state :link])
        is-output?  (rf/subscribe [:group-variable/is-output? gv-id])
        opposite-io (fn [{io :submodule/io}] (= io (if is-output? :input :output)))

        ;; Create a link. Links can only exist from an output variable (:link/source) to an input variable (:link/destination)
        ->link      (fn [other-gv-id]
                      {:link/source      (if is-output? gv-id other-gv-id)
                       :link/destination (if is-output? other-gv-id gv-id)})
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
     [:h3 (str (if @*link "Update" "Add") (if is-output? " 'Destination'" " 'Source'") " Link")]

     [:form
      {:on-submit (u/on-submit on-submit)}
      [dropdown {:label     "Module:"
                 :options   (map (->option :module/name) @modules)
                 :on-select #(set (p :module) (u/input-int-value %))}]
      [dropdown {:label     "Submodule:"
                 :options   (map (->option :submodule/name) (filter opposite-io @submodules))
                 :on-select #(set (p :submodule) (u/input-int-value %))}]
      [dropdown {:label     "Groups:"
                 :options   (map (->option :group/name) @groups)
                 :on-select #(set (p :group) (u/input-int-value %))}]
      [dropdown {:label     "Variable:"
                 :options   (map (->option :variable/name) @variables)
                 :on-select #(set (p :variable) (u/input-int-value %))}]
      [:button.btn.btn-sm.btn-outline-primary {:type "submit" :disabled @disabled?} "Save"]]]))

;;; Public Views

(defn group-variable-page
  "Renders the group-variable page. Takes in a group-variable UUID."
  [{id :id}]
  (let [gv-id           (parse-int id)
        group-variable  (rf/subscribe [:entity gv-id '[* {:variable/_group-variables [*]
                                                          :group/_group-variables    [*]}]])
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
         [edit-variable gv-id]]]

       [:hr]
       [accordion
        "Links"
        [:div.col-12
         [:div.row
          [links-table gv-id]
          [links-editor gv-id]]]]]]]))
