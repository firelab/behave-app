(ns kaocha-hooks
  (:require [shadow.cljs.devtools.api :as shadow-api]
            [shadow.cljs.devtools.server :as shadow-server]
            [shadow.cljs.devtools.server.runtime :as shadow-runtime]
            [kaocha.cljs2.funnel-client :as funnel])
  (:import (java.lang ProcessBuilder$Redirect)
           (java.net ServerSocket URI)
           (java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers WebSocket WebSocket$Listener)
           (java.util.concurrent CompletableFuture)))

(defn spawn
  "Start a process. Pass `:silent true` to discard stdout/stderr, otherwise
  they are inherited from the parent. Returns the Process object."
  [args opts]
  (let [builder (ProcessBuilder. args)
        redirect (if (:silent opts)
                   (ProcessBuilder$Redirect/DISCARD)
                   (ProcessBuilder$Redirect/INHERIT))]
    (.redirectOutput builder redirect)
    (.redirectError builder redirect)
    (when-let [env (:env opts)]
      (let [environment (.environment builder)]
        (doseq [[k v] env]
          (.put environment k v))))
    (.start builder)))

(defn ^:private port-bound? [port]
  (try
    (with-open [_ (ServerSocket. port)]
      false)
    (catch java.net.BindException _
      true)))

(defn ^:private wait-for-port!
  "Poll until `port` is bound, sleeping `interval-ms` between attempts."
  [port interval-ms timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (when-not (port-bound? port)
        (when (> (System/currentTimeMillis) deadline)
          (throw (ex-info "Timed out waiting for port" {:port port})))
        (Thread/sleep interval-ms)
        (recur)))))

(defn ensure-funnel! []
  (when-not (port-bound? 44220)
    (spawn ["clojure" "-M:funnel" "-vv"] {:silent true})
    (wait-for-port! 44220 250 15000)))

(defn ensure-shadow-instance! []
  (when (nil? @shadow-runtime/instance-ref)
    (shadow-server/start!)
    (loop []
      (Thread/sleep 250)
      (when (nil? @shadow-runtime/instance-ref)
        (recur)))))

(defn shadow-dev-build! [testable]
  (shadow-api/watch (:shadow/build testable)))

(defn pre-load [testable config]
  (println "Pre-Loading Funnel, Shadow-CLJS...")
  (ensure-funnel!)
  (ensure-shadow-instance!)
  (shadow-dev-build! testable)
  ;; Always return the first argument from Kaocha hooks
  testable)

(def ^:private chrome-path
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")

(defn ^:private chrome-reload!
  "Send Page.reload via CDP websocket to Chrome on the given debug port."
  [port]
  (let [client   (HttpClient/newHttpClient)
        request  (-> (HttpRequest/newBuilder (URI. (str "http://localhost:" port "/json")))
                     (.GET)
                     (.build))
        body     (.body (.send client request (HttpResponse$BodyHandlers/ofString)))
        ws-url   (second (re-find #"\"webSocketDebuggerUrl\"\s*:\s*\"([^\"]+)\"" body))
        done     (CompletableFuture.)
        listener (reify WebSocket$Listener
                   (onText [_ _ws _data _last]
                     (.complete done nil)))]
    (when ws-url
      (let [ws (-> (.newWebSocketBuilder client)
                   (.buildAsync (URI. ws-url) listener)
                   (.join))]
        (.sendText ws "{\"id\":1,\"method\":\"Page.reload\",\"params\":{\"ignoreCache\":true}}" true)
        (.get done)
        (.sendClose ws WebSocket/NORMAL_CLOSURE "")))))

(defn ^:private launch-chrome!
  "Launch Chrome with remote debugging on port 9222 if not already running."
  [url]
  (spawn [chrome-path
          "--remote-debugging-port=9222"
          "--user-data-dir=/tmp/chrome-debug-profile"
          "--no-first-run"
          "--no-default-browser-check"
          ;; "--headless"
          url]
         {:silent true}))

(defn launch-browser-and-wait [{:funnel/keys [conn]
                                :kaocha.cljs2/keys [timeout]}]
  ;; Both these calls ask Funnel if it has any clients that look like they are
  ;; the ones we would want to talk to, in particular it sends this query to Funnel:
  ;;
  ;; {:lambdaisland.chui.remote? true
  ;;  :working-directory (.getAbsolutePath (io/file ""))}
  ;;
  ;; Remember that Funnel is fully symmetrical, Kaocha-cljs2 (JVM) is just
  ;; another client, as are Chui-remote (JS) clients. We only want chui-remote
  ;; clients, and in particular we want ones which CLJS build was triggered in
  ;; the same project directory that we are in, so we don't accidentally connect
  ;; to another project's browser tab.
  (if (seq (funnel/list-clients conn))
    (chrome-reload! 9222)
    (launch-chrome! "http://localhost:8022"))
  (funnel/wait-for-clients conn (if timeout {:timeout timeout})))
