(ns behave-cms.record-types.views
  (:require [behave-cms.components.table-entity-form :refer [table-entity-form]]
            [behave-cms.record-types.subs]
            [behave-cms.subgroups.subs]
            [behave-cms.variables.subs]
            [map-utils.interface                     :refer [index-by]]
            [re-frame.core                           :as rf]
            [string-utils.interface                  :refer [->snake]]))

(declare fields-table)

;;; Record Types

(defn- records-table [app-id]
  (let [editor-state-path [:editors :record-type]
        entities          (rf/subscribe [:application/record-types app-id])]
    [table-entity-form
     {:entity             :record-type
      :form-state-path    editor-state-path
      :entities           (sort-by :record-type/order @entities)
      :parent-id          app-id
      :parent-field       :application/_record-types
      :table-header-attrs [:record-type/name :record-type/lib-ns]
      :order-attr         :record-type/order
      :translation-config {:key-fn  #(str "behaveplus:record_types:" (->snake (:record-type/name %)))
                           :text-fn :record-type/name}
      :entity-form-fields [{:label     "Name"
                            :required? true
                            :field-key :record-type/name}
                           {:label     "Library Namespace"
                            :required? true
                            :field-key :record-type/lib-ns}
                           {:label     "Label Template"
                            :field-key :record-type/label-template}
                           {:label     "C++ Namespace"
                            :type      :cpp-select
                            :cpp-level :namespace
                            :clears    [:record-type/cpp-class :record-type/cpp-function]
                            :field-key :record-type/cpp-namespace}
                           {:label           "C++ Class"
                            :type            :cpp-select
                            :cpp-level       :class
                            :scope-field-key :record-type/cpp-namespace
                            :clears          [:record-type/cpp-function]
                            :field-key       :record-type/cpp-class}
                           {:label           "C++ Function"
                            :type            :cpp-select
                            :cpp-level       :function
                            :scope-field-key :record-type/cpp-class
                            :field-key       :record-type/cpp-function}
                           {:label     "Target Group Variables"
                            :type      :group-variables
                            :app-id    app-id
                            :field-key :record-type/target-group-variables}
                           {:label     "Fields"
                            :type      :sub-table
                            :field-key :record-type/fields
                            :render    (fn [record-type-id] [fields-table record-type-id])}]}]))

;;; Record Fields

(def ^:private source-options
  [{:label "Input" :value :input}
   {:label "Generated" :value :generated}])

(defn- fields-table [record-type-id]
  (let [editor-state-path [:editors :record-field]
        entities          (rf/subscribe [:record-type/fields record-type-id])
        cpp-fn-uuid       (:record-type/cpp-function @(rf/subscribe [:entity record-type-id]))
        parameter-options (->> @(rf/subscribe [:cpp/parameters cpp-fn-uuid])
                               (sort-by :cpp.parameter/order)
                               (map (juxt :cpp.parameter/name :bp/uuid))
                               (map #(zipmap [:label :value] %)))
        variable-options  (->> @(rf/subscribe [:variables])
                               (map (juxt :variable/name :db/id))
                               (map #(zipmap [:label :value] %))
                               (sort-by :label))
        variable-lookup   (index-by :value variable-options)
        xform             (fn [fields]
                            (map (fn [{variable :record-field/variable :as field}]
                                   (-> field
                                       (assoc :record-field/variable (:db/id variable))
                                       (assoc :variable/name
                                              (:label (get variable-lookup (:db/id variable))))))
                                 fields))]
    [table-entity-form
     {:entity             :record-field
      :form-state-path    editor-state-path
      :entities           (sort-by :record-field/order (xform @entities))
      :parent-id          record-type-id
      :parent-field       :record-type/_fields
      :table-header-attrs [:variable/name :record-field/cpp-parameter :record-field/source :record-field/key?]
      :order-attr         :record-field/order
      :entity-form-fields [{:label     "Variable"
                            :type      :ref-select
                            :required? true
                            :field-key :record-field/variable
                            :options   variable-options}
                           {:label     "C++ Parameter"
                            :type      :select
                            :field-key :record-field/cpp-parameter
                            :options   parameter-options}
                           {:label     "Source"
                            :type      :keyword-select
                            :required? true
                            :field-key :record-field/source
                            :options   source-options}
                           {:label     "Key?"
                            :type      :boolean
                            :field-key :record-field/key?}]}]))

;;; Public

(defn record-types-section
  "Contents of the application's Custom Records accordion. Takes the
  application's entity ID. A record type's fields are edited inside its own
  form, alongside its other attributes."
  [app-id]
  [:div.col-12
   [:div.row
    [records-table app-id]]])
