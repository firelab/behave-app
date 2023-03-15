(ns behave.macros
  (:require [clojure.java.io :as io]))

(defmacro inline-resource [resource-path]
  (slurp (clojure.java.io/resource resource-path)))
