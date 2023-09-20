(ns behave-cms.components.cpp-editor
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [behave-cms.utils :as u]))

;;; Helpers

(defn- save! [editor-key cpp-attrs id]
  (let [state  @(rf/subscribe [:state [:editors editor-key]])
        entity (merge {:db/id id} (select-keys state cpp-attrs))]
    (rf/dispatch [:api/update-entity entity])
    (rf/dispatch [:state/set-state [:editors editor-key] {}])))

;;; Components

(defn- selector [label *uuid on-change name-attr options disabled?]
  [:div.mb-3
   [:div {:style {:visibility "hidden" :height "0px"}} @*uuid]
   [:label.form-label label]
   [:select.form-select
    {:disabled  disabled?
     :on-change #(on-change (u/input-value %))}
    [:option {:key 0 :value nil} "Select..."]
    (doall (for [{uuid :bp/uuid option-label name-attr} options]
             ^{:key uuid}
             [:option {:value uuid :selected (= @*uuid uuid)} option-label]))]])

;;; CPP Editor

(defn cpp-editor-form
  "Creates a CPP editor form for entity. Takes a map with:
  - id         [int]: ID of the entity.
  - editor-key [keyword]: Keyword of the editor (e.g. `:subtool-variables`)
  - cpp-ns     [keyword]: Attribute to store CPP Namespace UUID
  - cpp-class  [keyword]: Attribute to store CPP Class UUID
  - cpp-fn     [keyword]: Attribute to store CPP Class UUID
  - cpp-param  [keyword]: Attribute to store CPP Class UUID
  "
  [{:keys [id editor-key cpp-class cpp-fn cpp-ns cpp-param]}]
  (let [original    @(rf/subscribe [:entity id])
        cpp-attrs   [cpp-ns cpp-class cpp-fn cpp-param]
        on-submit   #(save! editor-key cpp-attrs id)
        editor-path [:editors editor-key]
        get-field   (fn [field]
                     (r/track #(or @(rf/subscribe [:state (conj editor-path field)]) (get original field ""))))
        set-field   (fn [field]
                     (fn [new-value] (rf/dispatch [:state/set-state (conj editor-path field) new-value])))
        namespaces  (rf/subscribe [:cpp/namespaces])
        classes     (rf/subscribe [:cpp/classes @(get-field cpp-ns)])
        functions   (rf/subscribe [:cpp/functions @(get-field cpp-class)])
        parameters  (rf/subscribe [:cpp/parameters @(get-field cpp-fn)])]
    [:form
     {:on-submit (u/on-submit on-submit)}
     [selector "Namespace:" (get-field cpp-ns)    (set-field cpp-ns)    :cpp.namespace/name  @namespaces  false]
     [selector "Class:"     (get-field cpp-class) (set-field cpp-class) :cpp.class/name      @classes    (nil? @(get-field cpp-ns))]
     [selector "Function:"  (get-field cpp-fn)    (set-field cpp-fn)    :cpp.function/name   @functions  (nil? @(get-field cpp-class))]
     [selector "Parameter:" (get-field cpp-param) (set-field cpp-param) :cpp.parameter/name  @parameters (nil? @(get-field cpp-fn))]
     [:button.btn.btn-sm.btn-outline-primary
      {:type "submit"}
      "Save"]]))
