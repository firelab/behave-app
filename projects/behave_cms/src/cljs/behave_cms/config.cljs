(ns behave-cms.config)

(defonce ^:private config (atom {}))

(defn update-config [new-config]
  "Updates config map to `new-config`."
  (reset! config new-config))

(defn get-config
  [& keys]
  "Retrieves key from config. Can pass in multiple keys to use `get-in`.

  ```clojure
  (get-config :my-secret) ;; Top level key
  (get-config :rest-api :login-route) ;; Nested keys
  ```"
  (get-in @config keys))

(comment
  (update-config {:secret "HELLO"})
  (get-config :secret)
  )
