(ns behave-cms.export
  (:require [clojure.java.io :refer [file output-stream input-stream] :as io]
            [clojure.string  :as str]
            [me.raynes.fs    :as fs])
  (:import [java.io File OutputStream ByteArrayInputStream ByteArrayOutputStream]
           [javax.imageio ImageIO]
           [java.awt.image BufferedImage]
           [java.util.zip ZipEntry ZipOutputStream ZipInputStream]
           [net.coobird.thumbnailator Thumbnailator Thumbnails]))

;;; Constants

(def ^:private image-extensions #{".png" ".jpg" ".jpeg" ".svg" ".webp" ".tiff"})
(def ^:private resize-max-width 500)
(def ^:private resize-quality   0.8)

;;; Image Resizing

(defn write-image [^BufferedImage image format-name ^OutputStream output-stream]
  (println "Writing Image" image format-name output-stream)
  (ImageIO/write image format-name output-stream))

(defn resize-image [file]
  (let [output-stream  (ByteArrayOutputStream.)
        buffered-image (-> (doto (Thumbnails/fromFilenames [(.getPath file)])
                             (.size 500 500)
                             (.outputQuality 0.8))
                           (.asBufferedImage))]
    (try 
      (write-image buffered-image (subs (fs/extension file) 1) output-stream)
      (finally (.close output-stream)))
    output-stream))

;; From: https://gist.github.com/mikeananev/b2026b712ecb73012e680805c56af45f

(defn zip-file
  "compress file or folder
  `input-file-or-folder` - filename or folder to be compressed.
  `out-file` - filename of output archive
  `opts` - optionals map, which can take:
    - `resize-images?` - resizes all images to max width of 500px with 80% quality
    - `rel-path?` - replaces the path of each zip entry with the relative path
    - `new-path` - replaces the path of each file with new-path"
  [input-file-or-folder out-file & [{:keys [rel-path? new-path resize-images?]}]]
  (with-open [zip (ZipOutputStream. (io/output-stream out-file))]
    (doseq [f (file-seq (io/file input-file-or-folder)) :when (.isFile f)]
      (.putNextEntry zip (ZipEntry. (cond
                                      new-path  (str new-path (fs/base-name f)) 
                                      rel-path? (str/replace (.getPath f) (.getPath fs/*cwd*) ".") 
                                      true      (.getPath f))))
      (if (and resize-images? (image-extensions (fs/extension f)))
        (io/copy (ByteArrayInputStream. (.toByteArray (resize-image f))) zip)
        (io/copy f zip))
      (.closeEntry zip))))

(defn unzip-file
  "uncompress zip archive.
  `input` - name of zip archive to be uncompressed.
  `output` - name of folder where to output."
  [input output]
  (with-open [stream (-> input io/input-stream ZipInputStream.)]
    (loop [entry (.getNextEntry stream)]
      (if entry
        (let [save-path (str output File/separatorChar (.getName entry))
              out-file (File. save-path)]
          (if (.isDirectory entry)
            (if-not (.exists out-file)
              (.mkdirs out-file))
            (let [parent-dir (File. (.substring save-path 0 (.lastIndexOf save-path (int File/separatorChar))))]
              (if-not (.exists parent-dir) (.mkdirs parent-dir))
              (io/copy stream out-file)))
          (recur (.getNextEntry stream )))))))


(comment

  (zip-file (io/resource "public/files") "files.zip" {:new-path "./images/" :resize-images? true})
  (unzip-file "files.zip" "new-io")

  (files/delete-file "files.zip")

  (fs/delete-dir "new-files")
  (file-seq (io/file (io/resource "public/files")))

  (doseq [f (file-seq (io/file (io/resource "public/files"))) :when (and (.isFile f) (#{".png" ".jpg" ".jpeg"} (fs/extension f)))]
    (let [bi (.asBufferedImage
              (doto Thumbnails
                      (of f)
                      (size 300)
                      (outputQuality 0.8)))]
      (println [:OLD (str/replace (.getPath f) (System/getProperty "user.dir") ".")
                :BASENAME (fs/base-name f)
                :WIDTH    (.getWidth bi)]))
    #_(println (.getPath f)))

  (def img (first (filter #(and (.isFile %) (#{".png" ".jpg" ".jpeg"} (fs/extension %))) (file-seq (io/file (io/resource "public/files"))))))

  (def bi (.asBufferedImage (doto (Thumbnails/fromFilenames [(.getPath img)])
                              (.size 400 400)
                              (.outputQuality 0.8))))
  (def os (ByteArrayOutputStream.))


  (type bi)
  (type image)

  ;; Example usage
  (def image (BufferedImage. 100 100 BufferedImage/TYPE_INT_RGB))
  (def format-name "png")
  (def my-stream (ByteArrayOutputStream.))

  (try 
    (write-image bi format-name (io/file "my-test.png"))
    (finally
      (.close my-stream)))

  (subs (fs/extension img) 1)
  (resize-image img)
  (fs/base-name img)


  (.of Thumbnails [(.getPath img)])


  )
