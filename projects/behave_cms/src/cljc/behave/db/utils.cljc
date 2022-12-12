(ns behave.db.utils
  (:require [behave.schema.core :refer [all-schemas]]
            #?(:clj [datahike.core  :refer [conn?]]
               :cljs [datascript.core :refer [conn?]])))

(defn split-datom
  "Splits a DataHike/DataScript datom into a vector of the form [e a v t op]."
  [datom]
  [(.-e datom) (.-a datom) (.-v datom) (nth datom 3) (nth datom 4)])

(def split-datoms (partial map split-datom))

; Attrs that are either derived (tuples) or contain sensitive info should not be
; synced across browsers.
(def ^:private unsafe-attrs
  (->> all-schemas
       (flatten)
       (filter #(= :db.type/tuple (:db/valueType %)))
       (map :db/ident)
       (into #{:user/password :user/reset-key :db/txInstant})))

(defn safe-attr?
  "Filters for datoms that are not tuples, passwords, or transaction dates to
  maintain compatability with DataScript."
  ([datom]
   (not (contains? unsafe-attrs (second datom))))
  ([_ datom]
   (safe-attr? datom)))

(defn safe-split-datoms
  "Splits datom from DataHike/DataScript Datom to vectors, filters for 'safe' attributes.
  See: safe-attr?."
  [datoms]
  (->> datoms
       (split-datoms)
       (filter safe-attr?)))

(defn- atom? [a]
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
