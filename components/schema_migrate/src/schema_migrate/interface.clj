(ns schema-migrate.interface
  (:require [schema-mgirate.core :as c]))

(def ^{:argslist '([conn attr])
       :doc      "Sets :db/isComponent true for a given schema attribute.
                  Takes a datahike conn."}
  migrate-attr-is-component c/migrate-attr-is-component)
