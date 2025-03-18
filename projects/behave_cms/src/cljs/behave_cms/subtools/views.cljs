(ns behave-cms.subtools.views
  (:require [clojure.set   :refer [difference]]
            [reagent.core  :as r]
            [re-frame.core :as rf]
            [string-utils.interface :refer [->kebab]]
            [behave-cms.components.common          :refer [accordion radio-buttons simple-table window checkbox]]
            [behave-cms.components.cpp-editor      :refer [cpp-editor-form]]
            [behave-cms.help.views                 :refer [help-editor]]
            [behave-cms.components.sidebar         :refer [->sidebar-links sidebar sidebar-width]]
            [behave-cms.components.translations    :refer [all-translations]]
            [behave-cms.components.variable-search :refer [variable-search]]
            [behave-cms.utils :as u]
            [behave-cms.subs]
            [behave-cms.events]))

;;; Constants

(def ^:private cpp-attrs {:cpp-class :subtool/cpp-class-uuid
                          :cpp-fn    :subtool/cpp-function-uuid
                          :cpp-ns    :subtool/cpp-namespace-uuid
                          :cpp-param :subtool/cpp-parameter-uuid})

;;; Components

(defn- remaining-variables [all-variables selected-variables]
  (let [all-variable-ids (set (map :db/id all-variables))
        current-var-ids  (set (map #(get-in % [:variable/_subtool-variables 0 :db/id]) selected-variables))
        remaining-ids    (difference all-variable-ids current-var-ids)]
    (filter #(-> % (:db/id) (remaining-ids)) all-variables)))

(defn- variable-search-wrapper [subtool-id translation-key disabled? io variables]
  (let [query         (rf/subscribe [:state [:search :variables]])
        all-variables (rf/subscribe [:group/search-variables @query])
        remaining     (remaining-variables @all-variables variables)]

    [variable-search
     {:results   remaining
      :disabled? disabled?
      :on-change (u/debounce #(rf/dispatch [:state/set-state [:search :variables] %]) 1000)
      :on-select #(let [variable @(rf/subscribe [:pull '[:variable/name] %])]
                    (rf/dispatch [:api/create-entity
                                  {:variable/_subtool-variables      %
                                   :subtool-variable/translation-key (str translation-key ":" (->kebab (:variable/name variable)))
                                   :subtool-variable/help-key        (str translation-key ":" (->kebab (:variable/name variable)) ":help")
                                   :subtool-variable/order           (count variables)
                                   :subtool-variable/io              io
                                   :subtool/_variables               subtool-id}]))
      :on-blur   #(rf/dispatch [:state/set-state [:search :variables] nil])}]))

(defn- manage-variable [subtool-id translation-key variables]
  (r/with-let [*variable-type (r/atom nil)]
    [:div.row
     [:h4 "Add Variable:"]

     [radio-buttons
      "Input/Output:"
      *variable-type
      [{:label "Input" :value "input"}
       {:label "Output" :value "output"}]]
     [variable-search-wrapper
      subtool-id
      translation-key
      (empty? @*variable-type)
      (if (= "input" @*variable-type) :input :output)
      variables]]))

(defn- variables-table [label variables]
  [:<>
   [:h5 label]
   [simple-table
    [:variable/name
     :subtool-variable/io]
    variables
    {:on-select   #(rf/dispatch [:subtool/edit-variable (first (:variable/_subtool-variables %))])
     :on-delete   #(when (js/confirm (str "Are you sure you want to delete the variable "
                                          (:variable/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))
     :on-increase #(rf/dispatch [:api/reorder % variables :subtool-variable/order :inc])
     :on-decrease #(rf/dispatch [:api/reorder % variables :subtool-variable/order :dec])}]])

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

(defn- settings [subtool]
  [:div.row.mt-2
   [bool-setting "Hide?" :subtool/hide? subtool]])

;;; Public

(defn subtools-page
  "Displays Subtools page. Takes a map with:
  - :id [int] - Subtool Entity ID"
  [{nid :nid}]
  (let [subtool          (rf/subscribe [:entity [:bp/nid nid] '[* {:tool/_subtools [*]}]])
        subtool-eid      (:db/id @subtool)
        tool-nid         (get-in @subtool [:tool/_subtools 0 :bp/nid])
        variables        (rf/subscribe [:subtool/variables nid])
        input-variables  (rf/subscribe [:subtool/input-variables nid])
        output-variables (rf/subscribe [:subtool/output-variables nid])]
    [:<>
     [sidebar
      "Input Variables"
      (->sidebar-links @input-variables :variable/name :get-subtool-variable)
      "Subtools"
      (str "/tools/" tool-nid)
      "Output Variables"
      (->sidebar-links @output-variables :variable/name :get-subtool-variable)]
     [window sidebar-width
      [:div.container
       [:div.row.mb-3.mt-4
        [:h2 (:subtool/name @subtool)]]
       [accordion
        "Variables"
        [:div.col-6
         [variables-table "Variables" @variables]]
        [:div.col-6
         [manage-variable subtool-eid (:subtool/translation-key @subtool) @input-variables @output-variables]]]
       [:hr]
       [accordion
        "Compute Function"
        [:div.col-6
         [cpp-editor-form
          (merge cpp-attrs {:id subtool-eid :editor-key :subtool-compute-fn})]]]
       [:hr]
       [accordion
        "Translations"
        [:div.col-12
         [all-translations (:subtool/translation-key @subtool)]]]
       [:hr]
       [accordion
        "Help Page"
        [:div.col-12
         [help-editor (:subtool/help-key @subtool)]]]
       [:hr]
       [accordion
        "Settings"
        [:div.col-12
         [:div.row
          [settings @subtool]]]]]]]))
