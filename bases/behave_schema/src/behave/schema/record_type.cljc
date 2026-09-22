(ns behave.schema.record-type
  (:require [behave.schema.utils :refer [many-ref? single-ref? valid-key? zero-pos?]]
            [clojure.spec.alpha  :as s]))

;;; Validation Fns

(def ^:private valid-source? (s/and keyword? #(#{:input :generated} %)))

;;; Spec

(s/def :record-type/name                   string?)
(s/def :record-type/translation-key        valid-key?)
(s/def :record-type/cpp-namespace          string?)
(s/def :record-type/cpp-class              string?)
(s/def :record-type/cpp-function           string?)
(s/def :record-type/lib-ns                 string?)
(s/def :record-type/fields                 many-ref?)
(s/def :record-type/target-group-variables many-ref?)
(s/def :record-type/label-template         string?)
(s/def :record-type/order                  zero-pos?)

(s/def :behave/record-type (s/keys :req [:record-type/name
                                         :record-type/translation-key
                                         :record-type/cpp-class
                                         :record-type/cpp-function
                                         :record-type/lib-ns
                                         :record-type/fields]
                                   :opt [:record-type/cpp-namespace
                                         :record-type/target-group-variables
                                         :record-type/label-template
                                         :record-type/order]))

(s/def :record-field/order         zero-pos?)
(s/def :record-field/source        valid-source?)
(s/def :record-field/key?          boolean?)
(s/def :record-field/variable      single-ref?)
(s/def :record-field/cpp-parameter string?)
(s/def :record-field/value         string?)

(s/def :behave/record-field (s/keys :req [:record-field/order
                                          :record-field/source
                                          :record-field/cpp-parameter]
                                    :opt [:record-field/key?
                                          :record-field/variable
                                          :record-field/value]))

;;; Schema

#_{:clj-kondo/ignore [:missing-docstring]}
(def schema
  ;; Record Types
  [{:db/ident       :record-type/name
    :db/doc         "Record type's name, (e.g., \"Custom Fuel Model\")."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-type/translation-key
    :db/doc         "Record type's translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-type/cpp-namespace
    :db/doc         "UUID of the cpp.namespace entity the record type's function belongs to."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-type/cpp-class
    :db/doc         "UUID of the cpp.class entity a record configures, (e.g., \"SIGFuelModels\")."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-type/cpp-function
    :db/doc
    (str "UUID of the cpp.function entity applied once per record before a run. Every one of its value "
         "parameters must be covered by exactly one of the record type's fields; its *Units parameters are "
         "filled implicitly from the preceding field's units.")
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-type/lib-ns
    :db/doc
    (str "CLJS namespace holding the generated wrappers for the record type's class, "
         "(e.g., \"behave.lib.fuel-models\"). Named in data because the namespace is not derivable from the "
         "class name and cannot be resolved at runtime.")
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-type/fields
    :db/doc         "Record type's fields."
    :db/valueType   :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/many}

   {:db/ident       :record-type/target-group-variables
    :db/doc
    (str "Group variables whose list gains one option per record, valued by the record's key field. The "
         "tags a record may be given are the tags of those group variables' lists.")
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :record-type/label-template
    :db/doc
    (str "Template for a record's option label, interpolating field values by their variable's name, "
         "(e.g., \"{code} — {name}\").")
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-type/order
    :db/doc         "Record type's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   ;; Record Fields
   {:db/ident       :record-field/order
    :db/doc         "Record field's order in the editor, independent of its parameter's position."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-field/source
    :db/doc
    (str "Who is responsible for the field's value. :input means the user supplies it. :generated means "
         "the app does: pinned to :record-field/value when that is set, and otherwise computed as a value "
         "that clashes with nothing already in use, in which case the field is still rendered, prefilled "
         "and editable.")
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-field/key?
    :db/doc
    (str "True on the one field per record type whose value identifies a record: it becomes the option "
         "value a worksheet stores, and must clash with neither an existing option of a target group "
         "variable's list nor another record's key.")
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-field/variable
    :db/doc         "Variable supplying the field's kind, bounds, units, label and help."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-field/cpp-parameter
    :db/doc
    (str "UUID of the cpp.parameter entity this field's value fills. When the following parameter is a "
         "*Units type, the field's units fill that one.")
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-field/value
    :db/doc
    (str "Literal value of a :generated field, pinning it and hiding it from the editor. Absent means the "
         "app computes the value instead.")
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
