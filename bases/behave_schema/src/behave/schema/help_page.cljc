(ns behave.schema.help-page
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [uuid-string? valid-key?]]))

;;; Spec

(s/def :help-page/uuid    uuid-string?)
(s/def :help-page/key     valid-key?)
(s/def :help-page/content string?)

(s/def :behave/help-page (s/keys :req [:help-page/uuid
                                       :help-page/key
                                       :help-page/content]))

;;; Schema

(def schema
  [{:db/ident       :help-page/uuid
    :db/doc         "Help's uuid."
    :db/valueType   :db.type/string
    :db/index       true
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :help-page/key
    :db/doc         "Help page's key."
    :db/valueType   :db.type/string
    :db/index       true
    :db/cardinality :db.cardinality/one}

   {:db/ident       :help-page/content
    :db/doc         "Help page's contents."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

;;; Tests

(comment
  (s/valid? :behave/help-page {:help-page/uuid    (str (random-uuid))
                               :help-page/key     "behaveplus:help"
                               :help-page/content "This is a help page"})

  )
