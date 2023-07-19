(ns behave-cms.handler
  (:require [clojure.edn        :as edn]
            [clojure.data.json  :as json]
            [clojure.string     :as str]
            [clojure.stacktrace :as st]
            [bidi.bidi          :refer [match-route]]
            [cognitect.transit  :as transit]
            [ring.middleware.absolute-redirects :refer [wrap-absolute-redirects]]
            [ring.middleware.content-type       :refer [wrap-content-type]]
            [ring.middleware.default-charset    :refer [wrap-default-charset]]
            [ring.middleware.gzip               :refer [wrap-gzip]]
            [ring.middleware.keyword-params     :refer [wrap-keyword-params]]
            [ring.middleware.nested-params      :refer [wrap-nested-params]]
            [ring.middleware.not-modified       :refer [wrap-not-modified]]
            [ring.middleware.multipart-params   :refer [wrap-multipart-params]]
            [ring.middleware.params             :refer [wrap-params]]
            [ring.middleware.resource           :refer [wrap-resource]]
            [ring.middleware.reload             :refer [wrap-reload]]
            [ring.middleware.session            :refer [wrap-session]]
            [ring.middleware.session.cookie     :refer [cookie-store]]
            [ring.middleware.ssl                :refer [wrap-ssl-redirect]]
            [ring.middleware.x-headers          :refer [wrap-frame-options wrap-content-type-options wrap-xss-protection]]
            [ring.util.codec                    :refer [url-decode url-encode]]
            [ring.util.response                 :refer [redirect]]
            [config.interface                   :refer [get-config]]
            [triangulum.logging                 :refer [log-str]]
            [behave-cms.file-io                 :refer [file-handler]]
            [behave-cms.remote-api              :refer [api-handler clj-handler]]
            [behave-cms.routes                  :refer [app-routes api-routes public-routes]]
            [behave-cms.sync                    :refer [sync-handler]]
            [behave-cms.views                   :refer [data-response render-page]]))

(defn- string-to-bytes [s] (.getBytes s))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Routing Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn bad-uri? [uri] (str/includes? (str/lower-case uri) "php"))

(defn token-resp [{:keys [auth-token]} handler]
  (if (= auth-token (get-config :secret-token))
    handler
    (constantly (data-response "Forbidden" {:status 403}))))

(defn public-route? [uri]
  (when-let [route (or (match-route api-routes uri) (match-route app-routes uri))]
    (some? (public-routes (:handler route)))))

(defn authenticated-page [uri {:keys [user-uuid]} handler]
  (if (or (public-route? uri) user-uuid)
    handler
    (constantly (redirect (format "/login?redirect=%s" (url-encode uri))))))

(defn authenticated-api [uri {:keys [user-uuid]} handler]
  (if (or (public-route? uri) user-uuid)
    handler
    (constantly (data-response "Forbidden" {:status 403}))))

(defn routing-handler [{:keys [uri session params] :as request}]
  (let [next-handler (cond
                       (bad-uri? uri)                  (constantly (data-response "Forbidden" {:status 403}))
                       (str/starts-with? uri "/sync")  (token-resp params sync-handler)
                       (match-route api-routes uri)    (authenticated-api uri session api-handler)
                       (str/starts-with? uri "/clj/")  (token-resp params clj-handler)
                       (str/starts-with? uri "/file/") #'file-handler
                       :else                           (authenticated-page uri session (render-page (match-route app-routes uri))))]
    (next-handler request)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Custom Middlewares
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-req-content-type+accept [handler]
  (fn [{:keys [headers] :as req}]
    (handler (assoc req
                    :content-type (get headers "content-type")
                    :accept       (get headers "accept")))))

(defn wrap-request-logging [handler]
  (fn [request]
    (let [{:keys [uri request-method params]} request
          param-str (pr-str (dissoc params :auth-token :password :re-password))]
      (log-str "Request(" (name request-method) "): \"" uri "\" " param-str)
      (handler request))))

(defn wrap-response-logging [handler]
  (fn [request]
    (let [{:keys [status headers body] :as response} (handler request)
          content-type (headers "Content-Type")]
      (log-str "Response(" status "): "
               (cond
                 (str/includes? content-type "text/html")
                 "<html>...</html>"

                 (= content-type "application/edn")
                 (binding [*print-length* 2] (print-str (edn/read-string body)))

                 (= content-type "application/json")
                 (binding [*print-length* 2] (print-str (json/read-str body)))

                 :else
                 body))
      response)))

(defn parse-query-string [query-string]
  (let [keyvals (-> (url-decode query-string)
                    (str/split #"&"))]
    (reduce (fn [params keyval]
              (->> (str/split keyval #"=")
                   (map edn/read-string)
                   (apply assoc params)))
            {}
            keyvals)))

(defn wrap-edn-params [handler]
  (fn [{:keys [content-type request-method query-string body params] :as request}]
    (if (= content-type "application/edn")
      (let [get-params (when (and (= request-method :get)
                                  (not (str/blank? query-string)))
                         (parse-query-string query-string))
            post-params (when (and (= request-method :post)
                                   (not (nil? body)))
                          (edn/read-string (slurp body)))]
        (handler (assoc request :params (merge params get-params post-params))))
      (handler request))))

(defn wrap-transit-params [handler]
  (fn [{:keys [content-type request-method query-string body params] :as request}]
    (if (= content-type "application/transit+json")
      (let [get-params (when (and (= request-method :get)
                                  (not (str/blank? query-string)))
                         (parse-query-string query-string))
            post-params (when (and (= request-method :post)
                                   (not (nil? body)))
                          (transit/read (transit/reader body :json)))]
        (handler (assoc request :params (merge params get-params post-params))))
      (handler request))))

(defn wrap-session-params [handler]
  (fn [{:keys [session] :as request}]
    (handler (update request :params merge session))))

(defn wrap-exceptions [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (let [{:keys [data cause]} (Throwable->map e)
              status (:status data)]
          (log-str "Error: " cause)
          (log-str (st/print-stack-trace e))
          (data-response cause {:status (or status 500)}))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler Stacks
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn optional-middleware [handler mw use?]
  (if use?
    (mw handler)
    handler))

(defn create-handler-stack [ssl? reload?]
  (-> routing-handler
      (optional-middleware wrap-ssl-redirect ssl?)
      wrap-request-logging
      wrap-keyword-params
      wrap-edn-params
      wrap-transit-params
      wrap-nested-params
      wrap-multipart-params
      wrap-session-params
      wrap-req-content-type+accept
      wrap-params
      wrap-session
      wrap-absolute-redirects
      (wrap-resource (if reload? "projects/behave_cms/resources/public" "public"))
      wrap-content-type
      (wrap-default-charset "utf-8")
      wrap-not-modified
      (wrap-xss-protection true {:mode :block})
      (wrap-frame-options :sameorigin)
      (wrap-content-type-options :nosniff)
      wrap-response-logging
      wrap-gzip
      wrap-exceptions
      (optional-middleware wrap-reload reload?)))

;; This is for Figwheel
(def development-app
  (create-handler-stack false true))
