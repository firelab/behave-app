(ns behave.schema.pivot-table
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [many-ref? uuid-string?]]))

;;; Validation Fns

(def ^:private valid-pivot-value-fn? (s/and keyword? #(#{:sum :min :max :count} %)))
(def ^:private valid-pivot-column-type? (s/and keyword? #(#{:field :value} %)))

;;; Spec

(s/def :pivot-table/columns  many-ref?)
(s/def :pivot-column/function  keyword?)
(s/def :pivot-column/group-variable-uuid  uuid-string?)
(s/def :pivot-column/function  valid-pivot-value-fn?)

(s/def :behave/pivot-table (s/keys :req [:pivot-table/title
                                         :pivot-table/columns]))

(s/def :behave/pivot-table-column-field (s/keys :req [:pivot-column/group-variable-uuid]))

(s/def :behave/pivot-table-column-value (s/keys :req [:pivot-column/group-variable-uuid
                                                      :pivot-column/function]))

;;; Schema
(def
  ^{:doc "Schema for pivot table."}
  schema
  [{:db/ident       :pivot-table/title
    :db/doc         "Pivot Table's title"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   {:db/ident       :pivot-table/columns
    :db/doc         "Pivot Table's columns"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   ;; pivot-column
   {:db/ident       :pivot-column/type
    :db/doc         "Pivot Column's type, #{:field :value}"
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :pivot-column/order
    :db/doc         "Pivot Column's order"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :pivot-column/group-variable-uuid
    :db/doc         "Pivot Column's group variable uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :pivot-column/function
    :db/doc         "Pivot Column's function used to summarize the values"
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}])

(comment
  (s/valid? :behave/pivot-table
            {:pivot-table/title "My Pivot Table"
             :pivot-table/columns #{1 2 3}})

  (s/valid? :behave/pivot-table-column-field
            {:pivot-column/group-variable-uuid (str (random-uuid))})

  (s/valid? :behave/pivot-table-column-value
            {:pivot-column/group-variable-uuid (str (random-uuid))
             :pivot-column/function            :sum}))
