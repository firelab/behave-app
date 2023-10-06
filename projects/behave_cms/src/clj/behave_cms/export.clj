(ns behave-cms.export
  (:require [clojure.java.io :as io]
            [file-utils.interface :refer [resource-file zip-file]]
            [date-utils.interface :refer [today]])
  (:import [java.io FileInputStream]))

(defn sync-images-handler
  "Creates and returns a zipped archive of resized images."
  [_req]
  (let [public-files    (io/file (resource-file "public") "files")
        zipped-filename (str "all-images-" (today) ".zip")
        zip-path        (io/file public-files "archives" zipped-filename)]
    (when-not (.exists zip-path)
      (io/make-parents zip-path)
      (zip-file (io/file public-files "users")
                (.getPath zip-path)
                {:new-path "./images/" :resize-images? true}))
    {:status  200
     :body    (FileInputStream. zip-path)
     :headers {"Content-Type" "application/zip"
               "Content-Disposition" (str "attachment; filename=" zipped-filename)}}))
