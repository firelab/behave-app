(ns behave.store
  (:require [behave.schema.core :refer [all-schemas]]
            [datomic-store.main :as s]))

(defn connect! [config]
  (s/default-conn config all-schemas))
