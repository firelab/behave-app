(ns datom-compressor.core
  (:require [clojure.set :refer [map-invert]]
            [msgpack.core :as msg]
            #?(:clj [msgpack.extensions])
            #?(:clj [msgpack.macros :refer [extend-msgpack]]))
  #?(:clj (:import [java.text SimpleDateFormat]
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

;;; Helpers

(defn- indexed-by [f coll]
  (->> coll
       (map f)
       (set)
       (map-indexed (fn [i attr] [attr i]))
       (into {})))

(defn- rle
  "Run-Length Encoding."
  ([xs] (rle (rest xs) (first xs) 1 []))
  ([xs cur cur-count result]
   (if (empty? xs)
     (conj result [cur cur-count])
     (if (= cur (first xs))
       (recur (rest xs) cur (inc cur-count) result)
       (recur (rest xs) (first xs) 1 (conj result [cur cur-count]))))))

(defn- un-rle
  [rle-xs]
  (vec (flatten (map (fn [[x n]] (repeat n x)) rle-xs))))

(defn- prepare [datoms]
  (let [e-idx  (indexed-by first datoms)
        a-idx  (indexed-by second datoms)
        v-idx  (indexed-by #(nth % 2) datoms)
        t-idx  (indexed-by #(nth % 3) datoms)
        datoms (->> datoms
                    (sort-by second)
                    (reduce (fn [{:keys [es as vs ts os]} [e a v t o]]
                              {:es (conj! es (get e-idx e))
                               :as (conj! as (get a-idx a))
                               :vs (conj! vs (get v-idx v))
                               :ts (conj! ts (get t-idx t))
                               :os (conj! os (if o 1 0))})
                            {:es (transient [])
                             :as (transient [])
                             :vs (transient [])
                             :ts (transient [])
                             :os (transient [])})
                    (map (fn [[k v]] [k (persistent! v)]))
                    (into {}))]

    [(rle (:es datoms))
     (rle (:as datoms))
     (rle (:vs datoms))
     (rle (:ts datoms))
     (rle (:os datoms))
     (map-invert e-idx)
     (map-invert a-idx)
     (map-invert v-idx)
     (map-invert t-idx)]))

(defn- unfold [[es as vs ts os e-idx a-idx v-idx t-idx]]
  (let [datoms (transient [])]
    (doall (map (fn [e a v t o] (conj! datoms
                                       [(get e-idx e)
                                        (get a-idx a)
                                        (get v-idx v)
                                        (get t-idx t)
                                        (if (= o 1) true false)]))
                (un-rle es)
                (un-rle as)
                (un-rle vs)
                (un-rle ts)
                (un-rle os)))
    (->> datoms
         (persistent!)
         (sort-by (juxt first second)))))

#?(:cljs
   (defn- ->array-buffer [packed]
     (cond
       (instance? js/ArrayBuffer packed)
       packed

       (instance? js/Uint8Array packed)
       (.slice (.-buffer packed)
               (.-byteOffset packed)
               (+ (.-byteLength packed) (.-byteOffset packed))))))

;;; Public

(defn pack
  "Packs datoms into a Run-Length Encoded MessagePack file."
  [datoms]
  (-> datoms (prepare) (msg/pack)))

(defn unpack
  "Unpacks datoms from a Run-Length Encoded MessagePack file."
  [packed]
  (-> packed
      #?(:cljs (->array-buffer))
      (msg/unpack)
      (unfold)))
