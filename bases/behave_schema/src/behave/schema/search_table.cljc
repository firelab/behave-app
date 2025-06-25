(ns behave.schema.search-table
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [uuid-string?]]))

(def ^:private valid-search-table-op? (s/and keyword? #(#{:min :max} %)))
;;; Spec

(s/def :search-table/title  string?)
(s/def :search-table/group-variable-uuid  uuid-string?)
(s/def :search-table/op  valid-search-table-op?)
(s/def :search-table-column/group-variable-uuid  uuid-string?)
(s/def :search-table-column/order  int?)
(s/def :search-table/columns  (s/coll-of :search-table/column))

(s/def :search-table/column (s/keys :req [:search-table-column/group-variable-uuid
                                          :search-table-column/order]))

(s/def :behave/search-table (s/keys :req [:search-table/title
                                          :search-table/group-variable
                                          :search-table/op]
                                    :opt [:search-table/columns
                                          :search-table/filters]))
(def
  ^{:doc "Schema for search table."}
  schema
  [{:db/ident       :search-table/title
    :db/doc         "The Title of the Table"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   {:db/ident       :search-table/group-variable
    :db/doc         "The Group Variable to search for"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table/op
    :db/doc         "The operation used for search"
    :db/valueType   :db.type/keyword ;#{:min :max}
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table/filters
    :db/doc         ""
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :search-table/columns
    :db/doc         "The output Group Variables to display in the table"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; search-table-filters
   {:db/ident       :search-table-filter/group-variable
    :db/doc         ""
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table-filter/operator
    :db/doc         "search table filter's operator. Can be either: `:equal`, `:not-equal`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table-filter/value
    :db/doc         ""
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; search-table-column
   {:db/ident       :search-table-column/group-variable
    :db/doc         ""
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table-column/order
    :db/doc         ""
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}])


(comment
  (s/valid? :behave/search-table
            {:search-table/title   "My Search Table"
             :search-table/columns #{{:search-table-column/group-variable-uuid (str (random-uuid))
                                      :search-table-column/order               0}
                                     {:search-table-column/group-variable-uuid (str (random-uuid))
                                      :search-table-column/order               1}}}))
