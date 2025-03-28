(ns transport.core
  (:require
    [clojure.set :refer [map-invert]]
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn]
    #?(:clj [clojure.data.json :as json])
    [cognitect.transit  :as transit]
    [msgpack.core :as msg]
    #?(:clj [msgpack.extensions])
    #?(:clj [msgpack.macros :refer [extend-msgpack]]))
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
                   [java.text SimpleDateFormat]
                   [java.util TimeZone])))

;;; Support for java.util.Date

#?(:clj
   (do
     (def ^:private ISO-8061-date-fmt "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
     (def ^:private sdf (SimpleDateFormat. ISO-8061-date-fmt))
     (.setTimeZone sdf (TimeZone/getTimeZone "UTC"))

     (extend-msgpack
      java.util.Date
      100
      #_{:clj-kondo/ignore [:unresolved-symbol]}
      (pack [date]
            (msg/pack (.format sdf date)))

      #_{:clj-kondo/ignore [:unresolved-symbol]}
      (unpack [bytes]
              (.parse sdf (msg/unpack bytes))))))

;;; MIME to Type

(def ^:private mime->type-mapping {"application/edn"          :edn
                                   "application/json"         :json
                                   "application/transit+json" :transit
                                   "application/msgpack"      :msgpack})

(def ^:private type->mime-mapping (map-invert mime->type-mapping))

(defn mime->type
  "Converts a MIME-Type to a simple keyword of: `:edn`, `:json`, `:transit`, `:msgpack`."
  [mime]
  (get mime->type-mapping mime))

(defn type->mime
  "Converts a simple keyword of: `:edn`, `:json`, `:transit`, `:msgpack` to a MIME-Type."
  [t]
  (get type->mime-mapping t))

;;; EDN

(defn clj->edn
  "Converts a Clojure data structure to an EDN representation."
  [x]
  (pr-str x))

(defn edn->clj
  "Converts a EDN string to Clojure data structure."
  [s]
  (edn/read-string s))

;;; JSON

(defn clj->json
  "Converts a Clojure data structure to JSON."
  [x]
  #?(:clj (json/write-str x)
     :cljs (.stringify js/JSON (clj->js x))))

(defn json->clj
  "Converts a JSON string to a Clojure data structure."
  [s]
  #?(:clj (json/read-str s :key-fn keyword)
     :cljs (js->clj (.parse js/JSON s) :keywordize-keys true)))

;;; MessagePack

(defn clj->msgpack
  "Converts a Clojure data structure to MessagePack file."
  [x]
  (msg/pack x))

(defn- ->array-buffer
  [packed]
  #?(:clj  packed
     :cljs (cond
             (instance? js/ArrayBuffer packed)
             packed

             (instance? js/Uint8Array packed)
             (.slice (.-buffer packed)
                     (.-byteOffset packed)
                     (+ (.-byteLength packed) (.-byteOffset packed))))))

(defn msgpack->clj
  "Converts a MessagePack file to a Clojure data structure."
  [s]
  (-> s
      (->array-buffer)
      (msg/unpack)))

;;; Transit

(defn clj->transit
  "Converts Clojure data structure to a Transit file."
  [x]
  #?(:clj
     (let [out (ByteArrayOutputStream. 4096)]
       (transit/write (transit/writer out :json) x)
       (.toString out))

     :cljs
     (transit/write (transit/writer :json) x)))

(defn transit->clj
  "Converts Transit file to a Clojure data structure."
  [s]
  #?(:clj
     (let [in (ByteArrayInputStream. (.getBytes s))]
       (transit/read (transit/reader in :json)))

     :cljs
     (transit/read (transit/reader :json) s)))

;;; Universal

(defn clj->
  "Universal conversion FROM Clojure data structure to `transport`, which is one of:
  - `:edn`
  - `:json`
  - `:msgpack`
  - `:transit`"
  [x transport]
  (condp = transport
    :edn     (clj->edn x)
    :json    (clj->json x)
    :msgpack (clj->msgpack x)
    :transit (clj->transit x)
    :else    (clj->edn x)))

(defn ->clj
  "Universal conversion TO Clojure data structure from `transport`, which is one of:
  - `:edn`
  - `:json`
  - `:msgpack`
  - `:transit`"
  [s transport]
  (condp = transport
    :edn     (edn->clj s)
    :json    (json->clj s)
    :msgpack (msgpack->clj s)
    :transit (transit->clj s)
    :else    (edn->clj s)))

