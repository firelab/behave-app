(ns behave.test-build
  (:require [clojure.java.io    :as io]
            [clojure.string     :as str]
            [clj-commons.digest :as digest]
            [behave.views       :as views]))

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
  "Generates index.html by calling views/render-page, then replacing the
   production app.js path (/cljs/app-<hash>.js) with the :browser build
   path (/js/app.js) so shadow-cljs devtools connect properly."
  []
  (let [handler (views/render-page {:route-params {:standalone true}})
        html    (:body (handler {}))]
    (str/replace html #"/cljs/app-[a-f0-9]+\.js" "/js/app.js")))

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
