(ns jcef.resource-handlers
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [org.cef.handler
            CefCookieAccessFilter
            CefRequestHandlerAdapter
            CefResourceHandlerAdapter
            CefResourceRequestHandlerAdapter]
           [org.cef.misc IntRef StringRef BoolRef]
           [org.cef.network CefRequest CefResponse CefPostDataElement$Type]
           [java.io InputStream IOException ByteArrayOutputStream]
           [java.net URL]
           [java.nio.file Paths Files]))

(defn- strip-leading-slash [s]
  (if (str/starts-with? s "/")
    (subs s 1)
    s))

(defn post-data->stream [cef-request]
  (let [post-data          (first (.. cef-request (getPostData) (getElements))) 
        data-type          (.getType post-data)
        byte-output-stream (ByteArrayOutputStream.)
        byte-count         (.getByteCount post-data)]
    (condp = data-type
      CefPostDataElement$Type/PDE_TYPE_EMPTY
      {:body byte-output-stream}

      CefPostDataElement$Type/PDE_TYPE_BYTES
      {:body           (.getBytes post-data (.getBytesCount post-data) byte-output-stream)
       :content-length (.getByteCount post-data)}
      
      CefPostDataElement$Type/PDE_TYPE_FILE
      {:filename       (.getFile data-type)
       :body           (.getBytes post-data (.getBytesCount post-data) byte-output-stream)
       :content-length (.getByteCount post-data)})))

(defn cef-request->ring-request [^CefRequest cef-request]
  (let [url            (.getURL cef-request)
        port           (.getPort url)
        content-length (.getPort url)
        method         (keyword (.toLowerCase (.getMethod cef-request)))
        headers        (.getHeaderMap cef-request {})]
    {:uri            url
     :request-method method
     :headers        (into {} headers)
     :query-params   
     :body           (when-not (#{:get :head :options} method)
                       )
     {:ssl-client-cert nil
      :protocol HTTP/1.1
      :params {}
      :server-port 4242
      :content-length nil
      :content-type nil
      :character-encoding nil
      :uri /
      :server-name localhost
      :query-string nil
      :body #object[org.eclipse.jetty.server.HttpInputOverHTTP 0xdcde6f HttpInputOverHTTP@dcde6f[c=0
                                                                                                 q=0
                                                                                                 [0]=null
                                                                                                 s=STREAM]]
      :multipart-params {}
      :scheme :http
      :request-method :get
      :accept */*}


     }))

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

(defn- ext [filename-or-resource]
  (-> (str/split (str filename-or-resource) #"/")
      (last)
      (str/split #"\.")
      (last)))

(defn- mime-type [filename-or-resource]
  (let [file-ext (ext filename-or-resource)
        resource (if (string? filename-or-resource)
                   (io/resource filename-or-resource)
                   filename-or-resource)]
    (condp = file-ext
      "msgpack"
      "application/msgpack"

      (-> resource
          (.toURI)
          (Paths/get)
          (Files/probeContentType)))))

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

(defn- get-resource-handler [public-dir _browser _frame request & _args]
  (let [url           (URL. (.getURL request))
        _req-protocol  (.getProtocol url)
        _req-authority (.getAuthority url)
        req-path      (.getPath url)
        resource      (->resource public-dir req-path)]
    #_(println [:GET-RESOURCE-HANDLER public-dir req-protocol req-authority req-path resource])
    (if resource
      (create-resource-handler resource {})
      (reject-handler))))

(defn- get-api-handler [handlers _browser _frame request & args]
  (let [url           (URL. (.getURL request))
        _req-protocol  (.getProtocol url)
        _req-authority (.getAuthority url)
        req-path      (.getPath url)
        handler       (get handlers req-path)]
    #_(println [:GET-RESOURCE-HANDLER public-dir req-protocol req-authority req-path resource])
    (if handler
      handler
      #_(apply create-handler-response handler (cef-request->ring-request request))
      (reject-handler))))

(defn- custom-resource-handler [public-dir _protocol _authority _browser _frame request & args]
  #_(println [:LOCAL-RESOURCE-HANDLER public-dir protocol authority args])

  (when (str/starts-with? (.getURL request) "http")
    (proxy [CefResourceRequestHandlerAdapter] []
      (onBeforeResourceLoad [& args]
        false)

      (getCookieAccessFilter [& args]
        (cookie-filter))

      (getResourceHandler [& args]
        (apply get-resource-handler public-dir args))

      (onResourceResponse [& args]
        #_(println [:ON-RESOURCE-RESPONSE args])
        false))))

(defn create-local-request-handler
  "Generate a local request handler that serves resources prepended with `public-dir`."
  [public-dir protocol authority]
  (proxy [CefRequestHandlerAdapter] []
    (onBeforeBrowse​ [& args]
      #_(println [:BEFORE-BROWSE args])
      false)

    (getResourceRequestHandler [& args]
       (apply custom-resource-handler public-dir protocol authority args))))

(defn custom-jcef-request-handler
  "Generate a local request handler that serves resources prepended with `public-dir`."
  [{:keys [protocol authority resource-dir handlers]}]
  (proxy [CefRequestHandlerAdapter] []
    (onBeforeBrowse​ [& args]
      #_(println [:BEFORE-BROWSE args])
      false)

    (getResourceRequestHandler [& args]
       (apply custom-resource-handler resource-dir protocol authority handlers args))))
