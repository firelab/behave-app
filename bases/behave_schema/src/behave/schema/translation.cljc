(ns behave.schema.translation
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string? zero-pos?]]))

;;; Spec

(s/def :translation/uuid string?)
(s/def :translation/key         valid-key?)
(s/def :translation/translation string?)

(s/def :behave/translation (s/keys :req [:translation/uuid
                                         :translation/key
                                         :translation/translation]))

;;; Schema

(def schema
  [{:db/ident       :translation/uuid
    :db/doc         "Translation's uuid."
    :db/valueType   :db.type/string
    :db/index       true
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :translation/key
    :db/doc         "Translation's key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :translation/translation
    :db/doc         "Translation's content."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/translation {:translation/uuid        (str (random-uuid))
                                 :translation/key         "behaveplus:tools"
                                 :translation/language    1
                                 :translation/translation "Tools"})
)

