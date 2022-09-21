(ns user)

(comment
  (require '[behave.core :as core])
  (core/init!)

  (require '[fig-repl :as r])
  (r/start-figwheel!)

  ;; Connect to 1337
  (r/start-repl!))
