(ns behave.open
  (:require [datom-store.main :as s]
            [datom-compressor.interface :as c]
            [transport.interface :refer [clj-> mime->type]]
            [behave.schema.core :refer [all-schemas]]
            [logging.interface  :refer [log-str]]
            [me.raynes.fs       :as fs]
            [clojure.string     :as str]
            [clojure.java.io    :as io])
  (:import  [java.io
             ByteArrayInputStream]
            java.util.UUID))

(def current-worksheet-atom (atom nil))

(defn upload-file! [{:keys [file] :as _params}]
  (let [{:keys [tempfile filename]} file
        new-filename                (str (UUID/randomUUID) (str/lower-case (fs/extension filename)))
        new-file                    (fs/absolute (fs/copy+ tempfile (io/file new-filename)))]
    (println " -- UPLOADING FILE" new-filename filename tempfile)
    (reset! current-worksheet-atom new-file)))

(defn open-handler [{:keys [request-method params accept] :as req}]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (let [res-type (or (mime->type accept) :edn)]
    (when (= request-method :post)
      (let [file (upload-file! params)]
        (s/release-conn!)
        (s/default-conn all-schemas
                        {:store {:backend :file
                                 :path    (str file)}})
        (let [datoms (s/export-datoms @s/conn)]
          {:status  200
           :body    (if (= res-type :msgpack)
                      (ByteArrayInputStream. (c/pack datoms))
                      (clj-> datoms res-type))
           :headers {"Content-Type" accept}})))))
