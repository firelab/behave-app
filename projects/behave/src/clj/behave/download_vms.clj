(ns behave.download-vms
  (:require [clojure.java.io    :as io]
            [clj-http.client    :as client]
            [triangulum.logging :refer [log-str]]))

(defn export-from-vms [auth-token & [url]]
  (log-str "Beginning download from VMS...")
  (let [{:keys [status body] :as response}
        (client/get (str (or url "https://behave.sig-gis.com") "/sync?auth-token=" auth-token)
                    {:as      :byte-array
                     :headers {"Accept" "application/msgpack"}})
        file (io/file (io/resource "public") "layout.msgpack")]
    (log-str response)
    (when (= status 200)
      (io/copy body file)
      (log-str "Completed downloading from VMS!"))))
