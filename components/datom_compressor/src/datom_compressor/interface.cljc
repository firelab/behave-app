(ns datom-compressor.interface
  (:require [datom-compressor.core :as c]))

(def ^{:argslist '[datoms]
       :doc "Packs `datoms` of the form `[e a v t op]` into an optimized
            MessagePack format which de-duplicates common values."}
  pack c/pack)

(def ^{:argslist '[packed]
       :doc "Unpacks special Messagepack buffer into collection of datoms of
            the form `[e a v t op]`."}
  unpack c/unpack)
