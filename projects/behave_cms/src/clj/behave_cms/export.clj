(ns behave-cms.export
  (:require [file-utils.interface :refer [zip-file unzip-file resize-image]]))

;;; Constants

(def ^:private image-extensions #{".png" ".jpg" ".jpeg" ".svg" ".webp" ".tiff"})
(def ^:private resize-max-width 500)
(def ^:private resize-quality   0.8)

(defn today []
  (.format (java.time.LocalDateTime/now)
           (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd")))

(defn sync-images [_req]
  (let [public-files    (io/resource "public/files")
        zipped-filename (str "all-images-" (today) ".zip")
        zip-path        (io/file public-files "archives" zipped-filename)]
    (when-not (.exists zip-path)
      (io/make-parents zip-path)
      (zip-file (io/resource "public/files/users")
                (.getPath zip-path)
                {:new-path "./images/" :resize-images? true}))
    {:status  200
     :body    (FileInputStream. zip-path)
     :headers {"Content-Type" "application/zip"
               "Content-Disposition" (str "attachment; filename=" zipped-filename)}}))
