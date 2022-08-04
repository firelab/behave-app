(ns behave.views
  (:require [clojure.data.json :as json]
            [clojure.java.io   :as io]
            [clojure.string    :as str]
            [clojure.edn       :as edn]
            [hiccup.page       :refer [html5 include-css include-js]]))

(defn- find-app-js []
  (as-> (slurp (io/resource "public/cljs/manifest.edn")) app
    (edn/read-string app)
    (get app "resources/public/cljs/app.js" "target/public/cljs/app.js")
    (str/split app #"/")
    (last app)
    (str "/cljs/" app)))

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
   (include-css "/css/style.css")])

(defn- cljs-init
  "A JavaScript script that calls the `init` function in `client.cljs`.
  Provides the entry point for rendering the content on a page."
  [params]
  [:script {:type "text/javascript"}
   (str "window.onload = function () {
        behave.client.init(" (json/write-str params) "); };")])

(defn render-page [{:keys [route-params] :as match}]
  (fn [{:keys [params]}]
    {:status  (if (some? match) 200 404)
     :headers {"Content-Type" "text/html"}
     :body    (html5
                (head-meta-css)
                [:body
                 [:div#app]
                 (include-js (find-app-js) "/js/katex.min.js")
                 (cljs-init (merge route-params params))])}))
