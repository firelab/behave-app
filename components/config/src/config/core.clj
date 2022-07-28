(ns config.core
  (:require [clojure.java.io :as io]
            [clojure.edn     :as edn]))

;;; Private vars

(def ^:private config-file  (atom (io/resource "config.edn")))
(def ^:private config-cache (atom nil))

;;; Helper Fns

(defn- wrap-throw [& strs]
  (-> (apply str strs)
      (ex-info {})
      (throw)))

(defn- read-config [file]
  (if file
    (-> (slurp file) (edn/read-string))
    (wrap-throw "Error: Cannot find file" file)))

(defn- cache-config []
  (or @config-cache
      (reset! config-cache (read-config @config-file))))

;;; Public Fns

(defn load-config
  "Re/loads a configuration file. Defaults to the last loaded file, or config.edn."
  ([]
   (load-config @config-file))
  ([new-config-file]
   (reset! config-file new-config-file)
   (reset! config-cache (read-config @config-file))))

(defn get-config
  "Retrieves the key `k` from the config file.
   Can also be called with the keys leading to a config.
   Examples:
   ```clojure
   (get-config :mail) -> {:host \"google.com\" :port 543}
   (get-config :mail :host) -> \"google.com\"
   ```"
  [& all-keys]
  (get-in (cache-config) all-keys))
