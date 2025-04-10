(ns behave.views
  (:require [clojure.data.json  :as json]
            [clojure.java.io    :as io]
            [clojure.string     :as str]
            [clojure.edn        :as edn]
            [config.interface   :refer [get-config]]
            [hiccup.page        :refer [html5] :as p]
            [clj-commons.digest :as digest]))

;;; Macros

(defmacro inline-resource
  "Inlines a file from the /resources directory."
  [resource-path]
  (slurp (io/resource resource-path)))

;;; State

(def ^:private *vms-version (atom nil))
(def ^:private *app-version (atom nil))

;;; Helpers

(declare reset-vms-version!)
(declare reset-app-version!)

(defn- get-vms-version []
  {:vms-version (if @*vms-version @*vms-version (reset-vms-version!))})

(defn- get-app-version []
  {:app-version (if @*app-version @*app-version (reset-app-version!))})

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
   [:title (if (:app-version (get-app-version))
            (format "%s (%s)"  (get-config :site :title) (:app-version (get-app-version)))
            (get-config :site :title))]
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

(defn- announcement-banner []
  (let [a11t-file    (io/resource "public/announcement.txt")
        announcement (if (nil? a11t-file)
                         []
                         (-> a11t-file
                             (slurp)
                             (str/split #"\n")))]
    (when-not (empty? (first announcement))
      [:div#banner {:style {:background-color "#082233"
                            :box-shadow       "3px 1px 4px 0 rgb(0, 0, 0, 0.25)"
                            :color            "#ffffff"
                            :display          (if (pos? (count announcement)) "block" "none")
                            :margin           "0px"
                            :padding          "5px"
                            :position         "fixed"
                            :text-align       "center"
                            :top              "0"
                            :right            "0"
                            :left             "0"
                            :width            "100vw"
                            :z-index          "10000"}}
       (map (fn [line]
              [:p {:style {:color       "#ffffff"
                           :font-size   "18px"
                           :font-weight "bold"
                           :margin      "0 30px 0 0"}
                   :key   line}
               line])
            announcement)
       [:button {:style   {:align-items      "center"
                           :background-color "transparent"
                           :border-color     "#ffffff"
                           :border-radius    "50%"
                           :border-style     "solid"
                           :border-width     "2px"
                           :cursor           "pointer"
                           :display          "flex"
                           :height           "25px"
                           :padding          "0"
                           :position         "fixed"
                           :right            "10px"
                           :top              "5px"
                           :width            "25px"}
                 :onClick "document.getElementById('banner').style.display='none'"}
        [:svg {:width "24px" :height "24px" :viewBox "0 0 48 48" :fill "#ffffff"}
         [:path {:d "M38 12.83l-2.83-2.83-11.17 11.17-11.17-11.17-2.83 2.83 11.17 11.17-11.17 11.17 2.83 2.83
                     11.17-11.17 11.17 11.17 2.83-2.83-11.17-11.17z"}]]]])))

;;; Public Fns

(defn reset-vms-version!
  "Resets the VMS version based on the file hash."
  []
  (reset! *vms-version (digest/md5 (slurp (io/resource "public/layout.msgpack")))))

(defn reset-app-version!
  "Resets the App version based on the file hash."
  []
  (reset! *app-version (some->> "version.edn"
                                io/resource
                                slurp
                                edn/read-string
                                :version)))

(defn render-tests-page
  "Renders the site for tests."
  [_request]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (html5
             [:meta
              [:title "BehavePlus Tests"]]
             [:body
              [:div#app-testing]
              (include-js "/js/behave-min.js" "/cljs/app-testing.js")])})

(defn render-page
  "Renders a page."
  [{:keys [route-params] :as match}]
  (fn [{:keys [params figwheel?]}]
    (let [init-params
          (merge route-params
                 params
                 (get-config :client)
                 (get-config :server)
                 (get-vms-version)
                 (get-app-version))]
      {:status  (if (some? match) 200 404)
       :headers {"Content-Type" "text/html"}
       :body    (html5
                  (head-meta-css)
                  [:body
                   (when (not (:ws-uuid route-params)) (announcement-banner))
                   [:div#app]
                   (cljs-init init-params figwheel?)
                   (include-js "/js/behave-min.js" "/js/katex.min.js" "/js/bodymovin.js")
                   (when figwheel? (include-js (find-app-js)))])})))
