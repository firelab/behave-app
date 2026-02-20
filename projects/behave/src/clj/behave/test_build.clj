(ns behave.test-build
  (:require [clojure.java.io    :as io]
            [clojure.string     :as str]
            [clj-commons.digest :as digest]))

(defn- inline-onload-js []
  (slurp (io/resource "onload.js")))

(defn- test-index-html
  "Build an index.html that mirrors the production loading chain:
   behave-min.js -> behave-min.wasm -> test.js -> init()"
  []
  (str "<!DOCTYPE html>\n"
       "<html>\n"
       "<head><title>kaocha.cljs2.shadow-runner</title><meta charset=\"utf-8\"></head>\n"
       "<body>\n"
       "<script>\n"
       "window.onWASMModuleLoadedPath = \"/js/test.js\";\n"
       "window.onAppLoaded = function () { kaocha.cljs2.shadow_runner.init(); };\n"
       (inline-onload-js)
       "</script>\n"
       "<script src=\"/js/behave-min.js\"></script>\n"
       "</body>\n"
       "</html>\n"))

(defn flush-hook
  "Shadow-cljs :flush hook — copies behave-min assets to the test dir
   and rewrites index.html with the WASM loading chain."
  {:shadow.build/stage :flush}
  [build-state & _args]
  (let [test-dir (get-in build-state [:shadow.build/config :test-dir] "target/test")
        js-dir   (io/file test-dir "js")
        src-dir  (io/file "resources/public/js")]
    (.mkdirs js-dir)
    (doseq [f ["behave-min.js" "behave-min.wasm"]]
      (let [src (io/file src-dir f)]
        (when (.exists src)
          (io/copy src (io/file js-dir f)))))
    (let [msgpack-src (io/file "resources/public/layout.msgpack")]
      (when (.exists msgpack-src)
        (io/copy msgpack-src (io/file test-dir "layout.msgpack"))))
    (spit (io/file test-dir "index.html") (test-index-html)))
  build-state)

;;; Browser SPA build

(defn- browser-index-html
  "Build an index.html for the standalone SPA that mirrors the production
   page from behave.views/render-page:
   behave-min.js -> behave-min.wasm -> app.js -> behave.client.init()"
  []
  (str/join
   "\n"
   ["<!DOCTYPE html>"
    "<html>"
    "<head>"
    "  <title>BehavePlus</title>"
    "  <meta charset=\"utf-8\">"
    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">"
    "  <link rel=\"icon\" type=\"image/png\" href=\"/images/favicon-96x96.png\" sizes=\"96x96\">"
    "  <link rel=\"icon\" type=\"image/svg+xml\" href=\"/images/favicon.svg\">"
    "  <link rel=\"shortcut icon\" type=\"image/png\" href=\"/images/favicon.ico\">"
    "  <link rel=\"apple-touch-icon\" href=\"/images/apple-touch-icon.png\" type=\"image/png\" sizes=\"180x180\">"
    "  <link rel=\"manifest\" href=\"/manifest.json\">"
    "  <link rel=\"stylesheet\" href=\"/css/roboto-font.css\">"
    "  <link rel=\"stylesheet\" href=\"/css/component-style.css\">"
    "  <link rel=\"stylesheet\" href=\"/css/app-style.css\">"
    "</head>"
    "<body>"
    "  <div id=\"app\"></div>"
    "  <script>"
    "    window.onWASMModuleLoadedPath = \"/js/app.js\";"
    "    window.onAppLoaded = function () { behave.client.init({standalone: true}); };"
    (inline-onload-js)
    "  </script>"
    "  <script src=\"/js/behave-min.js\"></script>"
    "  <script src=\"/js/katex.min.js\"></script>"
    "  <script src=\"/js/bodymovin.js\"></script>"
    "</body>"
    "</html>"]))

(defn browser-flush-hook
  "Shadow-cljs :flush hook for the :browser build — copies behave-min
   assets and vendor JS to the output dir and writes index.html."
  {:shadow.build/stage :flush}
  [build-state & _args]
  (let [out-dir (get-in build-state [:shadow.build/config :output-dir] "target/browser/js")
        app-dir (-> (io/file out-dir) (.getParentFile))
        js-dir  (io/file app-dir "js")
        src-dir (io/file "resources/public/js")]
    (.mkdirs js-dir)
    (doseq [f ["behave-min.js" "behave-min.wasm" "katex.min.js" "bodymovin.js"]]
      (let [src (io/file src-dir f)]
        (when (.exists src)
          (io/copy src (io/file js-dir f)))))
    (spit (io/file app-dir "index.html") (browser-index-html)))
  build-state)

;;; JCEF app build

(defn- clean-cljs-dir!
  "Remove stale build artifacts (app-*.js, manifest.edn) from `dir`."
  [dir]
  (when (.isDirectory dir)
    (doseq [f (.listFiles dir)]
      (.delete f))))

(defn app-configure-hook
  "Shadow-cljs :configure hook for the :app build — cleans stale artifacts
   from the output dir before compilation."
  {:shadow.build/stage :configure}
  [build-state & _args]
  (let [out-dir (get-in build-state [:shadow.build/config :output-dir] "resources/public/cljs")]
    (clean-cljs-dir! (io/file out-dir)))
  build-state)

(defn app-flush-hook
  "Shadow-cljs :flush hook for the :app build — generates a fingerprinted
   copy of app.js and a manifest.edn that views.clj's `find-app-js` reads."
  {:shadow.build/stage :flush}
  [build-state & _args]
  (let [out-dir  (get-in build-state [:shadow.build/config :output-dir] "resources/public/cljs")
        app-js   (io/file out-dir "app.js")]
    (when (.exists app-js)
      (let [md5      (subs (digest/md5 app-js) 0 7)
            dest     (io/file out-dir (str "app-" md5 ".js"))
            manifest {"resources/public/cljs/app.js"
                      (str "resources/public/cljs/app-" md5 ".js")}]
        (io/copy app-js dest)
        (spit (io/file out-dir "manifest.edn") (pr-str manifest)))))
  build-state)
