(ns version-utils.interface
  (:require [version-utils.core :as c]))

(def ^{:argslist '[s]
       :doc      "Parse a dotted version string like \"7.1.4\" into [7 1 4].
            Returns nil for nil, blank, or non-numeric input."}
  parse c/parse)

(def ^{:argslist '[a b]
       :doc      "Compare two versions (strings or pre-parsed vectors). Returns -1/0/1.
            nil sorts before any concrete version; shorter vectors pad with 0
            so \"7.1\" == \"7.1.0\"."}
  compare-versions c/compare-versions)
