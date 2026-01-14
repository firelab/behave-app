(ns ^{:author "Nikita Prokopov"
      :doc "A B-tree based persistent sorted set. Supports transients, custom comparators, fast iteration, efficient slices (iterator over a part of the set) and reverse slices. Almost a drop-in replacement for [[clojure.core/sorted-set]], the only difference being this one can't store nil.

      ClojureScript wrapper for the JavaScript implementation."}
  absurder-sql.datascript.persistent-sorted-set
  (:refer-clojure :exclude [conj disj sorted-set sorted-set-by])
  (:require
    ["../../persistent-sorted-set-js/index.js" :as pss-js]))

;; JavaScript interop helpers
(defn- js-comparator
  "Convert Clojure comparator to JavaScript comparator"
  [cmp]
  (fn [a b]
    (let [result (cmp a b)]
      (cond
        (neg? result) -1
        (pos? result) 1
        :else 0))))

(defn- clj-comparator
  "Convert JavaScript comparator to Clojure comparator"
  [js-cmp]
  (fn [a b]
    (js-cmp a b)))

;; Storage protocol matching the Java IStorage interface
(defprotocol IStorage
  (-restore [this address] "Load node from storage by address")
  (-store [this node] "Store node to storage, return address"))

;; Wrapper to adapt Clojure IStorage to JavaScript IStorage
(deftype StorageAdapter [clj-storage]
  Object
  (restore [_ address]
    (.restore clj-storage address))
  (store [_ node]
    (.store clj-storage node)))

(defn- adapt-storage
  "Adapt Clojure storage to JavaScript storage"
  [storage]
  (when storage
    (StorageAdapter. storage)))

;; Settings
(defn- settings->js
  "Convert Clojure settings map to JavaScript Settings object"
  [opts]
  (let [branching-factor (or (:branching-factor opts) 0)
        ref-type (case (:ref-type opts)
                   :strong "STRONG"
                   :soft   "SOFT"
                   :weak   "WEAK"
                   nil)]
    (new pss-js/Settings branching-factor ref-type)))

(defn- settings->clj
  "Convert JavaScript Settings object to Clojure map"
  [^js settings]
  {:branching-factor (.branchingFactor settings)
   :ref-type         (case (.refType settings)
                       "STRONG" :strong
                       "SOFT"   :soft
                       "WEAK"   :weak)})

;; Main API functions
(defn conj
  "Analogue to [[clojure.core/conj]] but with comparator that overrides the one stored in set."
  [set key cmp]
  (let [js-cmp (js-comparator cmp)]
    (.conj set key js-cmp)))

(defn disj
  "Analogue to [[clojure.core/disj]] with comparator that overrides the one stored in set."
  [set key cmp]
  (let [js-cmp (js-comparator cmp)]
    (.disj set key js-cmp)))

(defn slice
  "An iterator for part of the set with provided boundaries.
   `(slice set from to)` returns iterator for all Xs where from <= X <= to.
   `(slice set from nil)` returns iterator for all Xs where X >= from.
   Optionally pass in comparator that will override the one that set uses. Supports efficient [[clojure.core/rseq]]."
  ([set from to]
   (.slice set from to))
  ([set from to cmp]
   (let [js-cmp (js-comparator cmp)]
     (.slice set from to js-cmp))))

(defn rslice
  "A reverse iterator for part of the set with provided boundaries.
   `(rslice set from to)` returns backwards iterator for all Xs where from <= X <= to.
   `(rslice set from nil)` returns backwards iterator for all Xs where X <= from.
   Optionally pass in comparator that will override the one that set uses. Supports efficient [[clojure.core/rseq]]."
  ([set from to]
   (.rslice set from to))
  ([set from to cmp]
   (let [js-cmp (js-comparator cmp)]
     (.rslice set from to js-cmp))))

(defn seek
  "An efficient way to seek to a specific key in a seq (either returned by [[clojure.core.seq]] or a slice.)
  `(seek (seq set) to)` returns iterator for all Xs where to <= X.
  Optionally pass in comparator that will override the one that set uses."
  ([seq to]
   (when seq
     (.seek seq to)))
  ([seq to cmp]
   (when seq
     (let [js-cmp (js-comparator cmp)]
       (.seek seq to js-cmp)))))

(defn from-sorted-array
  "Fast path to create a set if you already have a sorted array of elements on your hands."
  ([cmp keys]
   (from-sorted-array cmp keys (count keys) {}))
  ([cmp keys len]
   (from-sorted-array cmp keys len {}))
  ([cmp keys len opts]
   (let [js-cmp     (js-comparator cmp)
         js-arr     (if (array? keys) keys (to-array keys))
         settings   (settings->js opts)
         storage    (adapt-storage (:storage opts))]
     (pss-js/PersistentSortedSet.from js-arr js-cmp))))

(defn from-sequential
  "Create a set with custom comparator and a collection of keys. Useful when you don't want to call [[clojure.core/apply]] on [[sorted-set-by]]."
  ([cmp keys]
   (from-sequential cmp keys {}))
  ([cmp keys opts]
   (let [js-cmp   (js-comparator cmp)
         arr      (to-array keys)
         _        (.sort arr js-cmp)
         settings (settings->js opts)
         storage  (adapt-storage (:storage opts))]
     (pss-js/PersistentSortedSet.from arr js-cmp))))

(defn sorted-set*
  "Create a set with custom comparator, metadata and settings"
  [opts]
  (let [js-cmp   (js-comparator (or (:cmp opts) compare))
        storage  (adapt-storage (:storage opts))
        settings (settings->js opts)]
    (new pss-js/PersistentSortedSet js-cmp storage settings)))

(defn sorted-set-by
  "Create a set with custom comparator."
  ([cmp]
   (pss-js/PersistentSortedSet.empty (js-comparator cmp)))
  ([cmp & keys]
   (from-sequential cmp keys)))

(defn sorted-set
  "Create a set with default comparator."
  ([]
   (pss-js/PersistentSortedSet.empty))
  ([& keys]
   (from-sequential compare keys)))

(defn restore-by
  "Constructs lazily-loaded set from storage, root address and custom comparator.
   Supports all operations that normal in-memory impl would,
   will fetch missing nodes by calling IStorage::restore when needed"
  ([cmp address storage]
   (restore-by cmp address storage {}))
  ([cmp address storage opts]
   (let [js-cmp   (js-comparator cmp)
         js-storage (adapt-storage storage)
         settings (settings->js opts)]
     (new pss-js/PersistentSortedSet js-cmp js-storage settings address nil -1 0))))

(defn restore
  "Constructs lazily-loaded set from storage and root address.
   Supports all operations that normal in-memory impl would,
   will fetch missing nodes by calling IStorage::restore when needed"
  ([address storage]
   (restore-by compare address storage {}))
  ([address storage opts]
   (restore-by compare address storage opts)))

(defn walk-addresses
  "Visit each address used by this set. Usable for cleaning up
   garbage left in storage from previous versions of the set"
  [set consume-fn]
  (.walkAddresses set consume-fn))

(defn store
  "Store each not-yet-stored node by calling IStorage::store and remembering
   returned address. Incremental, won't store same node twice on subsequent calls.
   Returns root address. Remember it and use it for restore"
  ([set]
   (.store set))
  ([set storage]
   (let [js-storage (adapt-storage storage)]
     (.store set js-storage))))

(defn settings
  "Get the settings for this set as a Clojure map"
  [set]
  (settings->clj (.-_settings set)))

;; Extend JavaScript PersistentSortedSet to implement Clojure protocols
(extend-type pss-js/PersistentSortedSet
  ISeqable
  (-seq [this]
    (let [js-seq (.seq this)]
      (when js-seq
        ;; Convert JavaScript iterator to Clojure seq
        (let [arr (.toArray js-seq)]
          (seq arr)))))

  ICounted
  (-count [this]
    (.count this))

  ILookup
  (-lookup
    ([this k]
     (when (.contains this k)
       k))
    ([this k not-found]
     (if (.contains this k)
       k
       not-found)))

  ICollection
  (-conj [this key]
    (.conj this key))

  ISet
  (-disjoin [this key]
    (.disj this key))

  IFn
  (-invoke
    ([this k]
     (-lookup this k))
    ([this k not-found]
     (-lookup this k not-found)))

  IEquiv
  (-equiv [this other]
    (if (instance? pss-js/PersistentSortedSet other)
      (.equals this other)
      (and
        (set? other)
        (= (.count this) (count other))
        (every? #(.contains this %) other))))

  IHash
  (-hash [this]
    (.hashCode this))

  IPrintWithWriter
  (-pr-writer [this writer opts]
    (let [arr (.toArray this)]
      (-write writer "#")
      (-write writer (pr-str (vec arr))))))

;; Extend JavaScript Seq to implement Clojure protocols
(when pss-js/Seq
  (extend-type pss-js/Seq
    ISeqable
    (-seq [this] this)

    ISeq
    (-first [this]
      (.first this))

    (-rest [this]
      (or (.next this) ()))

    INext
    (-next [this]
      (.next this))

    ICounted
    (-count [this]
      (loop [s this
             n 0]
        (if s
          (recur (.next s) (inc n))
          n)))

    IEquiv
    (-equiv [this other]
      (loop [s1 this
             s2 (seq other)]
        (cond
          (and (nil? s1) (nil? s2)) true
          (or (nil? s1) (nil? s2)) false
          :else (if (= (.first s1) (first s2))
                  (recur (.next s1) (next s2))
                  false))))

    IPrintWithWriter
    (-pr-writer [this writer opts]
      (let [arr (.toArray this)]
        (-write writer (pr-str (vec arr)))))))

;; Export the JavaScript classes for direct use
(def PersistentSortedSet pss-js/PersistentSortedSet)
(def Settings pss-js/Settings)
(def RefType pss-js/RefType)
