(ns behave.schema.language
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [many-ref? uuid-string?]]))

;;; Validation Fns

(def valid-shortcode? (s/and string? #(re-find #"[a-z]{2}-[A-Z]{2}" %)))

;;; Spec

(s/def :language/uuid         uuid-string?)
(s/def :language/name         string?)
(s/def :language/shortcode    valid-shortcode?)
(s/def :language/translations many-ref?)
(s/def :language/help         many-ref?)


(s/def :behave/language (s/keys :req [:language/uuid
                                      :language/name
                                      :language/shortcode]
                                :opt [:language/translations
                                      :language/help]))

;;; Schema

(def schema
  [{:db/ident       :language/uuid
    :db/doc         "Language's UUID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :language/name
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

   {:db/ident       :language/help-pages
    :db/doc         "Language's help pages."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}])

;;; Testing

(comment
  (s/valid? :behave/language {:language/uuid         (str (random-uuid))
                              :language/name         "English"
                              :language/shortcode    "en-US"
                              :language/help         #{0 1 2}
                              :language/translations #{0 1 2}})
)

