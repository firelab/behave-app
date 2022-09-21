(ns transport.interface
  (:require [transport.core :as c]))

;;; MIME to Type

(def ^{:argslist '([mime])
       :doc "Converts MIME type to keyword (e.g. (type->mime \"application/json\") ; => :json)

            Possible values for `mime` are one of:
            - \"application/edn\" -> :edn
            - \"application/json\" -> :json
            - \"application/transit+json\" -> :transit
            - \"application/msgpack\" -> :msgpack"}
  mime->type c/mime->type)

(def ^{:argslist '([t])
       :doc "Converts keyword to MIME type (e.g. (type->mime :json) => \"application/json\")

            Possible values for `transport` are one of:
            - :edn -> application/edn
            - :json -> application/json
            - :transit -> application/transit+json
            - :msgpack -> application/msgpack"}
  type->mime c/type->mime)

;;; Universal

(def ^{:argslist '([data transport])
       :doc "Converts CLJ/CLJS `data` to a `transport` format.

            Possible values for `transport` are one of:
            - :edn (Extensible Data Notation)
            - :json (JSON String)
            - :msgpack (MessagePack)
            - :transit (Transit w/ JSON encoding)"}
  clj-> c/clj->)

(def ^{:argslist '([s transport])
       :doc "Converts string to CLJ/CLJS to using the `transport` format.

            Possible values for `transport` are one of:
            - :edn (Extensible Data Notation)
            - :json (JSON String)
            - :msgpack (MessagePack)
            - :transit (Transit w/ JSON encoding)"}
  ->clj c/->clj)

;;; EDN

(def ^{:argslist '([data])
       :doc "Converts CLJ/CLJS `data` to a EDN encoded string."}
  clj->edn c/clj->edn)

(def ^{:argslist '([s])
       :doc "Converts EDN encoded string to CLJ/CLJS."}
  edn->clj c/edn->clj)

;;; JSON

(def ^{:argslist '([data])
       :doc "Converts CLJ/CLJS `data` to a JSON encoded string."}
  clj->json c/clj->json)

(def ^{:argslist '([s])
       :doc "Converts JSON encoded string to CLJ/CLJS."}
  json->clj c/json->clj)

;;; MsgPack

(def ^{:argslist '([data])
       :doc "Converts CLJ/CLJS `data` to a MsgPack format."}
  clj->msgpack c/clj->msgpack)

(def ^{:argslist '([s])
       :doc "Converts MsgPack string to CLJ/CLJS."}
  msgpack->clj c/msgpack->clj)

;;; Transit

(def ^{:argslist '([data])
       :doc "Converts CLJ/CLJS `data` to a Transit (JSON-encoded) string."}
  clj->transit c/clj->transit)

(def ^{:argslist '([s])
       :doc "Converts Transit (JSON encoded) string to CLJ/CLJS."}
  transit->clj c/transit->clj)

