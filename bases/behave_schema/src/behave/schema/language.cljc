(ns behave.schema.language
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn valid-shortcode? [s]
  (re-find #"[a-z]{2}-[A-Z]{2}" s))

;;; Spec

(s/def :language/name string?)
(s/def :language/shortcode (s/and string? valid-shortcode?))

(s/def :behave/language (s/keys :req [:language/name :language/shortcode]))

;;; Schema

(def schema
  [{:db/ident       :language/name
    :db/doc         "Language's name (e.g. 'English')."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :language/shortcode
    :db/doc         "Language's short code (e.g. 'en-US')."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :language/translations
    :db/doc         "Language's translations."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :language/help
    :db/doc         "Language's help pages."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])

;;; Testing

(comment
  (s/valid? :behave/language {:language/id #uuid "5eb87bf7-9501-4d50-9eee-7d0ffadbb1d0"
                              :language/name "English"
                              :language/shortcode "en-US"})
)

