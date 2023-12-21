(ns behave.schema.domain)

(def schema
  [
   ;; Domain Set
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
    :db/doc         "Doman's default native-unit uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/english-unit-uuid
    :db/doc         "Doman's default english-unit uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/metric-unit-uuid
    :db/doc         "Doman's default matric-unit uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :domain/dimension-uuid
    :db/doc         "Doman's dimension uuid"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])
