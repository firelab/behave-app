(ns jcef.resource-handlers
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [me.raynes.fs    :as fs]
            [jcef.mime-type :refer [filename-ext ext-mime-type]])
  (:import [org.cef.handler
            CefCookieAccessFilter
            CefRequestHandlerAdapter
            CefResourceHandlerAdapter
            CefResourceRequestHandlerAdapter]
           [org.cef.network CefRequest CefPostDataElement$Type]
           [java.io BufferedInputStream IOException]
           [java.net URL]))

(defn- strip-leading-slash [s]
  (if (str/starts-with? s "/")
    (subs s 1)
    s))

(defn- index-by [coll k & [xform]]
  (reduce (fn [acc m]
            (let [k (if (fn? xform) (xform (get m k)) (get m k))]
              (assoc acc k m)))
          {}
          coll))

(defn- create-temp-file [filename]
  (java.io.File/createTempFile (fs/base-name filename true) (format ".%s" (filename-ext filename))))

(defn- process-bytes [e boundary]
  (let [byte-count    (.getBytesCount e)
        bytes-to-read (byte-array byte-count)
        _             (.getBytes e byte-count bytes-to-read)
        entries 
        (-> (String. bytes-to-read)
            (str/replace (re-pattern (str "--" boundary "(\r\n)?")) "")
            (str/replace #"--\r\n" "")
            (str/replace #"\r\n" ";")
            (str/replace #"\"" "")
            (str/split #"[:=;]\s?"))]
    (when-not (empty? entries)
      (update-keys (apply hash-map entries) (comp keyword str/lower-case)))))

(defn- copy-to-temp-file! [filename]
(let [tempfile (create-temp-file filename)]
    (io/copy (io/file filename) tempfile)
    tempfile))

(defn- post-data->map [headers cef-request]
  (let [content-type (get headers "content-type")]
    (when (str/starts-with? content-type "multipart/form-data")
      (let [boundary (second (re-find #"boundary=(.+)" content-type))
            elements (java.util.Vector.)
            _        (.. cef-request (getPostData) (getElements elements))
            results 
            (loop [remaining (rest elements)
                   element   (first elements)
                   result    []]
              (if (nil? element)
                result
                (recur (rest remaining)
                       (first remaining)
                       (condp = (.getType element)
                         CefPostDataElement$Type/PDE_TYPE_FILE
                         (let [filename    (.getFile element)
                               tempfile    (copy-to-temp-file! filename)]
                           (update result (dec (count result)) merge {:filename filename :tempfile tempfile}))

                         CefPostDataElement$Type/PDE_TYPE_BYTES
                         (if-let [data-map (process-bytes element boundary)]
                           (conj result data-map)
                           result)))))
            results (index-by results :name)]

        {:multipart-params results
         :params           (update-keys results keyword)}))))

(defn- cef-request->ring-request [^CefRequest cef-request]
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
                        (post-data->map headers cef-request))]
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

(defn- double-escape [^String x]
  (.replace (.replace x "\\" "\\\\") "$" "\\$"))

(defn- parse-bytes [encoded-bytes]
  (->> (re-seq #"%.." encoded-bytes)
       (map #(subs % 1))
       (map #(.byteValue (Integer/parseInt % 16)))
       (byte-array)))

(defn percent-decode
  "Decode every percent-encoded character in the given string using the
  specified encoding, or UTF-8 by default."
  [encoded & [encoding]]
  (str/replace encoded
               #"(?:%..)+"
               (fn [cs]
                 (-> (parse-bytes cs)
                     (String. (or encoding "UTF-8"))
                     (double-escape)))))

(defn- ->resource [public-dir path]
  (let [path (if (#{"/" ""} path) "index.html" (percent-decode path))]
    (.getResource (ClassLoader/getSystemClassLoader)
                  (format "%s/%s" public-dir (strip-leading-slash path)))))

(defn- mime-type [filename-or-resource]
  (ext-mime-type (str filename-or-resource)))

(defn- string->stream [s]
  (java.io.ByteArrayInputStream. (.getBytes s "UTF-8")))

(defn- create-api-resource-handler [ring-response]
  (let [resource-input-stream (if (string? (:body ring-response))
                                (string->stream (:body ring-response))
                                (:body ring-response))]
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
  (let [resource-input-stream (BufferedInputStream. (io/input-stream resource))
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
    (processRequest [& args]
      (.cancel (last args))
      false)))

(defn- get-api-handler [ring-handler _browser _frame request & _args]
  (let [url            (URL. (.getURL request))
        _req-protocol  (.getProtocol url)
        _req-authority (.getAuthority url)
        _req-path      (.getPath url)]
    #_(println [:GET-API-HANDLER _req-protocol _req-authority _req-path response])
    (try 
      (let [response (ring-handler (cef-request->ring-request request))]
        (create-api-resource-handler response))
      (catch Exception e
        (.printStackTrace e)
        (str "caught exception: " (.getMessage e))))))

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
          (if (filename-ext req-path)
            (apply get-resource-handler public-dir args)
            (apply get-api-handler ring-handler args))))

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
