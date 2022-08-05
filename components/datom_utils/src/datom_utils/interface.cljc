(ns datom-utils.interface
  (:require [datom-utils.core :as c]))

(def ^{:argslist '([schema])
       :doc "Determines attributes that are not safe with transfer from Datahike
            to Datascript."}
  unsafe-attrs c/unsafe-attrs)

(def ^{:argslist '([datom])
       :doc "Splits a Datomic/DataHike/DataScript datom into a vector of the
            form [e a v t op]."}
  split-datom c/split-datom)

(def ^{:argslist '([datom])
       :doc "Splits Datomic/DataHike/DataScript datoms into vectors."}
  split-datoms c/split-datoms)

(def ^{:argslist '([unsafe-attrs datom])
       :doc "Meant to be used to filter for datoms that are not tuples, passwords,
            or transaction dates to maintain compatability with DataScript."}
  safe-attr? c/safe-attr?)

(def ^{:argslist '([a])
       :doc "Filters for datoms that are not tuples, passwords, or transaction
            dates to maintain compatability with DataScript."}
  safe-deref c/safe-deref)

(def ^{:argslist '([a])
       :doc "Filters for datoms that are not tuples, passwords, or transaction
            dates to maintain compatability with DataScript."}
  unwrap c/unwrap)
