(ns behave.test.lifecycle
  "App lifecycle management for E2E tests.
   Supports two targets: :jcef (full desktop) and :browser (Chrome)."
  (:require [behave.test.cdp :as cdp])
  (:import (java.lang ProcessBuilder$Redirect)
           (java.net URI)
           (java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers)
           (java.time Duration)))

;;; Helpers

(defn ^:private cdp-endpoint-ready?
  "Probes the CDP /json endpoint. Returns true if it responds."
  [port]
  (try
    (let [client  (-> (HttpClient/newBuilder)
                      (.connectTimeout (Duration/ofSeconds 2))
                      (.build))
          request (-> (HttpRequest/newBuilder (URI. (str "http://localhost:" port "/json")))
                      (.timeout (Duration/ofSeconds 2))
                      (.GET)
                      (.build))
          resp    (.send client request (HttpResponse$BodyHandlers/ofString))]
      (= 200 (.statusCode resp)))
    (catch Exception _
      false)))

(defn ^:private wait-for-cdp!
  "Polls until the CDP /json endpoint responds on `port`."
  [port interval-ms timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (when-not (cdp-endpoint-ready? port)
        (when (> (System/currentTimeMillis) deadline)
          (throw (ex-info "Timed out waiting for CDP endpoint" {:port port})))
        (Thread/sleep interval-ms)
        (recur)))))

(defn ^:private spawn
  "Starts a process with the given args."
  [args & [{:keys [silent]}]]
  (let [builder  (ProcessBuilder. ^java.util.List args)
        redirect (if silent
                   ProcessBuilder$Redirect/DISCARD
                   ProcessBuilder$Redirect/INHERIT)]
    (.redirectOutput builder redirect)
    (.redirectError builder redirect)
    (.start builder)))

;;; Chrome browser target

(def ^:private chrome-path
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")

(defn ^:private launch-chrome! [url debug-port]
  (spawn [chrome-path
          (str "--remote-debugging-port=" debug-port)
          "--user-data-dir=/tmp/behave-e2e-profile"
          "--no-first-run"
          "--no-default-browser-check"
          url]
         {:silent true}))

;;; JCEF target

(defn ^:private launch-jcef!
  "Launches the JCEF app with remote debugging enabled.
   Uses jcef.core directly to avoid System.exit on close."
  [http-port debug-port]
  (require 'behave.server 'behave.handlers 'jcef.interface 'config.interface)
  (let [init-config!             (resolve 'behave.server/init-config!)
        get-config               (resolve 'config.interface/get-config)
        init-cef-app!            (resolve 'jcef.interface/init-cef-app!)
        open-window!             (resolve 'jcef.interface/open-window!)
        create-cef-handler-stack (resolve 'behave.handlers/create-cef-handler-stack)
        custom-request-handler   (resolve 'jcef.interface/custom-request-handler)
        start-server!            (resolve 'server.interface/start-server!)
        server-handler-stack     (resolve 'behave.handlers/server-handler-stack)]
    (init-config!)
    (start-server! {:handler (server-handler-stack {:reload? false :figwheel? false})
                    :port    http-port})
    (let [cef-app (init-cef-app! {:remote-debug-port debug-port})
          request-handler (custom-request-handler
                           {:protocol     "http"
                            :authority    (format "localhost:%s" http-port)
                            :resource-dir "public"
                            :ring-handler (create-cef-handler-stack)})
          app-promise (promise)]
      (javax.swing.SwingUtilities/invokeLater
       #(let [app (open-window!
                   cef-app
                   {:title           (get-config :site :title)
                    :url             (str "http://localhost:" http-port)
                    :fullscreen?     false
                    :size            [1280 900]
                    :request-handler request-handler
                    :on-shown        (fn [app & _] (deliver app-promise app))
                    :on-close        (fn [])})]
          app))
      {:cef-app     cef-app
       :app-promise app-promise})))

;;; Public API

(defn start-app!
  "Starts the app for E2E testing. Options:
   - `:target`     - :jcef or :browser (default :browser)
   - `:http-port`  - Server port (default 4242)
   - `:debug-port` - CDP debug port (default 9222)
   - `:url`        - URL to open (for :browser target, default http://localhost:<http-port>)"
  [{:keys [target http-port debug-port url]
    :or   {target :browser http-port 4242 debug-port 9222}}]
  (let [url (or url (str "http://localhost:" http-port))]
    (case target
      :browser
      (let [already-running? (cdp-endpoint-ready? debug-port)
            process          (when-not already-running?
                               (launch-chrome! url debug-port))]
        (when-not already-running?
          (wait-for-cdp! debug-port 500 30000)
          (Thread/sleep 1000))
        (let [session (cdp/connect! debug-port)]
          {:target  :browser
           :process process
           :session session}))

      :jcef
      (let [{:keys [cef-app app-promise]} (launch-jcef! http-port debug-port)]
        (wait-for-cdp! debug-port 500 30000)
        (Thread/sleep 2000)
        (let [session (cdp/connect! debug-port)]
          {:target      :jcef
           :cef-app     cef-app
           :app-promise app-promise
           :session     session})))))

(defn wait-for-app-ready!
  "Polls until the app UI is loaded (skeleton replaced by real content).
   Default timeout is 30 seconds."
  ([session]
   (wait-for-app-ready! session 30000))
  ([session timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [ready (try
                     (cdp/evaluate! session
                                    "(function() {
                          var el = document.querySelector('.page__main');
                          return el !== null && !el.textContent.includes('Loading...');
                        })()"
                                    3000)
                     (catch Exception _ false))]
         (if ready
           true
           (do
             (when (> (System/currentTimeMillis) deadline)
               (throw (ex-info "Timed out waiting for app to be ready"
                               {:timeout-ms timeout-ms})))
             (Thread/sleep 500)
             (recur))))))))

(defn stop-app!
  "Tears down the app state created by `start-app!`."
  [state]
  (when-let [session (:session state)]
    (try (cdp/disconnect! session) (catch Exception _)))
  (case (:target state)
    :browser
    (when-let [^Process process (:process state)]
      (.destroy process))

    :jcef
    (when-let [app @(:app-promise state)]
      (javax.swing.SwingUtilities/invokeAndWait
       #(.dispose ^javax.swing.JFrame (:frame app))))

    nil))

(defn reload-page!
  "Reloads the current page via CDP. Useful for test isolation."
  [session]
  (cdp/send-sync! session "Page.reload" {:ignoreCache true}))
