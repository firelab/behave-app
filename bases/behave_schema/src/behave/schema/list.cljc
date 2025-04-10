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
(s/def :list-option/value           string?)
(s/def :list-option/order           zero-pos?)
(s/def :list-option/translation-key string?)
(s/def :list-option/hide?           boolean?)

(s/def :behave/list-option (s/keys :req [:list-option/uuid
                                         :list-option/name
                                         :list-option/value
                                         :list-option/order
                                         :list-option/translation-key]
                                   :opt [:list-option/default
                                         :list-option/hide?]))

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

   ;;; Deprecated
   {:db/ident       :list/color-tags
    :db/doc         "Lists's color tags."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident       :list/tag-set
    :db/doc         "Lists's tag set for filtering options."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list/color-tag-set
    :db/doc         "Lists's color tag set."
    :db/valueType   :db.type/ref
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

   {:db/ident       :list-option/value
    :db/doc         "List option's value."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/order
    :db/doc         "List option's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   ;;; Deprecated
   {:db/ident       :list-option/tags
    :db/doc         "List option's filter tags."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/many}

   {:db/ident       :list-option/tag-refs
    :db/doc         "List option's filter tags."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;;; Deprecated
   {:db/ident       :list-option/color-tag
    :db/doc         "List option's color tag."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/color-tag-ref
    :db/doc         "List option's color tag."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/translation-key
    :db/doc         "List option's translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :list-option/result-translation-key
    :db/doc         "List option's translation key on results."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   {:db/ident       :list-option/hide?
    :db/doc         "Used to hid a list option."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   ;; List Color tags
   ;;; Deprecated
   {:db/ident       :color-tag/id
    :db/doc         "Color tag's keyword id."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   ;;; Deprecated
   {:db/ident       :color-tag/translation-key
    :db/doc         "Color tag's description translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   ;; Tag Sets
   {:db/ident       :tag-set/name
    :db/doc         "Tag set's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   {:db/ident       :tag-set/color?
    :db/doc         "Tag set color configuration."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :tag-set/tags
    :db/doc         "Tag set's tags."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many
    :db/isComponent true}

   {:db/ident       :tag-set/translation-key
    :db/doc         "Tag's translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   ;; Tags
   {:db/ident       :tag/name
    :db/doc         "Tag set's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   {:db/ident       :tag/translation-key
    :db/doc         "Tag's translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}

   {:db/ident       :tag/order
    :db/doc         "Tag's order."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :tag/color
    :db/doc         "Tag's color."
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
                                 :list-option/value           "2"
                                 :list-option/order           1
                                 :list-option/translation-key "behave:my-list:my-option"}))
