(ns behave.views
  (:require [clojure.data.json  :as json]
            [clojure.java.io    :as io]
            [clojure.string     :as str]
            [clojure.edn        :as edn]
            [config.interface   :refer [get-config]]
            [hiccup.page        :refer [html5] :as p]
            [clj-commons.digest :as digest]))

;;; Macros

(defmacro inline-resource [resource-path]
  (slurp (clojure.java.io/resource resource-path)))

;;; State

(def vms-version (atom nil))

;;; Helpers

(declare reset-vms-version!)

(defn- get-vms-version []
  {:vms-version (if @vms-version @vms-version (reset-vms-version!))})

(defn- add-v-query [paths]
 (let [{:keys [vms-version]} (get-vms-version)]
    (map #(str % "?v=" vms-version) paths)))

(defn- include-css [& paths]
  (apply p/include-css (add-v-query paths)))

(defn- include-js [& paths]
  (apply p/include-js (add-v-query paths)))

(defn- find-app-js []
  (if-let [manifest (io/resource "public/cljs/manifest.edn")]
    (as-> (slurp manifest) app
      (edn/read-string app)
      (get app "resources/public/cljs/app.js" "resources/public/cljs/app.js")
      (str/split app #"/")
      (last app)
      (str "/cljs/" app))
    "/cljs/app.js"))

(defn- head-meta-css
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
  [params & [figwheel?]]
  (if figwheel?
    [:script {:type "text/javascript"}
     (str "window.onload = function () {behave.client.init(" (json/write-str params) "); };")]
    (let [app-js (find-app-js)]
      [:script {:type "text/javascript"}
       (->> [(str "window.onWASMModuleLoadedPath =\"" app-js "\";")
             (str "window.onAppLoaded = function () { behave.client.init(" (json/write-str params) "); };")
             (inline-resource "onload.js")]
            (str/join "\n"))])))

;;; Public Fns

(defn reset-vms-version!
  "Resets the VMS version based on the file hash."
  []
  (reset! vms-version (digest/md5 (io/file (io/resource "public/layout.msgpack")))))

(defn render-tests-page [_request]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (html5
             [:meta
              [:title "BehavePlus Tests"]]
             [:body
              [:div#app-testing]
              (include-js "/js/behave-min.js" "/cljs/app-testing.js")])})

(defn render-page [{:keys [route-params] :as match}]
  (fn [{:keys [params figwheel?]}]
    (let [init-params
          (merge route-params
                 params
                 (get-config :client)
                 (get-config :server)
                 (get-vms-version))]
      {:status  (if (some? match) 200 404)
       :headers {"Content-Type" "text/html"}
       :body    (html5
                  (head-meta-css)
                  [:body
                   [:div#app]
                   (cljs-init init-params figwheel?)
                   (include-js "/js/behave-min.js" "/js/katex.min.js" "/js/bodymovin.js")
                   (when figwheel? (include-js (find-app-js)))])})))
