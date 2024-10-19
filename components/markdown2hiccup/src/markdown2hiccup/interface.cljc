(ns markdown2hiccup.interface
  (:require [markdown2hiccup.core :as c]))

(def ^{:argslist '([s]) :doc "Converts markdown string to hiccup."}
  md->hiccup c/md->hiccup)
