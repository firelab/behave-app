(ns behave.download-vms
  (:require [clojure.java.io    :as io]
            [clj-http.client    :as client]
            [triangulum.logging :refer [log-str]]))

(defn export-from-vms [auth-token & [url]]
  (log-str "Beginning download from VMS...")
  (let [{:keys [status body]} (client/get (or url "https://behave.sig-gis.com/sync")
                                          {:as      :byte-array
                                           :headers {"Content-Type" "application/msgpack"}})
        file                  (io/file (io/resource "public") "layout-test.msgpack")]
    (when (= status 200)
      (io/copy body file)
      (log-str "Completed downloading from VMS!"))))

(comment
  (export-from-vms "derp" "http://localhost:8082/sync")
  )
