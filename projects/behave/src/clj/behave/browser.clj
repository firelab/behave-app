(ns behave.browser
  "Opens the app in a Chromium-based browser's app window, falling back to
  the default browser."
  (:require [clojure.java.browse  :refer [browse-url]]
            [clojure.java.io      :as io]
            [clojure.string       :as str]
            [file-utils.interface :refer [os-type]]
            [logging.interface    :refer [log-str]])
  (:import [java.io File]
           [java.lang ProcessBuilder$Redirect]))

;;; Detection

(defn- windows-candidates []
  (for [exe   ["Google/Chrome/Application/chrome.exe"
               "Microsoft/Edge/Application/msedge.exe"]
        env   ["ProgramFiles" "ProgramFiles(x86)" "LOCALAPPDATA"]
        :let  [dir (System/getenv env)]
        :when dir]
    (io/file dir exe)))

(defn- mac-candidates []
  (for [dir ["/Applications" (str (System/getProperty "user.home") "/Applications")]]
    (io/file dir "Google Chrome.app/Contents/MacOS/Google Chrome")))

(defn- linux-candidates []
  (for [exe ["google-chrome" "google-chrome-stable" "chromium" "chromium-browser"]
        dir (str/split (or (System/getenv "PATH") "") (re-pattern File/pathSeparator))]
    (io/file dir exe)))

(defn- chromium-path
  "Path to an installed Chrome (or Edge on Windows), or nil."
  []
  (some->> (case (os-type)
             :windows (windows-candidates)
             :mac     (mac-candidates)
             :linux   (linux-candidates))
           (filter #(.canExecute ^File %))
           (first)
           (str)))

;;; Launch

(defn- open-app-window!
  "Opens `url` in an `--app` window of the browser at `exe`."
  [exe url]
  (-> (ProcessBuilder. [exe (str "--app=" url)])
      (.redirectOutput ProcessBuilder$Redirect/DISCARD)
      (.redirectError ProcessBuilder$Redirect/DISCARD)
      (.start)))

(defn open!
  "Opens `url` in a Chrome app window, or in the default browser when Chrome
  isn't installed or fails to start."
  [url]
  (if-let [exe (chromium-path)]
    (try
      (log-str "Opening " url " in " exe)
      (open-app-window! exe url)
      (catch Exception e
        (log-str "Couldn't start " exe ", using the default browser: " (ex-message e))
        (browse-url url)))
    (browse-url url)))
