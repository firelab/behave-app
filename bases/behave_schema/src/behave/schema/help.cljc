(ns behave.schema.help
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [uuid-string? valid-key?]]))

;;; Spec

(s/def :help/uuid    uuid-string?)
(s/def :help/key     valid-key?)
(s/def :help/content string?)

(s/def :behave/help (s/keys :req [:help/uuid
                                  :help/key
                                  :help/content]))

;;; Schema

(def schema
  [{:db/ident       :help/uuid
    :db/doc         "Help's uuid."
    :db/valueType   :db.type/string
    :db/index       true
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :help/key
    :db/doc         "Help page's key."
    :db/valueType   :db.type/string
    :db/index       true
    :db/cardinality :db.cardinality/one}

   {:db/ident       :help/content
    :db/doc         "Help page's contents."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

;;; Tests

(comment
  (s/valid? :behave/help {:help/uuid    (str (random-uuid))
                          :help/key     "behaveplus:help"
                          :help/content "This is a help page"})

  )
