(ns browser-utils.interface
  (:require [browser-utils.core :as c]))

(def ^{:argslist '([f wait])
       :doc "Creates a debounced function which delays invocation until after `wait` ms have elapsed since the last time function was invoked."}
  debounce c/debounce)
