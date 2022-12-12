(ns behave-cms.group-variables.views
  (:require [reagent.core                       :as r]
            [re-frame.core                      :as rf]
            [behave-cms.components.common       :refer [accordion
                                                        labeled-text-input
                                                        labeled-float-input
                                                        labeled-integer-input
                                                        window]]
            [behave-cms.components.help-editor  :refer [help-editor]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.utils                   :as u]))

;;; Helpers

(def ^:private cpp-columns [:cpp_namespace :cpp_class :cpp_function :cpp_parameter])

(defn- save-group-variable! []
  (let [state          @(rf/subscribe [:state [:editors :group-variables]])
        group-variable (select-keys state (conj cpp-columns :uuid))]
    (rf/dispatch [:api/update-entity :group-variables group-variable])
    (rf/dispatch [:state/set-state :group-variable nil])
    (rf/dispatch [:state/set-state [:editors :group-variables] {}])))

;;; Private Views

(defn- namespace-selector []
  (rf/dispatch [:api/entities :namespaces])
  (let [*namespace-uuid (rf/subscribe [:state [:editors :group-variables :cpp_namespace]])
        namespaces      (rf/subscribe [:cpp/namespaces])]
    [:div.mb-3
     [:div {:style {:visibility "hidden" :height "0px"}} @*namespace-uuid]
     [:label.form-label "Namespace:"]
     [:select.form-select {:on-change #(rf/dispatch [:state/set-state [:editors :group-variables :cpp_namespace] (u/input-value %)])}
      [:option {:value nil} "Select namespace..."]
      (for [{:keys [uuid namespace_name]} @namespaces]
        ^{:key uuid}
        [:option {:value uuid :selected (= @*namespace-uuid (str uuid))} namespace_name])]]))

(defn- class-selector []
  (let [*namespace-uuid (rf/subscribe [:state [:editors :group-variables :cpp_namespace]])
        _               (when @*namespace-uuid
                          (rf/dispatch [:api/entities :classes {:uuid @*namespace-uuid}]))
        *class-uuid     (rf/subscribe [:state [:editors :group-variables :cpp_class]])
        classes         (rf/subscribe [:cpp/classes @*namespace-uuid])]
    [:div.mb-3
     [:div {:style {:visibility "hidden" :height "0px"}} @*class-uuid]
     [:label.form-label "Class:"]
     [:select.form-select
      {:disabled (nil? @*namespace-uuid)
       :on-change #(rf/dispatch [:state/set-state [:editors :group-variables :cpp_class] (u/input-value %)])}
      [:option {:value nil} "Select class..."]
      (for [{:keys [uuid class_name]} @classes]
        ^{:key uuid}
        [:option {:value uuid :selected (= @*class-uuid (str uuid))} class_name])]]))

(defn- function-selector []
  (let [*class-uuid    (rf/subscribe [:state [:editors :group-variables :cpp_class]])
        _              (when @*class-uuid
                         (rf/dispatch [:api/entities :functions {:uuid @*class-uuid}]))
        *function-uuid (rf/subscribe [:state [:editors :group-variables :cpp_function]])
        functions      (rf/subscribe [:cpp/functions @*class-uuid])]
    [:div.mb-3
     [:div {:style {:visibility "hidden" :height "0px"}} @*function-uuid]
     [:label.form-label "Function:"]
     [:select.form-select
      {:disabled (nil? @*class-uuid)
       :on-change #(rf/dispatch [:state/set-state [:editors :group-variables :cpp_function] (u/input-value %)])}
      [:option {:value nil} "Select function..."]
      (for [{:keys [uuid function_name]} @functions]
        ^{:key uuid}
        [:option {:value uuid :selected (= @*function-uuid (str uuid))} function_name])]]))

(defn- parameter-selector []
  (let [*function-uuid  (rf/subscribe [:state [:editors :group-variables :cpp_function]])
        _               (when @*function-uuid
                          (rf/dispatch [:api/entities :parameters {:uuid @*function-uuid}]))
        *parameter-uuid (rf/subscribe [:state [:editors :group-variables :cpp_parameter]])
        parameters      (rf/subscribe [:cpp/parameters @*function-uuid])]
    [:div.mb-3
     [:div {:style {:visibility "hidden" :height "0px"}} @*parameter-uuid]
     [:label.form-label "Parameter:"]
     [:select.form-select
      {:disabled (nil? @*function-uuid)
       :on-change #(rf/dispatch [:state/set-state [:editors :group-variables :cpp_parameter] (u/input-value %)])}
      [:option {:value nil} "Select parameter..."]
      (for [{:keys [uuid parameter_name]} @parameters]
        ^{:key uuid}
        [:option {:value uuid :selected (= @*parameter-uuid (str uuid))} parameter_name])]]))

(defn- edit-variable [uuid]
  (rf/dispatch [:api/entity :group-variables {:uuid uuid}])

  (let [original  (rf/subscribe [:entity :group-variables uuid])
        _         (rf/dispatch [:state/set-state [:editors :group-variables] @original])
        on-submit #(save-group-variable!)]
    [:form
     {:on-submit (u/on-submit on-submit)}
     [namespace-selector]
     [class-selector]
     [function-selector]
     [parameter-selector]
     [:button.btn.btn-sm.btn-outline-primary {:type "submit"} "Save"]]))

;;; Public Views

(defn group-variable-page
  "Renders the group-variable page. Takes in a group-variable UUID."
  [{:keys [uuid]}]
  (rf/dispatch [:api/entity :group-variables {:uuid uuid}])

  (let [group-variable  (rf/subscribe [:entity :group-variables uuid])
        group-variables (rf/subscribe [:sidebar/variables (:group-uuid @group-variable)])]
    [:<>
     [sidebar
      "Variables"
      @group-variables
      "Groups"
      (str "/groups/" (:group-uuid @group-variable))]
     [window
      sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:variable_name @group-variable)]]
       [accordion
        "Translations"
        [all-translations (:translation_key @group-variable)]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:help_key @group-variable)]]]
       [:hr]
       [accordion
        "CPP Functions"
        [:div.col-6
         [edit-variable uuid]]]]]]))
