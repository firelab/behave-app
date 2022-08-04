(ns behave.schema.help
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn valid-key? [s]
  (re-find #"^[a-z:]+$" s))

;;; Spec

(s/def :help/id      uuid?)
(s/def :help/key     (s/and string? valid-key?))
(s/def :help/content string?)

(s/def :behave/help (s/keys :req [:help/id
                                  :help/key
                                  :help/content]))

(def schema
  [{:db/ident       :help/language
    :db/doc         "Reference to language."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :help/key
    :db/doc         "Help page's key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :help/content
    :db/doc         "Help page's contents."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
