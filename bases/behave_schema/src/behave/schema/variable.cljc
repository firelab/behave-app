(ns behave.schema.variable
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn valid-key? [s]
  (re-find #"^[a-z:]+$" s))

(defn valid-kind? [k]
  (#{:continuous :discrete :text} k))

;;; Spec

(s/def :variable/id               uuid?)
(s/def :variable/name             string?)
(s/def :variable/kind             (s/and keyword? valid-kind?))
(s/def :variable/order            (s/and integer? #(<= 0 %)))
(s/def :variable/translation-key  (s/and string? valid-key?))
(s/def :variable/help-key         (s/and string? valid-key?))
(s/def :variable/groups           (s/and set? #(every? integer? %)))

;; Continuous Variables
(s/def :variable/default_value    (s/nilable float?))
(s/def :variable/english_decimals (s/nilable integer?))
(s/def :variable/english_units    (s/nilable string?))
(s/def :variable/maximum          float?)
(s/def :variable/minimum          float?)
(s/def :variable/metric_decimals  (s/nilable integer?))
(s/def :variable/metric_units     (s/nilable string?))
(s/def :variable/native_decimals  (s/nilable integer?))
(s/def :variable/native_units     (s/nilable string?))

;; Discrete Variables
(s/def :variable/list             (s/or :int integer? :str string?))

(defmulti kind :variable/kind)

(defmethod kind :continuous [_]
  (s/keys :req [:variable/id
                :variable/name
                :variable/order
                :variable/translation-key
                :variable/help-key
                :variable/maximum
                :variable/minimum]
          :opt [:variable/groups
                :variable/default_value
                :variable/english_decimals
                :variable/english_units
                :variable/metric_decimals
                :variable/metric_units
                :variable/native_decimals
                :variable/native_units]))

(defmethod kind :discrete [_]
  (s/keys :req [:variable/id
                :variable/name
                :variable/order
                :variable/translation-key
                :variable/help-key
                :variable/list]
          :opt [:variable/groups]))

(defmethod kind :text [_]
  (s/keys :req [:variable/id
                :variable/name
                :variable/order
                :variable/translation-key
                :variable/help-key]
          :opt [:variable/groups]))

(s/def :behave/variable (s/multi-spec kind :variable/kind))

;;; Schema

(def schema
  [{:db/ident       :variable/name
    :db/doc         "Variable's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/kind
    :db/doc         "Kind of variable. Can be :continuous, :discrete, or :text."
    :db/valueType   :db.type/keyword
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/translation-key
    :db/doc         "Variable's translation key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/help-key
    :db/doc         "Variable's help key."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   ;; Continuous Variables
   {:db/ident       :variable/maximum
    :db/doc         "Variable's maximum value."
    :db/valueType   :db.type/float
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/minimum
    :db/doc         "Variable's minimum value."
    :db/valueType   :db.type/float
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/default-value
    :db/doc         "Variable's default value."
    :db/valueType   :db.type/float
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/english-decimals
    :db/doc         "Variable's english decimal value."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/english-units
    :db/doc         "Variable's english units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/metric-decimals
    :db/doc         "Variable's metric decimal value."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/metric-units
    :db/doc         "Variable's metric units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/native-decimals
    :db/doc         "Variable's native decimal value."
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident       :variable/native-units
    :db/doc         "Variable's native units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Discrete Variables
   {:db/ident       :variable/list
    :db/doc         "Variable's list."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   ;; Lists
   {:db/ident       :list/name
    :db/doc         "List's names."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :list/options
    :db/doc         "List's options."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}
   {:db/ident       :list/translation-key
    :db/doc         "List's translation key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; List Options
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

;;; Testing

(comment
  (s/valid? :behave/variable {:variable/id #uuid "5eb87bf7-9501-4d50-9eee-7d0ffadbb1d0"
                              :variable/kind :text
                              :variable/name "Fire"
                              :variable/order 1
                              :variable/translation-key "behaveplus:fire"
                              :variable/help-key "behaveplus:fire:help"})

  (s/explain :behave/variable {:variable/id #uuid "5eb87bf7-9501-4d50-9eee-7d0ffadbb1d0"
                              :variable/kind :discrete
                              :variable/name "Fire"
                              :variable/order 1
                              :variable/list "derp"
                              :variable/translation-key "behaveplus:fire"
                              :variable/help-key "behaveplus:fire:help"})

  (s/explain :behave/variable {:variable/id #uuid "5eb87bf7-9501-4d50-9eee-7d0ffadbb1d0"
                              :variable/kind :continuous
                              :variable/name "Fire"
                              :variable/order 1
                              :variable/minimum 0.0
                              :variable/maximum 100.0
                              :variable/translation-key "behaveplus:fire"
                              :variable/help-key "behaveplus:fire:help"})

  )

