(ns behave.browser-log
  "Captures the embedded browser's console messages for this session and
  shows them in a desktop window (Help → Show Browser Logs). Every message is
  also written to the log file; this is the in-app view of the same lines."
  (:require [clojure.java.io :as io]
            [clojure.string  :as str])
  (:import [java.awt BorderLayout Desktop Font Toolkit]
           [java.awt.datatransfer StringSelection]
           [java.awt.event ActionListener]
           [java.time LocalTime]
           [java.time.format DateTimeFormatter]
           [javax.swing JButton JFrame JLabel JPanel JScrollPane JTextArea SwingUtilities WindowConstants]))

;;; Session buffer

(def ^:private max-entries
  "Messages kept in memory; older ones remain in the log file."
  5000)

(defonce ^:private entries (atom []))

(defonce ^:private viewer (atom nil))

(def ^:private time-format (DateTimeFormatter/ofPattern "HH:mm:ss"))

(defn- trim-to-max [es]
  (if (> (count es) max-entries)
    (subvec es (- (count es) max-entries))
    es))

(defn record!
  "Add a console message `{:level :message :source :line}` to the session buffer."
  [msg]
  (swap! entries #(trim-to-max (conj % (assoc msg :time (.format (LocalTime/now) time-format))))))

(defn- format-entry [{at :time :keys [level source line message]}]
  (format "%s %-7s %s:%s  %s"
          at
          (str/replace (str level) "LOGSEVERITY_" "")
          source
          line
          message))

(defn- all-text []
  (str/join "\n" (map format-entry @entries)))

;;; Viewer window

(defn- button [label f]
  (doto (JButton. ^String label)
    (.addActionListener (proxy [ActionListener] []
                          (actionPerformed [_] (f))))))

(defn- copy-to-clipboard! [s]
  (.setContents (.getSystemClipboard (Toolkit/getDefaultToolkit)) (StringSelection. s) nil))

(defn- open-folder! [log-dir]
  (let [dir (.getAbsoluteFile (io/file log-dir))]
    (.mkdirs dir)
    (.open (Desktop/getDesktop) dir)))

(defn- header-text [log-dir]
  (format "Browser console messages this session (last %d). Full history: %s"
          max-entries
          (.getAbsolutePath (io/file log-dir))))

(defn- selection-or-all
  "Selected text in `text-area`, or all of it when nothing is selected."
  [^JTextArea text-area]
  (let [selected (.getSelectedText text-area)]
    (if (str/blank? selected) (.getText text-area) selected)))

(defn- button-bar [text-area log-dir]
  (doto (JPanel.)
    (.add (button "Copy" #(copy-to-clipboard! (selection-or-all text-area))))
    (.add (button "Copy All" #(copy-to-clipboard! (.getText ^JTextArea text-area))))
    (.add (button "Open Log Folder" #(open-folder! log-dir)))))

(defn- build-viewer
  "Create the (hidden) viewer frame; returns `{:frame :text}`."
  [log-dir]
  (let [text  (doto (JTextArea. ^String (all-text))
                (.setEditable false)
                (.setFont (Font. Font/MONOSPACED Font/PLAIN 12)))
        frame (doto (JFrame. "Behave7 – Browser Logs")
                (.setDefaultCloseOperation WindowConstants/HIDE_ON_CLOSE)
                (.setSize 1000 600))]
    (doto (.getContentPane frame)
      (.add (JLabel. ^String (header-text log-dir)) BorderLayout/NORTH)
      (.add (JScrollPane. text) BorderLayout/CENTER)
      (.add (button-bar text log-dir) BorderLayout/SOUTH))
    {:frame frame :text text}))

(defn- append-latest!
  "Watch fn: append the newest entry to an open viewer (one entry per `record!`)."
  [_ _ _ new-entries]
  (when-let [{:keys [^JTextArea text]} @viewer]
    (when-let [entry (peek new-entries)]
      (SwingUtilities/invokeLater
       #(doto text
          (.append (str (when (pos? (.getLength (.getDocument text))) "\n") (format-entry entry)))
          (.setCaretPosition (.getLength (.getDocument text))))))))

(defn show-viewer!
  "Show the browser log window, creating it on first use; `log-dir` is the
  folder its Open Log Folder button opens."
  [log-dir]
  (add-watch entries ::viewer append-latest!) ; same key: re-adding replaces it
  (SwingUtilities/invokeLater
   #(let [{:keys [^JFrame frame]} (or @viewer (reset! viewer (build-viewer log-dir)))]
      (doto frame
        (.setVisible true)
        (.toFront)))))

;;; Solver (debug) logging toggle

(defn- solver-logging-file
  "Presence of this file in `app-data-dir` means solver logging is on."
  [app-data-dir]
  (io/file app-data-dir "solver-logging.on"))

(defn solver-logging?
  "True when solver logging was left enabled (persists across restarts)."
  [app-data-dir]
  (.exists (solver-logging-file app-data-dir)))

(defn push-solver-logging!
  "Tell the page in `browser` (a CefBrowser) whether to emit debug logs."
  [browser on?]
  (.executeJavaScript browser (format "behave.logger.set_debug_log_BANG_(%s)" (boolean on?)) "" 0))

(defn set-solver-logging!
  "Persist the solver logging choice under `app-data-dir` and apply it to `browser`."
  [app-data-dir browser on?]
  (let [f (solver-logging-file app-data-dir)]
    (if on?
      (do (io/make-parents f) (spit f ""))
      (io/delete-file f true)))
  (push-solver-logging! browser on?))

