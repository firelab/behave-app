(ns behave.open
  (:require [behave.schema.core         :refer [all-schemas]]
            [datom-compressor.interface :as c]
            [datom-store.main           :as s]
            [logging.interface          :refer [log-str]]
            [me.raynes.fs               :as fs]
            [transport.interface        :refer [clj-> mime->type]])
  (:import (java.io ByteArrayInputStream)
           (java.util UUID)))

(def current-worksheet-atom
  "Atom to track an opened worksheet. Points to a temporary .sqlite file"
  (atom nil))

(defn- create-temp-file! [{:keys [file] :as _params}]
  (let [{:keys [tempfile filename]} file
        dest-file                   (fs/temp-file (UUID/randomUUID) ".sqlite")
        new-file                    (fs/absolute (fs/copy+ tempfile dest-file))]
    (log-str " -- CREATING TEMPORARY COPY: " (str new-file) filename (str tempfile))
    (reset! current-worksheet-atom new-file)))

(defn open-handler
  "Given a sqlite db file create a temporary copy and reset db connection to it.
  Returns datoms from the copied db."
  [{:keys [request-method params accept] :as req}]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (let [res-type (or (mime->type accept) :edn)]
    (when (= request-method :post)
      (let [temp-file (create-temp-file! params)]
        (s/release-conn!)
        (s/default-conn all-schemas
                        {:store {:backend :file
                                 :path    (str temp-file)}})
        (let [datoms (s/export-datoms @s/conn)]
          {:status  200
           :body    (if (= res-type :msgpack)
                      (ByteArrayInputStream. (c/pack datoms))
                      (clj-> datoms res-type))
           :headers {"Content-Type" accept}})))))
