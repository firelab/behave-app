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

(s/def :behave/record-field (s/keys :req [:record-field/order
                                          :record-field/source
                                          :record-field/cpp-parameter]
                                    :opt [:record-field/key?
                                          :record-field/variable]))

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
    (str "Template for a record's option label, interpolating field values by the name of the C++ parameter "
         "each field fills, (e.g., \"{code} — {name}\").")
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
    (str "Whether the app protects this field's value. :input leaves it to the user, validated only by the "
         "bound variable. :generated means the app proposes a value that collides with nothing already in "
         "use and re-checks the user's edits against the same rule; the field is still rendered and "
         "editable. The key field is normally :generated, but any field needing a collision-free value "
         "may be.")
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
    :db/doc
    (str "Optional variable refining the field. The input's type is always derived from the bound "
         "cpp.parameter's type; a variable adds what that cannot supply — minimum and maximum for "
         "placeholders and validation, dimension and units for the unit selector, a list for discrete "
         "options, and a label (its :variable/name — variables carry no translation keys). Omit it for a "
         "parameter with no BehavePlus "
         "variable behind it, such as a bare bool flag, which then renders from its C++ type alone and "
         "labels itself from the parameter name.")
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :record-field/cpp-parameter
    :db/doc
    (str "UUID of the cpp.parameter entity this field's value fills. When the following parameter is a "
         "*Units type, the field's units fill that one. Its :cpp.parameter/type also decides how the field "
         "renders: bool as a checkbox, char*/char/std::string as text, double as a decimal number, "
         "int/long as an integer, and an enum type as a select over that cpp.enum's members.")
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
