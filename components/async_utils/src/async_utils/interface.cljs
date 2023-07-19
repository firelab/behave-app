(ns async-utils.interface
  (:require [async-utils.core :as c]))

(def ^{:argslist '([mime])
       :doc "Determine if it is a channel."}
  chan? c/chan?)

(def ^{:argslist
       '([on-refresh-fn interval])

       :doc
       "Refreshes the specified function every specified interval (ms) of time.
       Exit the go-loop by doing `put! exit-chan :exit` elsewhere in the code.
       Use stop-refresh! for simplicity"}
  refresh-on-interval! c/refresh-on-interval!)

(def ^{:argslist '([exit-chan])
       :doc "Take a chan from refresh-on-interval! and stops the refresh."}
  stop-refresh! c/stop-refresh!)

(def ^{:argslist '([p])
       :doc "Determine if it is a channel."}
  promise? c/promise?)

(def ^{:argslist
       '([url] [url options])

       :doc
       "Launches a js/window.fetch operation. Returns a channel that will
       receive the response or nil if a network error occurs. The options
       map will be automatically converted to a JS object for the fetch
       call."}
  fetch c/fetch)

(def ^{:argslist
       '([url options process-fn])

       :doc
       "Launches a js/window.fetch operation and runs process-fn on the
       successful result. HTTP Errors and Network Errors raised by the
       fetch are printed to the console. The options map will be
       automatically converted to a JS object for the fetch call. Returns a
       channel with the result of process-fn. If process-fn returns a
       channel or promise, these will be taken from using <! or <p!
       respectively."}
  fetch-and-process c/fetch-and-process)

(def ^{:argslist
       '([method url data auth-token])

       :doc
       "Launches a js/window.fetch operation and runs process-fn on the
       channel with the result of process-fn. If process-fn returns a
       channel or promise, these will be taken from using <! or <p!
       respectively."}
  call-remote! c/call-remote!)

(def ^{:argslist '([error])
       :doc "Returns a humanized error of the SQL error."}
  show-sql-error c/show-sql-error!)

(def ^{:argslist '([sql-fn-name & args])
       :doc
       "Calls SQL function from the backend and returns a go block
       containing the function's response."}
  call-sql-async! c/call-sql-async!)

(def ^{:argslist '([clj-fn-name & args])
       :doc
       "Calls a Clojure function from the backend and returns a go block
       containing the function's response."}
  call-clj-async! c/call-clj-async!)
