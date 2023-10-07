(ns date-utils.interface
  (:require [date-utils.core :as c]))

(def ^{:argslist '()
       :doc "Today's date in string format 'yyyy-MM-dd'."}
 today c/today)
