(ns jcef.setup
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.awt GraphicsEnvironment]
           [javax.swing JOptionPane]
           [me.friwi.jcefmaven CefAppBuilder CefBuildInfo EnumPlatform]))

;;; Bundle location

(defn- os []
  (let [os-name (str/lower-case (System/getProperty "os.name"))]
    (cond
      (str/starts-with? os-name "mac")     :mac
      (str/starts-with? os-name "windows") :windows
      :else                                :linux)))

(def ^:private native-marker
  "File whose presence shows a JCEF bundle holds this OS's natives."
  {:mac "jcef Helper.app" :windows "jcef.dll" :linux "libjcef.so"})

(defn- bundle-dir
  "JCEF bundle directory of a packaged app rooted at `app-dir`."
  [app-dir]
  (if (= :mac (os))
    (io/file app-dir "../Frameworks")
    (io/file app-dir "jcef")))

(defn- has-natives? [dir]
  (.exists (io/file dir (native-marker (os)))))

(defn- jar-dir
  "Directory holding the jar this app runs from, or nil outside a jar."
  []
  (try
    (let [f (-> CefAppBuilder .getProtectionDomain .getCodeSource .getLocation .toURI io/file)]
      (when (.isFile f) (.getParentFile f)))
    (catch Exception _ nil)))

(defn- packaged-app-dir
  "App directory of a packaged install: Conveyor's `app.dir`, else the
   directory of the running jar when a JCEF bundle sits beside it (e.g.
   `app\\behave7.jar` launched directly with `java -jar`)."
  []
  (or (System/getProperty "app.dir")
      (when-let [dir (jar-dir)]
        (when (has-natives? (bundle-dir dir)) (str dir)))))

;;; Installation check

(defn- build-info-problem
  "Why `build_meta.json` in `dir` doesn't match this jcefmaven, or nil."
  [dir]
  (try
    (let [installed (CefBuildInfo/fromFile (io/file dir "build_meta.json"))
          required  (CefBuildInfo/fromClasspath)
          platform  (.getIdentifier (EnumPlatform/getCurrentPlatform))]
      (cond
        (not= (.getReleaseTag installed) (.getReleaseTag required))
        (format "bundle is %s but jcefmaven expects %s"
                (.getReleaseTag installed) (.getReleaseTag required))

        (not= (.getPlatform installed) platform)
        (format "bundle is for %s, not %s" (.getPlatform installed) platform)))
    (catch Exception e
      (str "build_meta.json unreadable: " (.getMessage e)))))

(defn- installation-problem
  "Why `dir` is not a complete JCEF installation, or nil when it is.
   Mirrors jcefmaven's `CefInstallationChecker`, but names the reason."
  [dir]
  (cond
    (not (has-natives? dir))                         (str (native-marker (os)) " not found")
    (not (.exists (io/file dir "install.lock")))     "install.lock not found"
    (not (.exists (io/file dir "build_meta.json")))  "build_meta.json not found"
    :else                                            (build-info-problem dir)))

;;; Install policy

(defn- packaged-installation
  "Packaged installs ship their bundle and never install. Missing natives
   are fatal; any other problem is only reported."
  [app-dir]
  (let [dir     (bundle-dir app-dir)
        problem (installation-problem dir)]
    (if (has-natives? dir)
      {:dir dir :skip-install? true :warning problem}
      {:dir           dir
       :skip-install? true
       :error         (format "JCEF bundle missing from %s (%s). Reinstall Behave7."
                              (.getCanonicalPath dir) problem)})))

(defn- dev-installation
  "Dev installs use `dir` (`.jcef-bundle`), installing (downloading) it only
   when `allow-download?`."
  [dir allow-download?]
  (let [problem (installation-problem dir)]
    (cond
      (nil? problem)  {:dir dir :skip-install? true}
      allow-download? {:dir dir :skip-install? false :warning (str "installing JCEF: " problem)}
      :else           {:dir           dir
                       :skip-install? true
                       :error         (format (str "JCEF not installed at %s (%s). Start Behave7 with "
                                                   "bin\\Behave7.exe, or set -Dbehave.jcef.allow-download=true "
                                                   "for development.")
                                              (.getCanonicalPath dir) problem)})))

(defn- installation
  "Decides where JCEF lives and whether jcefmaven may install (download) it.
   Returns `{:dir :skip-install? :warning :error}`."
  [app-dir allow-download?]
  (if app-dir
    (packaged-installation app-dir)
    (dev-installation (io/file ".jcef-bundle") allow-download?)))

(defn- warn! [msg]
  (binding [*out* *err*]
    (println "[JCEF]" msg)))

(defn- fail! [msg]
  (warn! msg)
  (when-not (GraphicsEnvironment/isHeadless)
    (JOptionPane/showMessageDialog nil msg "Behave7 could not start" JOptionPane/ERROR_MESSAGE))
  (throw (IllegalStateException. ^String msg)))

(defn jcef-builder
  "Produces a `CefAppBuilder` pointed at the JCEF bundle.

   Never downloads Chrome/CEF for a packaged install (Conveyor's `app.dir`,
   or a jar with a bundle beside it). Outside a packaged install, downloads
   into `.jcef-bundle` only when `-Dbehave.jcef.allow-download=true`;
   otherwise shows an error and throws."
  []
  (let [{:keys [dir skip-install? warning error]}
        (installation (packaged-app-dir) (Boolean/getBoolean "behave.jcef.allow-download"))]
    (when error (fail! error))
    (when warning (warn! warning))
    (doto (CefAppBuilder.)
      (.setInstallDir dir)
      (.setSkipInstallation skip-install?))))
