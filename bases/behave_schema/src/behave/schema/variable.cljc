(ns behave.schema.variable
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [valid-key? uuid-string?]]))

;;; Validation Fns

(def valid-kind? #(#{:continuous :discrete :text} %))

;;; Spec

(s/def :variable/uuid             uuid-string?)
(s/def :variable/name             string?)
(s/def :variable/kind             (s/and keyword? valid-kind?))
(s/def :variable/order            (s/and integer? #(<= 0 %)))
(s/def :variable/translation-key  (s/and string? valid-key?))
(s/def :variable/help-key         (s/and string? valid-key?))
(s/def :variable/groups           (s/and set? #(every? integer? %)))

;; Continuous Variables
(s/def :variable/default-value    (s/nilable float?))
(s/def :variable/english-decimals (s/nilable integer?))
(s/def :variable/english-units    (s/nilable string?))
(s/def :variable/maximum          float?)
(s/def :variable/minimum          float?)
(s/def :variable/metric-decimals  (s/nilable integer?))
(s/def :variable/metric-units     (s/nilable string?))
(s/def :variable/native-decimals  (s/nilable integer?))
(s/def :variable/native-units     (s/nilable string?))

;; Discrete Variables
(s/def :variable/list             (s/or :int integer? :str string?))

(defmulti kind :variable/kind)

(defmethod kind :continuous [_]
  (s/keys :req [:variable/uuid
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
  (s/keys :req [:variable/uuid
                :variable/name
                :variable/order
                :variable/translation-key
                :variable/help-key
                :variable/list]
          :opt [:variable/groups]))

(defmethod kind :text [_]
  (s/keys :req [:variable/uuid
                :variable/name
                :variable/order
                :variable/translation-key
                :variable/help-key]
          :opt [:variable/groups]))

(s/def :behave/variable (s/multi-spec kind :variable/kind))

;;; Schema

(def schema
  [{:db/ident       :variable/uuid
    :db/doc         "Variable's UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/name
    :db/doc         "Variable's name."
    :db/valueType   :db.type/string
    :db/index       true
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/bp6-label
    :db/doc         "Variable's BehavePlus 6 name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/bp6-code
    :db/doc         "Variable's BehavePlus 6 code name."
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

   ;; Associated with Group Variables
   {:db/ident       :variable/group-variables
    :db/doc         "Relationship to group."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Associated with Subtool Variables
   {:db/ident       :variable/subtool-variables
    :db/doc         "Relationship to subtool."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Continuous Variables
   {:db/ident       :variable/maximum
    :db/doc         "Variable's maximum value."
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/minimum
    :db/doc         "Variable's minimum value."
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/default-value
    :db/doc         "Variable's default value."
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/english-decimals
    :db/doc         "Variable's english decimal value."
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/english-units
    :db/doc         "Variable's english units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/metric-decimals
    :db/doc         "Variable's metric decimal value."
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/metric-units
    :db/doc         "Variable's metric units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/native-decimals
    :db/doc         "Variable's native decimal value."
    :db/valueType   :db.type/double
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/native-units
    :db/doc         "Variable's native units."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/dimension-uuid
    :db/doc         "Variable's dimension UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/metric-unit-uuid
    :db/doc         "Variable's metric unit UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/english-unit-uuid
    :db/doc         "Variable's english unit UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :variable/native-unit-uuid
    :db/doc         "Variable's native unit UUID."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   ;; Discrete Variables
   {:db/ident       :variable/list
    :db/doc         "Variable's list."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/variable {:variable/uuid            (str (random-uuid))
                              :variable/kind            :text
                              :variable/name            "Fire"
                              :variable/order           1
                              :variable/translation-key "behaveplus:fire"
                              :variable/help-key        "behaveplus:fire:help"})

  (s/valid? :behave/variable {:variable/uuid            (str (random-uuid))
                              :variable/kind            :discrete
                              :variable/name            "Fire"
                              :variable/order           1
                              :variable/list            "derp"
                              :variable/translation-key "behaveplus:fire"
                              :variable/help-key        "behaveplus:fire:help"})

  (s/valid? :behave/variable {:variable/uuid            (str (random-uuid))
                              :variable/kind            :continuous
                              :variable/name            "Fire"
                              :variable/order           1
                              :variable/minimum         0.0
                              :variable/maximum         100.0
                              :variable/translation-key "behaveplus:fire"
                              :variable/help-key        "behaveplus:fire:help"}))
