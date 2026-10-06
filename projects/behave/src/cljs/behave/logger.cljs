(ns behave.logger
  "Debug logging (e.g. the solver's `[:SOLVER …]` lines). Always on in dev
  builds (`goog.DEBUG`); release builds log only while enabled at runtime —
  in the desktop app via Help → Enable Solver Logging."
  (:require [clojure.string :as str]
            [re-frame.core  :as rf]))

(defonce ^:private enabled? (atom false))

(defn ^:export set-debug-log!
  "Turn debug logging on or off at runtime. Exported (survives :advanced
  renaming) so the desktop app can call `behave.logger.set_debug_log_BANG_`."
  [on?]
  (reset! enabled? (boolean on?)))

(defn log [& s]
  ;; console.log rather than println: release builds never set *print-fn*.
  (when (or js/goog.DEBUG @enabled?)
    (js/console.log (apply str ">> [Log - Debug] " (str/join " " s)))))

;;; re-frame loggers

(def ^:private max-arg-length
  "Longest EDN rendering of a single console argument (matches the server log)."
  500)

(defn- ->console-arg
  "CLJS values as (truncated) EDN; strings and plain JS values unchanged.
  The desktop log only receives console *text*, where a CLJS value would
  otherwise print as \"[object Object]\"."
  [x]
  (if (implements? IPrintWithWriter x)
    (let [s (pr-str x)]
      (if (> (count s) max-arg-length) (str (subs s 0 max-arg-length) "…") s))
    x))

(defn- edn-logger [method]
  (fn [& args]
    (.apply (aget js/console method) js/console (to-array (map ->console-arg args)))))

(defn install-re-frame-loggers!
  "Route re-frame's console output (and libraries that log through it, such
  as re-frame-utils) through loggers that print CLJS values as EDN."
  []
  (rf/set-loggers! {:log   (edn-logger "log")
                    :warn  (edn-logger "warn")
                    :error (edn-logger "error")
                    :debug (edn-logger "debug")}))

