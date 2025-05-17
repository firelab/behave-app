(ns jcef.resource-handlers
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [org.cef.handler
            CefCookieAccessFilter
            CefRequestHandlerAdapter
            CefResourceHandlerAdapter
            CefResourceRequestHandlerAdapter]
           [org.cef.misc IntRef StringRef BoolRef]
           [org.cef.network CefRequest CefResponse]
           [java.io InputStream IOException]
           [java.net URL]
           [java.nio.file Paths Files]))

(defn- strip-leading-slash [s]
  (if (str/starts-with? s "/")
    (subs s 1)
    s))

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

(defn- create-local-resource-handler [public-dir _protocol _authority _browser _frame request & args]
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
       (apply create-local-resource-handler public-dir protocol authority args))))
