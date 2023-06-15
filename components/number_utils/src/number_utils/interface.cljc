(ns number-utils.interface
  (:require [number-utils.core :as c]))

(def ^{:argslist '([s])
       :doc      "Parses s into an integer."}
  parse-int c/parse-int)

(def ^{:argslist '([s])
       :doc      "Parses s into a long."}
  parse-long c/parse-long)

(def ^{:argslist '([units])
       :doc      "Parses s into a float."}
  parse-float c/parse-float)

(def ^{:argslist '([units])
       :doc      "Cleans units by adding/not adding a space when needed for units."}
  clean-units c/clean-units)

(def ^{:argslist '([v])
       :doc      "Returns true if a string can be parsed as a number."}
  is-numeric? c/is-numeric?)

(def ^{:argslist '([asc x y])
       :doc      "Compares two strings as numbers if they are numeric."}
  num-str-compare c/num-str-compare)

(def ^{:argslist '([n dbl])
       :doc      "Rounds a double to n significant digits."}
  to-precision c/to-precision)

(def ^{:argslist '([n dbl])
       :doc      "Sets a decimal to precision specific."}
  decimal-precision c/decimal-precision)

