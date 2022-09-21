(ns server.interface
  (:require [server.core :as core]))

(def ^{:argslist '([config])
       :doc "Starts the ring server with configuration.

            Configuration should include:
            - `:port`       Which port to bind to.
            - `:handler`    The ring handler which the server wraps.
            - `:reload?`    Whether to reload the application on every request.
            - `:defaults?`  Whether to use the default middleware."}
  start-server! core/start-server!)

(def ^{:argslist '([])
       :doc "Stops the ring server, if one has been started."}
  stop-server! core/stop-server!)

(def ^{:argslist '([])
       :doc "Creates the ring handler stack. (Mostly used development with Figwheel)."}
  create-handler-stack core/create-handler-stack)
