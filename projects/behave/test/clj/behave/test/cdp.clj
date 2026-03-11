(ns behave.test.cdp
  "CDP (Chrome DevTools Protocol) WebSocket client for E2E testing.
   Connects to JCEF or Chrome's remote debug port and sends CDP commands."
  (:require [clojure.data.json :as json])
  (:import (java.net URI)
           (java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers WebSocket WebSocket$Listener)
           (java.util.concurrent CompletableFuture ConcurrentHashMap TimeUnit)
           (java.util.concurrent.atomic AtomicLong)))

(defn ^:private fetch-ws-url
  "Fetches the WebSocket debugger URL from the CDP /json endpoint."
  [port]
  (let [client  (HttpClient/newHttpClient)
        request (-> (HttpRequest/newBuilder (URI. (str "http://localhost:" port "/json")))
                    (.GET)
                    (.build))
        body    (.body (.send client request (HttpResponse$BodyHandlers/ofString)))]
    (second (re-find #"\"webSocketDebuggerUrl\"\s*:\s*\"([^\"]+)\"" body))))

(defn connect!
  "Connects to a CDP endpoint on the given port. Returns a session atom containing
   `:ws`, `:id-counter`, and `:pending` (a map of id -> CompletableFuture)."
  [port]
  (let [ws-url     (fetch-ws-url port)
        _          (when-not ws-url
                     (throw (ex-info "No webSocketDebuggerUrl found" {:port port})))
        id-counter (AtomicLong. 0)
        pending    (ConcurrentHashMap.)
        events     (atom [])
        session    (atom {:id-counter id-counter
                          :pending    pending
                          :events     events})
        listener   (reify WebSocket$Listener
                     (onText [_ ws data _last?]
                       (let [msg (json/read-str (str data) :key-fn keyword)]
                         (if-let [id (:id msg)]
                           (when-let [^CompletableFuture fut (.remove pending id)]
                             (.complete fut msg))
                           (swap! events conj msg)))
                       (.request ws 1))
                     (onOpen [_ ws]
                       (.request ws 1))
                     (onError [_ _ws error]
                       (doseq [^CompletableFuture fut (vals pending)]
                         (.completeExceptionally fut error))
                       (.clear pending)))
        ws         (-> (HttpClient/newHttpClient)
                       (.newWebSocketBuilder)
                       (.buildAsync (URI. ws-url) listener)
                       (.join))]
    (swap! session assoc :ws ws)
    session))

(defn disconnect!
  "Closes the CDP WebSocket connection."
  [session]
  (when-let [^WebSocket ws (:ws @session)]
    (.sendClose ws WebSocket/NORMAL_CLOSURE "")))

(defn send!
  "Sends a CDP command and returns a CompletableFuture of the response map."
  [session method params]
  (let [{:keys [^WebSocket ws ^AtomicLong id-counter ^ConcurrentHashMap pending]} @session
        id  (.incrementAndGet id-counter)
        fut (CompletableFuture.)
        msg (json/write-str {:id id :method method :params (or params {})})]
    (.put pending id fut)
    (.sendText ws msg true)
    fut))

(defn send-sync!
  "Sends a CDP command and blocks until the response arrives (default 10s timeout)."
  ([session method params]
   (send-sync! session method params 10000))
  ([session method params timeout-ms]
   (.get ^CompletableFuture (send! session method params) timeout-ms TimeUnit/MILLISECONDS)))

(defn evaluate!
  "Evaluates a JavaScript expression via CDP Runtime.evaluate.
   Returns the JS result value (parsed from JSON)."
  ([session js-expr]
   (evaluate! session js-expr 10000))
  ([session js-expr timeout-ms]
   (let [response (send-sync! session "Runtime.evaluate"
                              {:expression     js-expr
                               :returnByValue  true
                               :awaitPromise   true}
                              timeout-ms)
         result   (get-in response [:result :result])]
     (when-let [exception (get-in response [:result :exceptionDetails])]
       (throw (ex-info "JS evaluation error"
                       {:expression js-expr :exception exception})))
     (:value result))))

(defn drain-events!
  "Returns and clears all collected CDP events since last drain."
  [session]
  (let [evts @(:events @session)]
    (reset! (:events @session) [])
    evts))
