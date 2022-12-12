(ns behave-cms.file-io
  (:import java.util.UUID)
  (:require [me.raynes.fs       :as fs]
            [clojure.set        :as set]
            [clojure.string     :as str]
            [clojure.java.io    :as io]
            [triangulum.logging :refer [log-str]]
            [behave-cms.views   :refer [data-response]]))

;;; Constants

(def files-dir (io/file "resources" "public" "files" "users"))

(def ^:private extensions {:tables    #{"csv" "xlsx"}
                           :databases #{"db"}
                           :images    #{"png" "jpg" "jpeg" "svg" "webp" "tiff"}})

(def ^:private valid-extension (apply set/union (map val extensions)))

;;; Helpers

(defn valid-extension? [f]
  (some-> f
          (fs/extension)
          (str/lower-case)
          (subs 1)
          (valid-extension)))

;;; File Manipulation

(defn delete-folder [user-uuid folder-name]
  ;; Empty contents of all folders, then delete folders
  (let [dir (io/file user-uuid folder-name)]
    (if (fs/directory? dir)
      (do
        (fs/delete-dir dir)
        (data-response {:message (format "Deleted folder: %s" folder-name)}))
      (data-response {:error "Error: folder not found."} {:status 404}))))

(defn delete-file [user-uuid filename]
  ;; Empty contents of all folders, then delete folders
  (let [file (io/file files-dir user-uuid filename)]
    (if (fs/file? file)
      (do
        (io/delete-file file)
        (data-response {:message (format "Deleted %s" filename)}))
      (data-response {:error "Error: file not found."} {:status 404}))))

(defn upload-file! [user-uuid {:keys [file] :as params}]
  (println params)
  (let [{:keys [tempfile filename]} file
        new-filename (str (UUID/randomUUID) (str/lower-case (fs/extension filename)))
        user-dir     (io/file files-dir user-uuid)]
    (println " -- UPLOADING FILE" user-dir new-filename filename tempfile)
    (if (valid-extension? filename)
      (do
        (println (format "-- COPYING FILE %s to %s." tempfile (io/file user-dir new-filename)))
        (fs/copy+ tempfile (io/file user-dir new-filename))
        (data-response {:message (format "Uploaded %s." filename)
                        :results {:filename new-filename
                                  :path (format "/files/users/%s/%s" user-uuid new-filename)}}))
      (data-response {:error "Invalid file type."} {:status 404}))))

(defn download-file [user-uuid filename]
  (let [file (io/file files-dir user-uuid filename)]
    (if (.exists file)
      (do (log-str "Download of " filename " has started.")
          {:status  200
           :headers {"Access-Control-Expose-Headers" "Content-Disposition"
                     "Content-Disposition" (str "attachment; filename=" filename)}
           :body    file})
      (do (log-str filename " not found")
          (data-response {:error "Could not find file."} {:status 404})))))

;;; Route Handler
(defn file-handler [{:keys [uri params session] :as req}]
  (println "--- File Handler:" req)
  (if (:user-uuid session)
    (let [{:keys [user-uuid]} session
          [_ action filename] (->> (str/split uri #"/")
                                   (remove str/blank?))]
      (cond
        (= action "upload")        (upload-file! user-uuid params)
        (= action "download")      (download-file user-uuid filename)
        (= action "delete")        (delete-file user-uuid filename)
        (= action "delete-folder") (delete-folder user-uuid filename)
        :else                      (data-response 400 (str uri " is not a valid file operation."))))
    (data-response {:error (str "You must be logged in to perform that operation.")} {:status 403})))

(comment

  (def user-dir (io/file files-dir "75d20caf-b7d8-4fe5-acea-10e30cbda4c3"))
  (fs/mkdirs user-dir)
  (def tmp-file (io/file "/var/folders/61/_c18bnls1_ngj45g64_7nx2m0000gn/T/ring-multipart-2966144437477874063.tmp"))

  (def new-file (io/file user-dir (str (UUID/randomUUID) (fs/extension "287.jpg"))))
  (fs/rename tmp-file new-file)
  (fs/exists? new-file)

  (first (fs/glob user-dir "*.jpg"))
  )
