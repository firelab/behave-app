(ns jcef.setup
  (:import [me.friwi.jcefmaven CefAppBuilder])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- get-jcef-dir []
  (let [app-dir (System/getProperty "app.dir")
        os      (str/lower-case (System/getProperty "os.name"))]
    (if (nil? app-dir)
      ;; Dev mode
      (io/file ".jcef-bundle")
      ;; Packaged with Conveyor
      (let [jcef-dir (cond
                       (str/starts-with? os "mac")
                       (io/file app-dir "../Frameworks")

                       (str/starts-with? os "windows")
                       (io/file app-dir "jcef")

                       :else
                       (io/file app-dir "jcef"))]
        (cond
          (str/starts-with? os "mac")
          (when-not (.exists (io/file jcef-dir "jcef Helper.app"))
            (throw (IllegalStateException. "jcef Helper.app not found")))

          (str/starts-with? os "windows")
          (when-not (.exists (io/file jcef-dir "jcef.dll"))
            (throw (IllegalStateException. "jcef.dll not found")))

          :else
          (when-not (.exists (io/file jcef-dir "libjcef.so"))
            (throw (IllegalStateException. "libjcef.so not found"))))
        jcef-dir))))

(defn jcef-builder
  "Produces a `CEFAppBuilder` with the installation
   directory set according to the System OS.

   Skips installation (i.e. no Chrome/CEF download) whenever an
   install directory is already present on disk. In packaged
   Conveyor mode (`app.dir` is set) installation is always skipped
   because the bundle ships with the app."
  []
  (let [app-dir  (System/getProperty "app.dir")
        jcef-dir (get-jcef-dir)
        builder  (doto (CefAppBuilder.)
                   (.setInstallDir jcef-dir))]
    (when (or (some? app-dir)
              (.exists jcef-dir))
      (.setSkipInstallation builder true))
    builder))
