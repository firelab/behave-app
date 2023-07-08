(ns behave.views
  (:require [clojure.data.json :as json]
            [clojure.java.io   :as io]
            [clojure.string    :as str]
            [clojure.edn       :as edn]
            [config.interface  :refer [get-config]]
            [hiccup.page       :refer [html5 include-css include-js]]))

(defmacro inline-resource [resource-path]
  (slurp (clojure.java.io/resource resource-path)))

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
   [:title (get-config :site :title)]
   [:meta {:name    "description"
           :content (get-config :site :description)}]
   [:meta {:name "robots" :content "index, follow"}]
   [:meta {:charset "utf-8"}]
   [:meta {:name    "viewport"
           :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
   [:link {:rel "manifest" :href "/manifest.json"}]
   [:link {:rel "icon" :type "image/png" :href "/images/favicon.ico"}]
   [:link {:rel "apple-touch-icon" :href "/images/apple-touch-icon.png" :type "image/png"}]
   (include-css "/css/roboto-font.css" "/css/component-style.css" "/css/app-style.css")])

(defn- cljs-init
  "A JavaScript script that calls the `init` function in `client.cljs`.
  Provides the entry point for rendering the content on a page."
  [params]
  (let [app-js (find-app-js)]
    [:script {:type "text/javascript"}
     (->> [(str "window.onWASMModuleLoadedPath =\"" app-js "\";")
           (str "window.onAppLoaded = function () { behave.client.init(" (json/write-str params) "); };")
           (inline-resource "onload.js")]
          (str/join "\n"))]))

(defn render-page [{:keys [route-params] :as match}]
  (fn [{:keys [params]}]
    {:status  (if (some? match) 200 404)
     :headers {"Content-Type" "text/html"}
     :body    (html5
                (head-meta-css)
                [:body
                 [:div#app]
                 (cljs-init (merge route-params params (get-config :client)))
                 (include-js "/js/behave-min.js" "/js/katex.min.js")])}))
