(ns datom-utils.core
  (:require [clojure.string :as str]
            [datascript.core :refer [conn?]]))

(defn split-datom
  "Splits a DataHike/DataScript datom into a vector of the form [e a v t op]."
  [datom]
  [(.-e datom) (.-a datom) (.-v datom) (nth datom 3) (nth datom 4)])

(def split-datoms (partial map split-datom))

(defn unsafe-attrs
  "Attrs that are either derived (tuples) or contain sensitive info should not be
  synced across browsers."
  [schemas]
  (->> schemas
       (flatten)
       (filter #(= :db.type/tuple (:db/valueType %)))
       (map :db/ident)
       (into #{:user/password :user/reset-key :db/txInstant})))

(defn safe-attr?
  "Filters for datoms that are not tuples, passwords, or transaction dates to
  maintain compatability with DataScript."
  [unsafe-attrs datom]
  (not (contains? unsafe-attrs (second datom))))

(defn- atom?
  [a]
  #?(:cljs (instance? Atom a)
     :clj  (instance? clojure.lang.IAtom a)))

(defn safe-deref
  "Recursively derefs the argument `a` to get the underlying value."
  [a]
  (if (atom? a) (safe-deref @a) a))

(defn unwrap
  "Recursively derefs the atom to get the DataHike/Script connection."
  [conn]
  (if (conn? conn)
    conn
    (unwrap @conn)))

(defn db-attr? [k]
  (let [s (str k)]
    (or (str/starts-with? s ":db")
        (str/starts-with? s ":fressian"))))

(defn db-attrs
  "Returns all attributes that begin with :db/* or :fressian*"
  [datoms]
  (as-> datoms $
    (map second $)
    (filter db-attr? $)
    (set $)))

(defn ref-attrs
  "Returns all reference attributes as a set of keywords from a Datomic schema."
  [schema]
  (->> schema
       (filter #(= :db.type/ref (:db/valueType %)))
       (map :db/ident)
       (set)))

(defn datoms->map
  "Turns a vector of datoms into a list of maps."
  [datoms]
  (sort-by :db/id
           (map (fn [[idx m]] (assoc m :db/id idx))
                (persistent!
                 (reduce (fn [acc [e a v]]
                           (if-let [entity (get acc e)]
                             (let [value (get entity a)
                                   value (cond
                                           (coll? value)
                                           (conj value v)

                                           (some? value)
                                           (vec (list value v))

                                           (nil? value)
                                           v)]
                               (assoc! acc e (merge entity {a value})))
                             (assoc! acc e {a v})))
                         (transient {})
                         datoms)))))
