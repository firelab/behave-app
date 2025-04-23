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
   directory set according to the System OS."
  [cache-path]
  (let [builder (CefAppBuilder.)]
    (doto builder
      (.setInstallDir (get-jcef-dir)))
    (when cache-path
      (set! (.cache_path (.getCefSettings builder)) cache-path))
    builder))
