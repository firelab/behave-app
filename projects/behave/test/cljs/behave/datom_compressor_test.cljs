(ns behave.datom-compressor-test
  "Regression test for the msgpack-cljc 2.0.359 cljs decode bug: negative fixints
  (-1..-32) were unpacked as their raw unsigned byte (255..224), so a table-filter
  min of -1 became 255 when a saved worksheet was reopened. `datom-compressor.core`
  loads `datom-compressor.msgpack-patch`, which corrects the decode."
  (:require [cljs.test                  :refer [deftest is] :include-macros true]
            [datom-compressor.interface :as compress]))

(deftest negative-fixint-roundtrip-test
  (doseq [v (concat (range -1 -33 -1)                 ; every negative fixint
                    [-100 -1000 -32768 0 1 42 127 128 255 256])]
    (let [roundtrip (compress/unpack (compress/pack [[1 :val v 30000 true]]))]
      (is (= v (nth (first roundtrip) 2))
          (str "value " v " should survive datom pack/unpack (was corrupted for -1..-32)")))))
