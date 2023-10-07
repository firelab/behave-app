(ns behave-cms.export
  (:require [clojure.java.io :as io]
            [clojure.string  :as str]
            [file-utils.interface :refer [resource-file zip-file]]
            [date-utils.interface :refer [today]])
  (:import [java.io FileInputStream]))

(defn sync-images
  "Creates and returns a zipped archive of resized images."
  ([] (sync-images nil))
  ([_req]
   (let [public-files    (io/file (resource-file "public") "files")
         zipped-filename (str "all-images-" (today) ".zip")
         zip-path        (io/file public-files "archives" zipped-filename)
         new-path        (fn [path] (str/replace path #".*files" "."))]
     (when-not (.exists zip-path)
       (io/make-parents zip-path)
       (zip-file (io/file public-files "users")
                 (.getPath zip-path)
                 {:resize-images? true :new-path new-path}))
     {:status  200
      :body    (FileInputStream. zip-path)
      :headers {"Content-Type"        "application/zip"
                "Content-Disposition" (str "attachment; filename=" zipped-filename)}})))
