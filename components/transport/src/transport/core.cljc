(ns transport.core
  (:require
    [clojure.set :refer [map-invert]]
    [#?(:clj clojure.edn :cljs cljs.reader) :as edn]
    #?(:clj [clojure.data.json :as json])
    [cognitect.transit  :as transit]
    [msgpack.core :as msg]
    #?(:clj [msgpack.extensions]))
  #?(:clj (:import [java.io ByteArrayInputStream ByteArrayOutputStream])))

;;; MIME to Type

(def ^:private mime->type-mapping {"application/edn"          :edn
                                   "application/json"         :json
                                   "application/transit+json" :transit
                                   "application/msgpack"      :msgpack})

(def ^:private type->mime-mapping (map-invert mime->type-mapping))

(defn mime->type
  [mime]
  (get mime->type-mapping mime))

(defn type->mime
  [t]
  (get type->mime-mapping t))

;;; EDN

(defn clj->edn [x]
  (pr-str x))

(defn edn->clj [s]
  (edn/read-string s))

;;; JSON

(defn clj->json [x]
  #?(:clj (json/write-str x)
     :cljs (.stringify js/JSON (clj->js x))))

(defn json->clj [s]
  #?(:clj (json/read-str s :key-fn keyword)
     :cljs (js->clj (.parse js/JSON s) :keywordize-keys true)))

;;; MessagePack

(defn clj->msgpack [x]
  (msg/pack x))

(defn- ->array-buffer [packed]
  #?(:clj  packed
     :cljs (cond
             (instance? js/ArrayBuffer packed)
             packed

             (instance? js/Uint8Array packed)
             (.slice (.-buffer packed)
                     (.-byteOffset packed)
                     (+ (.-byteLength packed) (.-byteOffset packed))))))

(defn msgpack->clj [s]
  (-> s
      (->array-buffer)
      (msg/unpack)))

;;; Transit

(defn clj->transit [x]
  #?(:clj
     (let [out (ByteArrayOutputStream. 4096)]
       (transit/write (transit/writer out :json) x)
       (.toString out))

     :cljs
     (transit/write (transit/writer :json) x)))

(defn transit->clj [s]
  #?(:clj
     (let [in (ByteArrayInputStream. (.getBytes s))]
       (transit/read (transit/reader in :json)))

     :cljs
     (transit/read (transit/reader :json) s)))

;;; Universal

(defn clj->
  [x transport]
  (condp = transport
    :edn     (clj->edn x)
    :json    (clj->json x)
    :msgpack (clj->msgpack x)
    :transit (clj->transit x)
    :else    (clj->edn x)))

(defn ->clj
  [s transport]
  (condp = transport
    :edn     (edn->clj s)
    :json    (json->clj s)
    :msgpack (msgpack->clj s)
    :transit (transit->clj s)
    :else    (edn->clj s)))

