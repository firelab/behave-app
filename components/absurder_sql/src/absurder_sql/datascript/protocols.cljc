(ns absurder-sql.datascript.protocols)

(defprotocol ^:export IStorage
  :extend-via-metadata true

  (-store [_ addr+data-seq]
    "Gives you a sequence of `[addr data]` pairs to serialize and store.

     `addr`s are 64 bit integers.
     `data`s are clojure-serializable data structure (maps, keywords, lists, integers etc)")

  (-restore [_ addr]
    "Read back and deserialize data stored under single `addr`")

  (-list-addresses [_]
    "Return seq that lists all addresses currently stored in your storage.
     Will be used during GC to remove keys that are no longer used.")

  (-delete [_ addrs-seq]
    "Delete data stored under `addrs` (seq). Will be called during GC"))

;; Storage protocol matching the Java IStorage interface
(defprotocol IPersistentSortedSetStorage
  (accessed [this address]
    "Tell the storage layer that address is accessed")

  (restore [this address]
    "Load node from storage by address")

  (store [this node]
    "Store node to storage, return address"))
