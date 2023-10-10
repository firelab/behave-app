(ns behave.download-vms
  (:require [clojure.java.io    :as io]
            [clj-http.client    :as client]
            [me.raynes.fs       :as fs]
            [file-utils.interface :refer [resource-file unzip-file]]
            [date-utils.interface :refer [today]]
            [logging.interface :refer [log-str]]))

(defn export-images-from-vms [auth-token & [url]]
  (log-str "Beginning download of images from VMS...")
  (let [{:keys [status body] :as response}
        (client/get (str (or url "https://firelab.sig-gis.com") "/clj/sync-images?auth-token=" auth-token)
                    {:as      :byte-array
                     :headers {"Accept" "application/zip"}})
        file (io/file (resource-file "public") (format "images-%s.zip" (today)))]
    (log-str response)
    (when (= status 200)
      (let [files-dir (io/file (resource-file "public") "files")]
        (io/copy body file)
        (when (.exists files-dir) (fs/delete-dir files-dir))
        (unzip-file file files-dir))
      (log-str "Completed downloading images from VMS!"))))

(defn export-from-vms [auth-token & [url]]
  (log-str "Beginning download from VMS...")
  (let [{:keys [status body] :as response}
        (client/get (str (or url "https://firelab.sig-gis.com") "/sync?auth-token=" auth-token)
                    {:as      :byte-array
                     :headers {"Accept" "application/msgpack"}})
        file (io/file (resource-file "public") "layout.msgpack")]
    (log-str response)
    (when (= status 200)
      (io/copy body file)
      (log-str "Completed downloading from VMS!"))))
