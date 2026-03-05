(ns behave.telemetry)

(defn- send!
  "Send a telemetry message via cefQuery. No-ops outside JCEF."
  [level message]
  (when (exists? js/window.cefQuery)
    (js/window.cefQuery
     #js {:request   (str "telemetry:" level ":" message)
          :onSuccess (fn [_])
          :onFailure (fn [_ _])})))

(defn- install-error-handlers!
  "Installs global window.onerror and unhandledrejection handlers."
  []
  (set! (.-onerror js/window)
        (fn [message source lineno colno error]
          (send! "error"
                 (str message " at " source ":" lineno ":" colno
                      (when error (str " | " (.-stack error)))))
          false))
  (.addEventListener js/window "unhandledrejection"
                     (fn [event]
                       (send! "error" (str "Unhandled rejection: " (.-reason event))))))

(defn- install-console-warn-hook!
  "Wraps console.warn to also send warnings via telemetry."
  []
  (let [original js/console.warn]
    (set! js/console.warn
          (fn [& args]
            (send! "warning" (apply str (interpose " " args)))
            (.apply original js/console (to-array args))))))

(defonce ^:private memory-interval (atom nil))

(defn- start-memory-reporting!
  "Reports performance.memory stats every 60s. Chromium-only API."
  []
  (when (and (exists? js/performance) (exists? js/performance.memory))
    (reset! memory-interval
            (js/setInterval
             (fn []
               (let [mem js/performance.memory]
                 (send! "memory"
                        (str "used=" (.-usedJSHeapSize mem)
                             " total=" (.-totalJSHeapSize mem)
                             " limit=" (.-jsHeapSizeLimit mem)))))
             60000))))

(defn init!
  "Starts all telemetry collection. Call once from client init."
  []
  (install-error-handlers!)
  (install-console-warn-hook!)
  (start-memory-reporting!))
