(ns csv-parser.interface
  (:require [csv-parser.core :as c]))

(def ^{:argslist ([csv-text])
       :doc "Parses CSV string into a vector of maps. Numbers are converted to floats by default."}

  parse-csv c/parse-csv)

(def ^{:argslist ([csv-url])
       :doc "Fetches CSV from a URL."}

  fetch-csv c/fetch-csv)
