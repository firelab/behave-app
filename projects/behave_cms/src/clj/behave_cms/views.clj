(ns behave-cms.views
  (:import  [java.io ByteArrayOutputStream ByteArrayInputStream])
  (:require [clojure.edn       :as edn]
            [clojure.java.io   :as io]
            [clojure.string    :as str]
            [clojure.data.json :as json]
            [cognitect.transit :as transit]
            [msgpack.core      :as msg]
            [msgpack.extensions]
            [config.interface  :refer [get-config]]
            [hiccup.page       :refer [html5 include-css include-js]]))

(defn- find-app-js []
  (if-let [manifest (io/resource "public/cljs/manifest.edn")]
    (as-> (slurp manifest) app
      (edn/read-string app)
      (get app "resources/public/cljs/app.js" "resources/public/cljs/app.js")
      (str/split app #"/")
      (last app)
      (str "/cljs/" app))
     "/cljs/app.js"))

(defn head-meta-css
  "Specifies head tag elements."
  []
  [:head
   [:title "Behave CMS"]
   [:meta {:name    "description"
           :content ""}]
   [:meta {:name "robots" :content "index, follow"}]
   [:meta {:charset "utf-8"}]
   [:meta {:name    "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:link {:rel "icon" :type "image/png" :href "/images/favicon.png"}]
   (include-css "/css/bootstrap.min.css" "/css/katex.min.css" "/css/help.css" "/css/conditionals-table.css")
   (include-js (find-app-js) "/js/katex.min.js" "/js/fontawesome.js")])

(defn- cljs-init
  "A JavaScript script that calls the `init` function in `client.cljs`.
  Provides the entry point for rendering the content on a page."
  [params]
  [:script {:type "text/javascript"}
   (str "window.onload = function () {"
        "behave_cms.client.init("
        (json/write-str
         (merge params
                {:client-config
                 {:secret-token (get-config :secret-token)}}))
        "); };")])

(defn render-page [{:keys [route-params] :as match}]
  (fn [{:keys [params]}]
    {:status  (if (some? match) 200 404)
     :headers {"Content-Type" "text/html"}
     :body    (html5
                (head-meta-css)
                [:body
                 [:div#app]
                 (cljs-init (merge route-params params))])}))

(defn body->transit [body & [fmt]]
  (let [out    (ByteArrayOutputStream. 4096)
        writer (transit/writer out (or fmt :json))]
    (transit/write writer body)
    (.toString out)))

(defn body->msgpack [body]
  (ByteArrayInputStream. (msg/pack body)))

(defn data-response
  "Create a response object.
   Body is required. Status, type, and session are optional.
   When a type keyword is passed, the body is converted to that type,
   otherwise the body and type are passed through."
  ([body]
   (data-response body {}))
  ([body {:keys [status type session]
          :or   {status 200 type :edn}
          :as   params}]
   (merge (when (contains? params :session) {:session session})
          {:status  status
           :headers {"Content-Type" (condp = type
                                      :edn     "application/edn"
                                      :transit "application/transit+json"
                                      :json    "application/json"
                                      :msgpack "application/msgpack"
                                      type)}
           :body    (condp = type
                      :edn     (pr-str         body)
                      :transit (body->transit  body)
                      :json    (json/write-str body)
                      :msgpack (body->msgpack  body)
                      body)})))
