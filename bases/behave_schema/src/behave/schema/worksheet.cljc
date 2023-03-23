(ns behave.schema.worksheet
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(def single-ref? integer?)

(def many-ref? (s/and set? #(every? integer? %)))

;;; Spec

(s/def :worksheet/uuid                  string?)
(s/def :worksheet/name                  string?)
(s/def :worksheet/furthest-visited-step keyword?)
(s/def :worksheet/notes                 many-ref?)
(s/def :worksheet/inputs                many-ref?)
(s/def :worksheet/outputs               many-ref?)
(s/def :worksheet/result-table          single-ref?)
(s/def :worksheet/graph-settings        single-ref?)
(s/def :worksheet/table-settings        single-ref?)

;;; Schema

(def schema
  [{:db/ident       :worksheet/uuid
    :db/doc         "Worksheet's ID. UUID stored as a string."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :worksheet/run-description
    :db/doc         "Worksheet's run description."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :worksheet/name
    :db/doc         "Worksheet's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :worksheet/created
    :db/doc         "Worksheet's creation time in milliseconds since Jan 1., 1970."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :worksheet/furthest-visited-step
    :db/doc         "Worksheet's furthest completed step."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   ;; Relations
   {:db/ident       :worksheet/modules
    :db/doc         "Worksheet's modules."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/many}

   {:db/ident       :worksheet/notes
    :db/doc         "Worksheet's notes."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :worksheet/input-groups
    :db/doc         "Worksheet's input groups."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :worksheet/repeat-groups
    :db/doc         "Worksheet's repeat groups."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :worksheet/outputs
    :db/doc         "Worksheet's outputs."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :worksheet/result-table
    :db/doc         "Worksheet's result table."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :worksheet/graph-settings
    :db/doc         "Worksheet's graph settings."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :worksheet/table-settings
    :db/doc         "Worksheet's table settings."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   ;; Notes
   {:db/ident       :note/name
    :db/doc         "Note's name"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :note/content
    :db/doc         "Note's content."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :note/submodule
    :db/doc         "Note's reference to a submodule's UUID"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Input Groups
   {:db/ident       :input-group/group-uuid
    :db/doc         "Input Group's reference to a Group's UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :input-group/repeat-id
    :db/doc         "Input Group's repeat identifier."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :input-group/inputs
    :db/doc         "Input Group's input variables."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Repeat Groups
   {:db/ident       :repeat-group/group-uuid
    :db/doc         "Repeat Group's reference to a Group's UUID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :repeat-group/repeats
    :db/doc         "Number of repeats."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   ;; Input Variables
   {:db/ident       :input/group-variable-uuid
    :db/doc         "Input's reference to a Group Variable's UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :input/units
    :db/doc         "Input's units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :input/value
    :db/doc         "Input's value."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;;{:db/ident       :input/continuous-value
   ;; :db/doc         "Input's continuous value."
   ;; :db/valueType   :db.type/float
   ;; :db/cardinality :db.cardinality/one}
   ;;{:db/ident       :input/discrete-value
   ;; :db/doc         "Input's discrete value."
   ;; :db/valueType   :db.type/string
   ;; :db/cardinality :db.cardinality/one}
   ;;{:db/ident       :input/text-value
   ;; :db/doc         "Input's text value."
   ;; :db/valueType   :db.type/string
   ;; :db/cardinality :db.cardinality/one}

   ;; Outputs
   {:db/ident       :output/group-variable-uuid
    :db/doc         "Output's reference to Variable's UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :output/enabled?
    :db/doc         "Whether an output is enabled."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   ;; Result Table
   {:db/ident       :result-table/headers
    :db/doc         "Result table's heaers."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :result-table/rows
    :db/doc         "Result table's rows."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Result Header
   {:db/ident       :result-header/group-variable-uuid
    :db/doc         "Result header's group variable UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :result-header/repeat-id
    :db/doc         "Result header's repeat ID."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :result-header/order
    :db/doc         "Result header's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :result-header/units
    :db/doc         "Result header's units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Result Row
   {:db/ident       :result-row/id
    :db/doc         "Results row's id."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :result-row/cells
    :db/doc         "Results row's cells."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Result Cell
   {:db/ident       :result-cell/header
    :db/doc         "Results cell's header."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :result-cell/value
    :db/doc         "Results cell's value."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Results Table Settings
   {:db/ident       :table-settings/enabled?
    :db/doc         "Whether table results are enabled."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :table-settings/filters
    :db/doc         "Filters for the table."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Table filters
   {:db/ident       :table-filter/group-variable-uuid
    :db/doc         "Table filter's variable"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :table-filter/min
    :db/doc         "Table filter's min value"
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :table-filter/max
    :db/doc         "Table filter's max value"
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   ;; Results Graph Settings
   {:db/ident       :graph-settings/enabled?
    :db/doc         "Whether graph results are enabled."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :graph-settings/x-axis-group-variable-uuid
    :db/doc         "Graph's x-axis variable."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :graph-settings/z-axis-group-variable-uuid
    :db/doc         "Graph's z-axis variable."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :graph-settings/z2-axis-group-variable-uuid
    :db/doc         "Graph's z2-axis variable."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :graph-settings/y-axis-limits
    :db/doc         "Graph's y-axis limits."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; y-axis-limit
   {:db/ident       :y-axis-limit/group-variable-uuid
    :db/doc         "Y-axis's group variable uuid."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :y-axis-limit/min
    :db/doc         "Y axis's minimum value."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :y-axis-limit/max
    :db/doc         "Y axis's maximum value."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   ;; Table Shading

   ])
