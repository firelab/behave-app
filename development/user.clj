(ns user
  (:require [behave.core :as core]
            [fig-repl :as r]))

(comment
  (core/init!)

  (r/start-figwheel!)

  ;; Connect to 1337
  (r/start-repl!))
