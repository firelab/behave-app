(ns behave.test-build
  (:require [clojure.java.io :as io]))

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
