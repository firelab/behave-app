(ns behave.schema.note-category
  (:require [clojure.spec.alpha  :as s]))

;;; Spec

(s/def :note-category/name            string?)
(s/def :note-category/order           integer?)
(s/def :note-category/modules         set?)
(s/def :note-category/translation-key string?)

(s/def :behave/note-category (s/keys :req [:note-category/name
                                           :note-category/order
                                           :note-category/translation-key]
                                     :opt [:note-category/modules]))

;;; Schema

(def schema
  [{:db/ident       :note-category/name
    :db/doc         "Note Category's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :note-category/order
    :db/doc         "Note Category's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :note-category/modules
    :db/doc         "Note Category's modules."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :note-category/translation-key
    :db/doc         "Note Category's translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}])
