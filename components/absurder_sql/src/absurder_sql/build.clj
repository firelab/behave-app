(ns absurder-sql.build
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]))

(defn test-hook
  {:shadow.build/stage :flush}
  [build-state & _args]
  (let [test-dir (get-in build-state [:shadow.build/config :test-dir] "target/test")
        js-dir (io/file test-dir "js")
        src-dir (io/file "resources/public/js")]
    (println "Copying SQLite Files to" test-dir)
    (.mkdirs js-dir)
    (doseq [f ["sqlite.js" "sqlite.wasm" "users.db"]]
      (let [src (io/file src-dir f)]
        (when (.exists src)
          (io/copy src (io/file js-dir f))))))
  build-state)

(defn kaocha-hook
  {:shadow.build/stage :flush}
  [build-state & _args]
  (sh/sh "bin/chrome-refresh")
  (sh/sh "bin/kaocha")
  build-state)
