(ns config.interface
  (:require [config.core :as core]))

(def ^{:argslist '([] [config-file])
       :doc "Re/loads a configuration file. Defaults to the last loaded file,
            or config.edn."}
  load-config core/load-config)

(def ^{:argslist '([& ks])
       :doc
       "Retrieves the key `k` from the config file. Can also be called with the
       multiple keys leading to a config.

       For example, with the config.edn file loaded:
       ```clojure
       {:mail {:host \"google.com\"
               :port 543}}
       ```

       Then `get-config` can be called like so:
       ```clojure
       (get-config :mail) ; => {:host \"google.com\" :port 543}
       (get-config :mail :host) ; => \"google.com\"
       ```"
       }
  get-config core/get-config)
