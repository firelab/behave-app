(ns behave.schema.link
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string?]]))

(def schema
  [{:db/ident       :link/source
    :db/doc         "Link's source group variable. Group variable
                     NOTE: must be an *output* variable.

                     (Used in conjuction with :link/destination)."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :link/destination
    :db/doc         "Link's destination group variable.
                     NOTE: must be an *input* variable.

                     (Used in conjuction with :link/source)."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}])
