(ns behave.schema.list
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [many-ref? valid-key? uuid-string? zero-pos?]]))

;;; Spec

(s/def :list/uuid            uuid-string?)
(s/def :list/name            string?)
(s/def :list/translation-key valid-key?)
(s/def :list/options         many-ref?)

(s/def :behave/list (s/keys :req [:list/uuid
                                  :list/name
                                  :list/translation-key
                                  :list/options]))

(s/def :list-option/uuid            uuid-string?)
(s/def :list-option/name            string?)
(s/def :list-option/default         boolean?)
(s/def :list-option/index           zero-pos?)
(s/def :list-option/order           zero-pos?)
(s/def :list-option/translation-key string?)

(s/def :behave/list-option (s/keys :req [:list-option/uuid
                                         :list-option/name
                                         :list-option/index
                                         :list-option/order
                                         :list-option/translation-key]
                                   :opt [:list-option/default]))

(def schema
  ;; Lists
  [{:db/ident       :list/uuid
    :db/doc         "List's UUID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list/name
    :db/doc         "List's names."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list/translation-key
    :db/doc         "List's translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list/options
    :db/doc         "List's options."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; List Options
   {:db/ident       :list-option/uuid
    :db/doc         "List option's UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/name
    :db/doc         "List option's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/default
    :db/doc         "Whether list option's is the default value."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/index
    :db/doc         "List option's index."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/order
    :db/doc         "List option's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/translation-key
    :db/doc         "List option's translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

;;; Tests

(comment
  (s/valid? :behave/list {:list/uuid            (str (random-uuid))
                          :list/name            "My List"
                          :list/options         #{1}
                          :list/translation-key "behave:my-list"})

  (s/valid? :behave/list-option {:list-option/uuid            (str (random-uuid))
                                 :list-option/name            "My Option"
                                 :list-option/index           2
                                 :list-option/order           1
                                 :list-option/translation-key "behave:my-list:my-option"}))
