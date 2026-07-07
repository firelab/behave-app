(ns behave.main
  "Minimal launch shim (see STARTUP.org). Loading the full application
  namespace tree costs ~17 CPU-seconds of class-init on slow Windows
  hardware, so this namespace requires almost nothing: it shows the splash
  screen first, then loads the app via `requiring-resolve` while the splash
  is visible. Keep the `:require` list empty of application namespaces —
  anything added here loads *before* the splash can appear."
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:import [java.lang ProcessHandle]))

(defn- uptime-ms
  "Milliseconds since the process started."
  []
  (let [start (-> (ProcessHandle/current) .info .startInstant (.orElse nil))]
    (if start
      (- (System/currentTimeMillis) (.toEpochMilli start))
      -1)))

(defn -main
  "Unified entry point. In desktop (Conveyor) mode shows the splash before
  the application namespaces load, then hands off to [[behave.core/start-cef!]]."
  [& _args]
  (println (str "[TIMING] -main entered " (uptime-ms) "ms after JVM start"))
  (if (System/getProperty "app.dir")
    ;; jcef.loading (not jcef.interface) on purpose: the interface ns pulls
    ;; in jcef.core/org.cef classes, which the splash doesn't need.
    (let [show-loader! (requiring-resolve 'jcef.loading/show-loader!)
          loader       (show-loader! "Behave7" (io/resource "public/images/android-chrome-512x512.png"))]
      (println (str "[TIMING] splash shown " (uptime-ms) "ms after JVM start"))
      ((requiring-resolve 'behave.core/start-cef!) loader))
    (do
      ((requiring-resolve 'behave.server/start-server!))
      ;; Jetty runs with :join? false — park the main thread (see behave.core/-main).
      @(promise))))
