(ns datom-compressor.msgpack-patch
  "Runtime patch for a decoding bug in `com.github.rosejn/msgpack-cljc` 2.0.359.

  The library's ClojureScript `msgpack.pack/unpack-stream` decodes msgpack
  *negative fixints* (values -1..-32, bytes 0xE0..0xFF) by returning the raw
  UNSIGNED byte, so -1 decodes to 255, -2 to 254, ... -32 to 224. (The Clojure
  side is correct: it uses `(unchecked-byte byte)`.) This corrupts any value in
  -1..-32 whenever datoms cross the server->browser msgpack boundary, e.g. opening
  a saved worksheet (`behave.store` `c/unpack`).

  We redefine `unpack-stream` with the library's exact body and the single
  correction `byte` -> `(- byte 256)` in the negative-fixint branch. The library's
  `unpack`, `unpack-n`, and `unpack-map` all reach `unpack-stream` through its var,
  so this one `set!` fixes every cljs msgpack consumer (datom_compressor, transport)
  including nested values inside packed arrays/maps.

  Requiring this namespace applies the patch as a load-time side effect."
  (:require [msgpack.pack   :as pack]
            [msgpack.stream :as stream]))

(set! pack/unpack-stream
      (fn unpack-stream [stream]
        (let [b (stream/read-u8 stream)]
          (case b
            0xc0 nil
            0xc2 false
            0xc3 true
            0xc4 (stream/read-bytes stream (stream/read-u8 stream))
            0xc5 (stream/read-bytes stream (stream/read-u16 stream))
            0xc6 (stream/read-bytes stream (stream/read-u32 stream))
            0xc7 (pack/unpack-ext stream (stream/read-u8 stream))
            0xc8 (pack/unpack-ext stream (stream/read-u16 stream))
            0xc9 (pack/unpack-ext stream (stream/read-u32 stream))
            0xca (stream/read-f32 stream)
            0xcb (stream/read-f64 stream)
            0xcc (stream/read-u8 stream)
            0xcd (stream/read-u16 stream)
            0xce (stream/read-u32 stream)
            0xcf (stream/read-i64 stream)
            0xd0 (stream/read-i8 stream)
            0xd1 (stream/read-i16 stream)
            0xd2 (stream/read-i32 stream)
            0xd3 (stream/read-i64 stream)
            0xd4 (pack/unpack-ext stream 1)
            0xd5 (pack/unpack-ext stream 2)
            0xd6 (pack/unpack-ext stream 4)
            0xd7 (pack/unpack-ext stream 8)
            0xd8 (pack/unpack-ext stream 16)
            0xd9 (stream/read-str stream (stream/read-u8 stream))
            0xda (stream/read-str stream (stream/read-u16 stream))
            0xdb (stream/read-str stream (stream/read-u32 stream))
            0xdc (pack/unpack-n stream (stream/read-u16 stream))
            0xdd (pack/unpack-n stream (stream/read-u32 stream))
            0xde (pack/unpack-map stream (stream/read-u16 stream))
            0xdf (pack/unpack-map stream (stream/read-u32 stream))
            (cond
              ;; negative fixint: b is 0xE0..0xFF -> signed value (b - 256).
              ;; Library bug returned the raw byte here (-> 224..255).
              (= (bit-and 2r11100000 b) 2r11100000) (- b 256)
              (= (bit-and 2r10000000 b) 0)          b
              (= (bit-and 2r11100000 b) 2r10100000) (stream/read-str stream (bit-and 2r11111 b))
              (= (bit-and 2r11110000 b) 2r10010000) (pack/unpack-n stream (bit-and 2r1111 b))
              (= (bit-and 2r11110000 b) 2r10000000) (pack/unpack-map stream (bit-and 2r1111 b))
              :else (throw (js/Error. "invalid msgpack stream")))))))
