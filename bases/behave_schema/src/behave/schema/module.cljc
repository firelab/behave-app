(ns behave.schema.module
  (:require [clojure.spec.alpha  :as s]
            [behave.schema.utils :refer [valid-key? uuid-string? zero-pos?]]))

;;; Spec

(s/def :module/uuid            uuid-string?)
(s/def :module/name            string?)
(s/def :module/order           zero-pos?)
(s/def :module/translation-key valid-key?)
(s/def :module/help-key        valid-key?)
(s/def :module/submodules      set?)

(s/def :behave/module (s/keys :req [:module/uuid
                                    :module/name
                                    :module/order
                                    :module/translation-key
                                    :module/help-key]
                              :opt [:module/submodules]))

;;; Schema

(def schema
  [{:db/ident       :module/uuid
    :db/doc         "Module's ID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :module/name
    :db/doc         "Module's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :module/order
    :db/doc         "Module's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :module/submodules
    :db/doc         "Module's submodules."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident       :module/translation-key
    :db/doc         "Module's translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :module/help-key
    :db/doc         "Module's help key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :module/diagrams
    :db/doc         "Module's diagrams"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :module/pivot-tables
    :db/doc         "Module's pivot tables"
    :db/valueType   :db.type/ref}])

;;; Testing

(comment
  (s/valid? :behave/module {:module/uuid            (str (random-uuid))
                            :module/name            "Contain"
                            :module/order           1
                            :module/translation-key "behaveplus:contain"
                            :module/help-key        "behaveplus:contain:help"})
  )
