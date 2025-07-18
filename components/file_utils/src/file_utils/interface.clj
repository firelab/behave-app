(ns file-utils.interface
  (:require [file-utils.core :as c]))

(def ^{:argslist '([file image-max-size image-quality]
                   [file image-max-size image-quality output-file])
       :doc
       "Resizes image to a maximum size, preserving aspect ratio. Also allows image quality to be adjusted.

       Operates on:
        - `file`           - Java File instance.
        - `image-max-size` - Maximum size in pixels.
        - `image-quality`  - Image quality, between 0.0 to 1.0.
        - `output-file`    - (Optional) Output image file, as a Java File instance."}
  resize-image c/resize-image)

(def ^{:argslist '([input-file-or-folder out-file]
                   [input-file-or-folder out-file opts])
       :doc 
       "compress file or folder
       - `input-file-or-folder` - filename or folder to be compressed.
       - `out-file` - filename of output archive
       - `opts` - optionals map, which can take:
         - `resize-images?` - resizes all images
         - `image-max-size` - resizes images to this max height/width, keeping the aspect ratio (default: 500)
         - `image-quality`  - compresses images to this quality (default: 0.8)
         - `rel-path?`      - replaces the path of each zip entry with the relative path
         - `new-path`       - replaces the path of each file with new-path"}
  zip-file c/zip-file)

(def 
  ^{:argslist '([input output])
    :doc
    "uncompress zip archive.
     - `input` - name of zip archive to be uncompressed.
     - `output` - name of folder where to output."}
  unzip-file c/unzip-file)

(def
  ^{:argslist '([path])
    :doc "Returns the file of a resource (safe for JAR files)."}
  resource-file c/resource-file)

(def
  ^{:argslist '([path])
    :doc "Translates a path in either Windows/Unix format into a path compatible with the current system."}
  os-path c/os-path)

(def ^{:arglist '([org-name app-name])
       :doc "Retrieve the type of OS, one of: `:windows` `:mac` `:linux`."}
  os-type c/os-type)

(def ^{:arglist '([org-name app-name])
       :doc     "Returns the OS-specific location for application data."}
  app-data-dir c/app-data-dir)
