(ns jcef.resource-handlers
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [jcef.mime-type :refer [ext-mime-type]])
  (:import [org.cef.handler
            CefCookieAccessFilter
            CefRequestHandlerAdapter
            CefResourceHandlerAdapter
            CefResourceRequestHandlerAdapter]
           [org.cef.misc IntRef StringRef BoolRef]
           [org.cef.network CefRequest CefResponse CefPostDataElement$Type]
           [java.io InputStream IOException ByteArrayOutputStream]
           [java.net URL]))

(defn- strip-leading-slash [s]
  (if (str/starts-with? s "/")
    (subs s 1)
    s))

(defn post-data->map [cef-request]
  (let [post-data          (first (.. cef-request (getPostData) (getElements)))
        data-type          (.getType post-data)
        byte-output-stream (ByteArrayOutputStream.)
        byte-count         (.getByteCount post-data)]
    (condp = data-type
      CefPostDataElement$Type/PDE_TYPE_EMPTY
      {:body byte-output-stream}

      CefPostDataElement$Type/PDE_TYPE_BYTES
      {:body           (.getBytes post-data (.getBytesCount post-data) byte-output-stream)
       :content-length byte-count}
      
      CefPostDataElement$Type/PDE_TYPE_FILE
      {:filename           (.getFile data-type)
       :body               (.getBytes post-data (.getBytesCount post-data) byte-output-stream)
       :content-length     byte-count
       :character-encoding "UTF-8"})))

(defn cef-request->ring-request [^CefRequest cef-request]
  (let [url           (URL. (.getURL cef-request))
        host          (.getHost url) 
        port          (.getPort url)
        query-string  (.getQuery url)
        uri           (.getPath url)
        method        (keyword (.toLowerCase (.getMethod cef-request)))
        headers       (java.util.HashMap.)
        _             (.getHeaderMap cef-request headers)
        headers       (update-keys (into {} headers) str/lower-case)
        _             (println [:CEF-REQUEST-:HEADERS headers])
        post-data-map (when-not (#{:get :head :options} method)
                        (post-data->map cef-request))]
    (merge 
     {:uri                uri
      :server-port        port
      :server-name        host
      :scheme             :http
      :request-method     method
      :query-string       query-string
      :headers            headers
      :ssl-client-cert    nil
      :params             {}
      :multipart-params   {}}
     post-data-map)))

(defn ring-response->cef-response [ring-response]
  (let [cef-response (CefResponse/create)]
    (doto cef-response
      (.setStatus (or (:status ring-response) 200))
      (.setMimeType (get-in ring-response [:headers "Content-Type"] "text/plain"))
      (.setHeaderMap (get ring-response :headers {})))
    cef-response))

(defn- ->resource [public-dir path]
  (let [path (if (#{"/" ""} path) "index.html" path)]
    (.getResource (ClassLoader/getSystemClassLoader)
                  (format "%s/%s" public-dir (strip-leading-slash path)))))

(defn- mime-type [filename-or-resource]
  (ext-mime-type (str filename-or-resource)))

(defn- create-api-resource-handler [ring-response]
  (let [resource-input-stream (:body ring-response)]
    (proxy [CefResourceHandlerAdapter] []
      (processRequest [& args]
        (let [callback (last args)]
          (.Continue callback)
          true))

      (getResponseHeaders [response & args]
        (.setMimeType response (get-in ring-response [:headers "Content-Type"] "text/plain"))
        (.setStatus response (:status ring-response))
        (doseq [[header-key value] (:headers response)]
          (.setHeaderByName response header-key value true))
        nil)

      (readResponse [data-out bytes-to-read bytes-read callback & args]
        (try
          (let [read-size (.read resource-input-stream data-out 0 bytes-to-read)]
            (.set bytes-read read-size)
            (if (not= read-size -1)
              true
              (do
                (.set bytes-read 0)
                (.close resource-input-stream)
                false)))
          (catch IOException _
            (.close resource-input-stream)
            (.cancel callback)
            false)))

      (cancel [& args]
        (.close resource-input-stream)))))

(defn- create-resource-handler [resource headers]
  (let [resource-input-stream (io/input-stream resource)
        resource-mime-type    (mime-type resource)]

    (proxy [CefResourceHandlerAdapter] []
      (processRequest [& args]
        (let [callback (last args)]
          (.Continue callback)
          true))

      (getResponseHeaders [response & args]
        (.setMimeType response resource-mime-type)
        (.setStatus response 200)
        (doseq [[header-key value] headers]
          (.setHeaderByName response header-key value true))
        nil)

      (readResponse [data-out bytes-to-read bytes-read callback & args]
        (try
          (let [read-size (.read resource-input-stream data-out 0 bytes-to-read)]
            (.set bytes-read read-size)
            (if (not= read-size -1)
              true
              (do
                (.set bytes-read 0)
                (.close resource-input-stream)
                false)))
          (catch IOException _
            (.close resource-input-stream)
            (.cancel callback)
            false)))

      (cancel [& args]
        (.close resource-input-stream)))))

(defn- cookie-filter []
  (proxy [CefCookieAccessFilter] []
    (canSaveCookie​ [_this _browser _frame _request _response _cookie]	
      true)
    (canSendCookie​ [_this _browser _frame _request _cookie]	
      true)))

(defn- reject-handler []
  (proxy [CefResourceHandlerAdapter] []
    (processRequest [this request callback]
      (.cancel callback)
      false)))

(defn- get-api-handler [ring-handler _browser _frame request & _args]
  (let [url            (URL. (.getURL request))
        _req-protocol  (.getProtocol url)
        _req-authority (.getAuthority url)
        req-path       (.getPath url)
        response       (ring-handler (cef-request->ring-request request))]
    (println [:GET-API-HANDLER _req-protocol _req-authority req-path response])
    (create-api-resource-handler response)))

(let [])

(defn- get-resource-handler [public-dir _browser _frame request & _args]
  (let [url            (URL. (.getURL request))
        _req-protocol  (.getProtocol url)
        _req-authority (.getAuthority url)
        req-path      (.getPath url)
        resource      (->resource public-dir req-path)]
    #_(println [:GET-RESOURCE-HANDLER public-dir _req-protocol _req-authority req-path resource])
    (if resource
      (create-resource-handler resource {})
      (reject-handler))))

(defn- custom-resource-handler [public-dir _protocol _authority ring-handler _browser _frame request & args]
  (when (str/starts-with? (.getURL request) "http")
    (proxy [CefResourceRequestHandlerAdapter] []
      (onBeforeResourceLoad [& args]
        false)

      (getCookieAccessFilter [& args]
        (cookie-filter))

      (getResourceHandler [& args]
        (let [[_ _ request] args
              
              url           (URL. (.getURL request))
              req-path      (.getPath url)]
          #_(println [:GET-RESOURCE-HANDLER url req-path])
          (if (str/starts-with? req-path "/api")
            (apply get-api-handler ring-handler args)
            (apply get-resource-handler public-dir args))))

      (onResourceResponse [& args]
        #_(println [:ON-RESOURCE-RESPONSE args])
        false))))

(defn custom-request-handler
  "Generate a local request handler that serves resources prepended with `public-dir`."
  [{:keys [protocol authority resource-dir ring-handler]}]
  (proxy [CefRequestHandlerAdapter] []
    (onBeforeBrowse​ [& args]
      #_(println [:BEFORE-BROWSE args])
      false)

    (getResourceRequestHandler [& args]
       (apply custom-resource-handler resource-dir protocol authority ring-handler args))))
