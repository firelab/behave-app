(ns behave.schema.domain
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [uuid-string? zero-pos?]]))

;; Domain Set
(s/def :domain-set/name    string?)
(s/def :domain-set/domains set?)

(s/def :behave/domain-set (s/keys :req [:domain-set/name
                                        :domain-set/domains]))

(s/def :domain/name                string?)
(s/def :domain/decimals            zero-pos?)
(s/def :domain/dimension-uuid      uuid-string?)
(s/def :domain/native-unit-uuid    uuid-string?)
(s/def :domain/english-unit-uuid   uuid-string?)
(s/def :domain/metric-unit-uuid    uuid-string?)
(s/def :domain/filtered-unit-uuids (s/coll-of uuid-string?))

(s/def :behave/domain (s/keys :req [:domain/name
                                    :domain/dimension-uuid
                                    :domain/native-unit-uuid
                                    :domain/english-unit-uuid
                                    :domain/metric-unit-uuid]
                              :opt [:domain/filtered-unit-uuids
                                    :domain/decimals]))

(def schema
  [;; Domain Set
   {:db/ident       :domain-set/name
    :db/doc         "Domain set's name"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain-set/domains
    :db/doc         "Domain set's collection of domains"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   ;; Domain
   {:db/ident       :domain/name
    :db/doc         "Domain's name"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/decimals
    :db/doc         "Domain's default decimal places"
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/native-unit-uuid
    :db/doc         "Domain's default native-unit uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/english-unit-uuid
    :db/doc         "Domain's default english-unit uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/metric-unit-uuid
    :db/doc         "Domain's default metric-unit uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/dimension-uuid
    :db/doc         "Domain's dimension uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/filtered-unit-uuids
    :db/doc         "Domain's filtered unit UUID's."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/many}])
