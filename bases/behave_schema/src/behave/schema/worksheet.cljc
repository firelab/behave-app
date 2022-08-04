(ns behave.schema.worksheet
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(def single-ref? integer?)

(def many-ref? (s/and set? #(every? integer? %)))

;;; Spec

(s/def :worksheet/id            uuid?)
(s/def :worksheet/name          string?)
(s/def :workshet/notes          many-ref?)
(s/def :workshet/inputs         many-ref?)
(s/def :workshet/outputs        many-ref?)
(s/def :workshet/results-table  single-ref?)
(s/def :workshet/graph-settings single-ref?)
(s/def :workshet/table-settings single-ref?)

;;; Schema

(def schema
  [{:db/ident       :worksheet/id
    :db/doc         "Worksheet's ID."
    :db/valueType   :db.type/uuid
    :db/cardinality :db.cardinality/one}
   {:db/ident       :worksheet/name
    :db/doc         "Worksheet's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :worksheet/notes
    :db/doc         "Worksheet's notes."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :worksheet/inputs
    :db/doc         "Worksheet's inputs."
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
   {:db/ident       :note/submodule
    :db/doc         "Note's submodule."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :note/content
    :db/doc         "Note's content."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Inputs
   {:db/ident       :input/variable
    :db/doc         "Input's variable."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :input/kind
    :db/doc         "Input's kind."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident       :input/continuous-value
    :db/doc         "Input's continuous value."
    :db/valueType   :db.type/float
    :db/cardinality :db.cardinality/many}
   {:db/ident       :input/discrete-value
    :db/doc         "Input's discrete value."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :input/text-value
    :db/doc         "Input's text value."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Outputs
   {:db/ident       :output/variable
    :db/doc         "Input's variable."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   ;; Result Table
   {:db/ident       :result-table/header
    :db/doc         "Result table's heaers."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :result-table/row
    :db/doc         "Result table's rows."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Result Header
   {:db/ident       :result-header/variable
    :db/doc         "Result header's variable."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :result-header/units
    :db/doc         "Result header's units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Result Row
   {:db/ident       :result-row/cell
    :db/doc         "Results row's cell."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Result Cell
   {:db/ident       :result-cell/header
    :db/doc         "Results cell's header."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :result-cell/continuous-value
    :db/doc         "Results cell's continuous value."
    :db/valueType   :db.type/float
    :db/cardinality :db.cardinality/one}
   {:db/ident       :result-cell/discrete-value
    :db/doc         "Results cell's discrete value."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; TODO Graph Settings
   ;{:db/ident       :graph-settings/<setting>
   ; :db/doc         "Graph setting's <setting>."
   ; :db/valueType   :db.type/ref
   ; :db/cardinality :db.cardinality/one}

   ;; TODO Table Settings
   ;{:db/ident       :table-settings/<setting>
   ; :db/doc         "Table setting's <setting>."
   ; :db/valueType   :db.type/ref
   ; :db/cardinality :db.cardinality/one}

   ; Table Shading

])
