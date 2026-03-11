(ns behave.test.fixture
  "Shared E2E test fixture. Keeps a single browser/CDP session alive across
   all test namespaces. Chrome is cleaned up at JVM shutdown."
  (:require [behave.test.lifecycle :as lifecycle]))

(defonce ^:private shared-state (atom nil))

(defonce ^:private shutdown-hook-registered
  (delay
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. (fn []
                (when-let [state @shared-state]
                  (println "[fixture] JVM shutting down, stopping browser")
                  (try
                    (lifecycle/stop-app! state)
                    (catch Exception e
                      (println "[fixture] Error stopping browser:" (.getMessage e))))
                  (reset! shared-state nil)))))))

(defn e2e-fixture
  "Kaocha :once fixture. Acquires (or reuses) a shared browser session.
   Chrome is started on first use and cleaned up at JVM shutdown."
  [f]
  @shutdown-hook-registered
  (when (nil? @shared-state)
    (let [target     (keyword (or (System/getProperty "behave.e2e.target") "browser"))
          http-port  (Integer/parseInt (or (System/getProperty "behave.e2e.http-port") "4242"))
          debug-port (Integer/parseInt (or (System/getProperty "behave.e2e.debug-port") "9222"))]
      (println "[fixture] Starting browser session")
      (println "[fixture] target:" target "http-port:" http-port "debug-port:" debug-port)
      (let [state (lifecycle/start-app! {:target     target
                                         :http-port  http-port
                                         :debug-port debug-port})]
        (println "[fixture] CDP connected, waiting for app ready...")
        (lifecycle/wait-for-app-ready! (:session state) 60000)
        (println "[fixture] App ready")
        (reset! shared-state state))))
  (f))

(defn session
  "Returns the current shared CDP session."
  []
  (:session @shared-state))

(defn state
  "Returns the full shared state map."
  []
  @shared-state)
