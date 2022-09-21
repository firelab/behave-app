(ns ds-schema-utils.core)

(defn keep-key
  [m k valid-vals]
  (if (contains? valid-vals (get m k))
    m
    (dissoc m k)))

(defn- simplify-schema [schema]
  (-> schema
      (select-keys [:db/ident :db/valueType :db/index :db/unique :db/cardinality :db/tupleAttrs])
      (keep-key :db/valueType #{:db.type/ref :db.type/tuple})
      (keep-key :db/cardinality #{:db.cardinality/many})))

(defn- required-schema? [schema]
  (or (:db/index schema)
      (:db/unique schema)
      (= (:db/cardinality schema) :db.cardinality/many)
      (#{:db.type/ref :db.type/tuple} (:db/valueType schema))))

(defn ->ds-schema [datomic-schema]
  (->> datomic-schema
       (filter required-schema?)
       (map simplify-schema)
       (reduce (fn [acc cur] (assoc acc
                                    (:db/ident cur)
                                    (dissoc cur :db/ident)))
               {})))
