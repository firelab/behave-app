(ns behave.handlers
  (:require [behave-routing.main               :refer [routes]]
            [behave.download-vms               :refer [export-from-vms export-images-from-vms]]
            [behave.views                      :refer [render-page render-tests-page]]
            [bidi.bidi                         :refer [match-route]]
            [clojure.core.async                :refer [<! alts! chan go-loop put! timeout]]
            [clojure.edn                       :as edn]
            [clojure.string                    :as str]
            [config.interface                  :refer [get-config]]
            [logging.interface                 :as l :refer [log-str]]
            [ring.middleware.content-type      :refer [wrap-content-type]]
            [ring.middleware.multipart-params  :refer [wrap-multipart-params]]
            [ring.middleware.keyword-params    :refer [wrap-keyword-params]]
            [ring.middleware.reload            :refer [wrap-reload]]
            [ring.middleware.resource          :refer [wrap-resource]]
            [ring.util.codec                   :refer [url-decode]]
            [ring.util.response                :refer [not-found]]
            [transport.interface               :refer [->clj mime->type]]))

;;; Constants

(def ^:private KILL-TIMEOUT-MS 5000) ;; 5 seconds

;;; Close window indirection (avoids transitive JCEF dependency)

(defonce ^:private *close-window-fn (atom nil))

(defn register-close-fn!
  "Registers a function to handle window close requests. Called by desktop entry point."
  [f]
  (reset! *close-window-fn f))

;;; State

(def ^:private kill-channel (atom nil))
(def ^:private cancel-channel (atom nil))
(def ^:private close-time (atom 0))

;;; Helpers

(defn- now-in-ms
  "Returns the current time since Jan. 1, 1970 in milliseconds."
  []
  (inst-ms (java.util.Date.)))

(defn- kill-app!
  "Use Runtime exit to kill entire JVM Process"
  []
  (.exit (Runtime/getRuntime) 0))

(defn watch-kill-signal!
  "Creates a channel to listen on for a 'kill' signal. Once a message is put on the kill channel, waits 10 seconds to cancel the kill."
  []
  (let [kill-chan   (chan)
        cancel-chan (chan)]
    (go-loop []
      (<! kill-chan)
      (let [[_ ch] (alts! [cancel-chan (timeout KILL-TIMEOUT-MS)])]
        (if (not= ch cancel-chan)
          (kill-app!)
          (recur))))
    (reset! kill-channel kill-chan)
    (reset! cancel-channel cancel-chan)))

(defn vms-sync!
  "Sync to the VMS"
  []
  (let [{:keys [secret-token url]} (get-config :vms)]
    (pmap #(% secret-token url) [export-from-vms export-images-from-vms])))

(defn- vms-sync-handler [req]
  (log-str "Request Received:" (select-keys req [:uri :request-method :params]))
  (vms-sync!)
  {:status 200 :body "OK"})

;;; Handlers

(defn- close-handler
  [{:keys [params window-id]}]
  (log-str "Got /close request:" params "window-id:" window-id)
  (if (= (get-config :server :mode) "prod")
    (let [{:keys [cancel]} params]
      (cond
        (nil? cancel)
        (if window-id
          (do (when-let [f @*close-window-fn] (f window-id))
              {:status 200 :body "OK"})
          (do (reset! close-time (now-in-ms))
              (put! @kill-channel true)
              {:status 200 :body "OK"}))

        :else
        (do (put! @cancel-channel true)
            {:status 200 :body "OK"})))
    {:status 404 :body "Not Found"}))

(defn- bad-uri?
  [uri]
  (str/includes? (str/lower-case uri) "php"))

(defn- routing-handler [{:keys [uri] :as request}]
  (let [next-handler (cond
                       (bad-uri? uri)                         (not-found "404 Not Found")
                       (str/starts-with? uri "/api/vms-sync") #'vms-sync-handler
                       (str/starts-with? uri "/api/test")     #'render-tests-page
                       (str/starts-with? uri "/api/close")    #'close-handler
                       (match-route routes uri)              (render-page (match-route routes uri))
                       :else                                 (not-found "404 Not Found"))]
    (next-handler request)))

(defn- wrap-query-params [handler]
  (fn [{:keys [params query-string] :or {params {}} :as req}]
    (if (empty? query-string)
      (handler req)
      (let [keyvals (-> (url-decode query-string)
                        (str/split #"&"))
            params  (reduce (fn [params keyval]
                              (let [[k v] (str/split keyval #"=")]
                                (assoc params (keyword k) (edn/read-string v))))
                            params keyvals)]
        (handler (assoc req :params params))))))

(defn- wrap-params [handler]
  (fn [{:keys [content-type body query-string] :as req}]
    (if-let [req-type (mime->type content-type)]
      (let [query-params (->clj query-string req-type)
            body-params  (when body (->clj (slurp body) req-type))]
        (handler (update req :params merge query-params body-params)))
      (handler req))))

(defn- wrap-req-content-type+accept [handler]
  (fn [{:keys [headers] :as req}]
    (handler (assoc req
                    :content-type (get headers "content-type")
                    :accept       (get headers "accept")))))

(defn- wrap-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [{:keys [via cause trace]} (Throwable->map e)]
          (log-str "Error: " cause "via" via)
          (log-str request)
          (log-str trace)
          {:status 503 :body cause})))))

(defn- reloadable-clj-files
  "Creates a list of files that can be fed to the ring-wrap-reload interceptor."
  []
  (if (get-config :client :jar-local?)
    ["src"]
    (let [m        (meta #'reloadable-clj-files)
          n-spaces (:ns m)
          ns-file  (-> n-spaces
                       (str/replace "-" "_")
                       (str/replace "." "/")
                       (->> (format "/%s.clj")))
          path     (:file m)]
      [(str/replace path #"/projects/.*" "/components")
       (str/replace path #"/projects/.*" "/bases")
       (str/replace path ns-file "")])))

(defn- optional-middleware [handler mw use?]
  (if use?
    (mw handler)
    handler))

(defn- wrap-figwheel [handler figwheel?]
  (fn [request]
    (handler (assoc request :figwheel? figwheel?))))

(defn server-handler-stack
  "Server handler stack."
  [{:keys [reload? figwheel?]}]
  (-> routing-handler
      (wrap-figwheel figwheel?)
      wrap-params
      wrap-keyword-params
      wrap-query-params
      wrap-req-content-type+accept
      (wrap-resource "public" {:allow-symlinks? true})
      (wrap-content-type {:mime-types {"wasm" "application/wasm"}})
      wrap-multipart-params
      wrap-exceptions
      (optional-middleware #(wrap-reload % {:dirs (reloadable-clj-files)}) reload?)))

(defn- wrap-window-id
  "Injects a closed-over `window-id` into each request map."
  [handler window-id]
  (fn [req]
    (handler (assoc req :window-id window-id))))

(defn create-cef-handler-stack
  "Custom handler stack for Chrome Embedded Framework.
   Optionally takes a `window-id` to associate requests with a window."
  ([]
   (create-cef-handler-stack nil))
  ([window-id]
   (cond-> (-> routing-handler
               wrap-params
               wrap-keyword-params
               wrap-query-params
               wrap-req-content-type+accept
               wrap-exceptions)
     window-id (wrap-window-id window-id))))

;; This is for Figwheel
(def ^{:doc "Figwheel handler."}
  development-app
  (server-handler-stack {:figwheel? true :reload? true}))
