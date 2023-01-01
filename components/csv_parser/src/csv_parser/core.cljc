(ns csv-parser.core
  (:require [clojure.core.async :refer [go <!]]
            [clojure.string     :as str]
            #?(:cljs [cljs.core.async.interop :refer-macros [<p!]])))

(defn- numeric? [n]
  (re-matches #"^[0-9].*" n))

(defn parse-csv [text & [col-parser]]
  (let [[header & rows] (str/split text #"\n")
        header          (str/split header #",")]
    (mapv
     (fn [row]
       (into {} (map-indexed (fn [i col]
                               [(nth header i)
                                (if (numeric? col)
                                  (js/parseFloat col) col)])
                             (str/split row #","))))
     rows)))

(defn fetch-csv [csv-url]
  #?(:clj
     (println "Not implemented yet.")

     :cljs
     (go
       (let [response (<p! (.fetch js/window csv-url))
             text     (<p! (.text response))]
         text))))
