(ns behave-cms.store
  (:require [behave.schema.core :refer [all-schemas]]
            [datom-store.main :as s]))

(defn connect! [config]
  (s/default-conn all-schemas config))
