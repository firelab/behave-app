(ns csv-parser.core
  (:require [clojure.core.async :refer [go <!]]
            [clojure.string     :as str]
            #?(:clj [clojure.data.csv :as csv])
            #?(:clj [clojure.java.io :as io])
            #?(:cljs [cljs.core.async.interop :refer-macros [<p!]])))

(defn- numeric? [n]
  (re-matches #"^[0-9].*" n))

(defn parse-csv [text]
  #?(:clj
     (prn "Not implemented yet.")

     :cljs
     (let [[header & rows] (str/split text #"\n")
           header          (str/split header #",")]
       (mapv
        (fn [row]
          (into {} (map-indexed (fn [i col]
                                  [(nth header i)
                                   (if (numeric? col)
                                     (js/parseFloat col) col)])
                                (str/split row #","))))
        rows))))

(defn- csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data)
            (map keyword)
            repeat)
       (rest csv-data)))

(defn fetch-csv [csv-url]
  #?(:clj
     (with-open [reader (io/reader csv-url)]
       (csv-data->maps (doall (csv/read-csv reader))))

     :cljs
     (go
       (let [response (<p! (.fetch js/window csv-url))
             text     (<p! (.text response))]
         text))))
