(ns behave.schema.queries
  (:require #?(:cljs [datascript.core :as d]
               :clj  [datahike.api :as d])
            [behave.schema.rules :refer [all-rules]]))

(defn q-with-rules [query conn & args]
  (apply d/q query conn all-rules args))

(defn pull-children [conn child-attr eid & [pattern]]
  (d/pull-many conn
               (or pattern '[*])
               (d/q '[:find [?c ...]
                      :in $ ?child-attr ?e
                      :where [?e ?child-attr ?c]]
                    conn child-attr eid)))

(defn pull-with-attr [conn attr & [pattern]]
  (d/pull-many conn
               (or pattern '[*])
               (d/q '[:find [?e ...]
                      :in $ ?attr
                      :where [?e ?attr]]
                    conn attr)))
