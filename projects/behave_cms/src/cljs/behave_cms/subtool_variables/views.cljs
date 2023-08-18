(ns behave-cms.subtool-variables.views
  (:require [reagent.core                       :as r]
            [re-frame.core                      :as rf]
            [behave-cms.components.common       :refer [accordion
                                                        window]]
            [behave-cms.components.sidebar      :refer [sidebar sidebar-width ->sidebar-links]]
            [behave-cms.components.translations :refer [all-translations]]
            [behave-cms.help.views              :refer [help-editor]]
            [behave-cms.utils                   :as u]))

;;; Constants

(def ^:private cpp-class :subtool-variable/cpp-class-uuid)
(def ^:private cpp-fn    :subtool-variable/cpp-function-uuid)
(def ^:private cpp-ns    :subtool-variable/cpp-namespace-uuid)
(def ^:private cpp-param :subtool-variable/cpp-parameter-uuid)
(def ^:private cpp-attrs [cpp-ns cpp-class cpp-fn cpp-param])

;;; Helpers

(defn- save-subtool-variable! [id]
  (let [state          @(rf/subscribe [:state [:editors :subtool-variables]])
        subtool-variable (merge {:db/id id} (select-keys state cpp-attrs))]
    (rf/dispatch [:api/update-entity subtool-variable])
    (rf/dispatch [:state/set-state :subtool-variable nil])
    (rf/dispatch [:state/set-state [:editors :subtool-variables] {}])))

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
                     (r/track #(or @(rf/subscribe [:state [:editors :subtool-variables field]]) (get original field ""))))
        set-field  (fn [field]
                     (fn [new-value] (rf/dispatch [:state/set-state [:editors :subtool-variables field] new-value])))
        on-submit  #(save-subtool-variable! id)
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

(defn subtool-variable-page
  "Renders the subtool-variable page. Takes in a map with:
   - :id [int]: Subtool variable's entity ID."
  [{eid :id}]
  (let [subtool-variable (rf/subscribe [:entity eid '[* {:variable/_subtool-variables [*]
                                                         :subtool/_input-variables    [*]
                                                         :subtool/_output-variables   [*]}]])

        subtool          (or (get-in @subtool-variable [:subtool/_input-variables 0])
                             (get-in @subtool-variable [:subtool/_output-variables 0]))
        variable         (get-in @subtool-variable [:variable/_subtool-variables 0])
        input-variables  (rf/subscribe [:subtool/input-variables (:db/id subtool)])
        output-variables (rf/subscribe [:subtool/output-variables (:db/id subtool)])]
    [:<>
     [sidebar
      "Input Variables"
      (->sidebar-links @input-variables :variable/name :get-subtool-variable)
      "Subtools"
      (str "/subtools/" (:db/id subtool))
      "Output Variables"
      (->sidebar-links @output-variables :variable/name :get-subtool-variable)]
     [window
      sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:variable/name variable)]]
       [accordion
        "Translations"
        [all-translations (:subtool-variable/translation-key @subtool-variable)]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:subtool-variable/help-key @subtool-variable)]]]
       [:hr]
       [accordion
        "CPP Functions"
        [:div.col-6
         [edit-variable eid]]]]]]))
