(ns behave-cms.group-variables.views
  (:require [reagent.core                       :as r]
            [re-frame.core                      :as rf]
            [data-utils.interface               :refer [parse-int]]
            [behave-cms.components.common       :refer [accordion
                                                        labeled-text-input
                                                        labeled-float-input
                                                        labeled-integer-input
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

;;; Private Views

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

;;; Public Views

(defn group-variable-page
  "Renders the group-variable page. Takes in a group-variable UUID."
  [{:keys [id]}]
  (let [loaded? (rf/subscribe [:state :loaded?])]
    (if (not @loaded?)
      [:div "Loading ..."]
      (let [gv-id           (parse-int id)
            group-variable  (rf/subscribe [:entity gv-id '[* {:variable/_group-variable [*]
                                                              :group/_group-variable    [*]}]])
            group           (get-in @group-variable [:group/_group-variable 0])
            variable        (get-in @group-variable [:variable/_group-variable 0])
            group-variables (rf/subscribe [:sidebar/variables (:db/id group)])]
        [:<>
         [sidebar
          "Variables"
          @group-variables
          "Groups"
          (str "/groups/" (:db/id group))]
         [window
          sidebar-width
          [:div.container
           [:div.row.mb-3.mt-4
            [:h2 (:variable/name variable)]]
           [accordion
            "Translations"
            [all-translations (:translation/key @group-variable)]]
           [:hr]
           [accordion
            "Help Page"
            [:div.col-12
             [help-editor (:help/key @group-variable)]]]
           [:hr]
           [accordion
            "CPP Functions"
            [:div.col-6
             [edit-variable gv-id]]]]]]))))
