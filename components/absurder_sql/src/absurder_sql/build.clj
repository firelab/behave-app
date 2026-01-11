(ns absurder-sql.build
  (:require [clojure.java.io :as io]))

(defn test-hook
  {:shadow.build/stage :flush}
  [build-state & _args]

  (println "Copying SQLite Files to tests dir" (io/resource "public/js/sqlite.js"))
  (io/copy (io/file (io/resource "public/js/sqlite.js")) (io/file "target/test/js/sqlite.js"))
  (io/copy (io/file (io/resource "public/js/sqlite.wasm")) (io/file "target/test/js/sqlite.wasm"))

  build-state)
