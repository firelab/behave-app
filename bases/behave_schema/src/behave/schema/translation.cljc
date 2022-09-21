(ns behave.schema.translation
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn valid-key? [s]
  (re-find #"^[a-z:]+$" s))

;;; Spec

(s/def :translation/key         (s/and string? valid-key?))
(s/def :translation/language    integer?)
(s/def :translation/translation string?)

(s/def :behave/translation (s/keys :req [:translation/language
                                         :translation/key
                                         :translation/translation]))

;;; Schema

(def schema
  [{:db/ident       :translation/key
    :db/doc         "Translation's key."
    :db/valueType   :db.type/string
    :db/index       true
    :db/cardinality :db.cardinality/one}
   {:db/ident       :translation/translation
    :db/doc         "Translation's content."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/translation {:translation/key "behaveplus:tools"
                                 :translation/translation "tools"})
)

