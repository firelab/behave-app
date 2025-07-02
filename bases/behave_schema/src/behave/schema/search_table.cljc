(ns behave.schema.search-table
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [uuid-string?]]))

;;; Spec
;; search-table-column
(s/def :search-table-column/group-variable  (s/coll-of (s/or :behave/group-variable :behave/group-variable
                                                             :ref                  int?)))
(s/def :search-table-column/order  int?)
(s/def :behave/search-table-column (s/keys :req [:bp/uuid
                                                 :bp/nid
                                                 :search-table-column/group-variable
                                                 :search-table-column/order]))

;; search-table-filter
(s/def :search-table-filter/group-variable (s/coll-of (s/or :behave/group-variable :behave/group-variable
                                                            :ref                  int?)))
(s/def :search-table-filter/value  string?)
(s/def :behave/search-table-filter (s/keys :req [:search-table-filter/group-variable
                                                 :search-table-filter/value]))

;; search-table
(s/def :search-table/name  string?)
(s/def :search-table/group-variable-uuid  uuid-string?)
(s/def :search-table/operator  (s/and keyword? #(#{:min :max} %)))
(s/def :search-table/columns  (s/coll-of (s/or :behave/search-table-column :behave/search-table-column
                                               :ref                  int?)))
(s/def :search-table/filters (s/coll-of (s/or :behave/search-table-filter :behave/search-table-filter
                                              :ref                  int?)))
(s/def :behave/search-table (s/keys :req [:bp/uuid
                                          :bp/nid
                                          :search-table/name
                                          :search-table/group-variable
                                          :search-table/operator]
                                    :opt [:search-table/columns
                                          :search-table/filters]))

(def
  ^{:doc "Schema for search table."}
  schema
  [{:db/ident       :search-table/name
    :db/doc         "Name of search-table"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table/group-variable
    :db/doc         "The Group Variable to search for"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table/operator
    :db/doc         "The operation used on the values for `:search-table/group-variable`. Can be either `:min` or `:max`"
    :db/valueType   :db.type/keyword ;#{:min :max}
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table/filters
    :db/doc         "Search table filters"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident       :search-table/columns
    :db/doc         "Search table columns"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident       :search-table/translation-key
    :db/doc         "The search-table's translation-key"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   ;; search-table-filters
   {:db/ident       :search-table-filter/group-variable
    :db/doc         "The search table filter's Group Variable to operate on"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table-filter/operator
    :db/doc         "The operator to use on `:search-table-filter/group-variable`. Can be either: `:equal`, `:not-equal`."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table-filter/value
    :db/doc         "The value to use for the `:search-table-filter/operator`"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; search-table-column
   {:db/ident       :search-table-column/name
    :db/doc         "The search table column's name"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table-column/group-variable
    :db/doc         "The search table column's Group Variable"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table-column/order
    :db/doc         "The search table column's order"
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :search-table-column/translation-key
    :db/doc         "The search table column's translation-key"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])


(comment
  (s/valid? :behave/search-table
            {:search-table/name   "My Search Table"
             :search-table/columns #{{:search-table-column/group-variable-uuid (str (random-uuid))
                                      :search-table-column/order               0}
                                     {:search-table-column/group-variable-uuid (str (random-uuid))
                                      :search-table-column/order               1}}}))
