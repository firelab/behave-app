(ns behave.schema.pivot-table
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [many-ref? uuid-string?]]))

;;; Validation Fns

(def ^:private valid-pivot-value-fn? (s/and keyword? #(#{:sum :min :max :count} %)))

;;; Spec

(s/def :pivot-table/rows    many-ref?)
(s/def :pivot-table/values  many-ref?)

(s/def :pivot-value/function  keyword?)
(s/def :pivot-value/group-variable-uuid  uuid-string?)
(s/def :pivot-value/function  valid-pivot-value-fn?)

(s/def :behave/pivot-table-row (s/keys :req [:pivot-value/group-variable-uuid]))

(s/def :behave/pivot-table-value (s/keys :req [:pivot-value/group-variable-uuid
                                               :pivot-value/function]))

(s/def :behave/pivot-table (s/keys :req [:pivot-table/rows]
                                   :opt [:pivot-table/values]))

;;; Schema
(def
  ^{:doc "Schema for pivot table."}
  schema
  [{:db/ident       :pivot-table/tittle
    :db/doc         "Pivot Table's tittle"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   {:db/ident       :pivot-table/rows
    :db/doc         "Pivot Table's rows, see :pivot-row schema"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident       :pivot-table/values
    :db/doc         "Pivot Table's values, see :pivot-value schema"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   ;; pivot-row
   {:db/ident       :pivot-row/group-variable-uuid
    :db/doc         "Pivot Table's input group variable uuids"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;;pivot-value
   {:db/ident       :pivot-value/group-variable-uuid
    :db/doc         "Pivot Table's input group variable uuids"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :pivot-value/function
    :db/doc         "Function used to summarize the values"
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}])

(comment
  (s/valid? :behave/pivot-table
            {:pivot-table/rows   [(str (random-uuid))]
             :pivot-table/values #{1}})

  (s/valid? :behave/pivot-table-value
            {:pivot-value/group-variable-uuid (str (random-uuid))
             :pivot-value/function            :sum}))
