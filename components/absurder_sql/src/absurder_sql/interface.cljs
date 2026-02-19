(ns absurder-sql.interface
  (:require [absurder-sql.core :as c]))

(def ^{:doc      "Initializes in-browser SQLite via [AbsurderSQL](https://github.com/npiesco/absurder-sql)."
       :arglists '()}
  init! c/init!)

(def ^{:doc      "Initializes in-browser SQLite via [AbsurderSQL](https://github.com/npiesco/absurder-sql)."
       :arglists '()}
  connected? c/connected?)

(def ^{:doc      "Creates a new in-browser SQLite database connection to `db-name`."
       :arglists '([db-name])}
  connect! c/connect!)

(def ^{:doc      "Closes an existing SQLite database connection."
       :arglists '([connection])}
  close! c/close!)

(def ^{:doc      "Flushes in-memory VFS blocks to IndexedDB."
       :arglists '([connection])}
  sync! c/sync!)

(def ^{:doc      "Executes `sql` on a SQLite database connection."
       :arglists '([connection sql])}
  execute! c/execute!)

(def ^{:doc      "Executes parameterized `sql` with `params` on a SQLite database connection."
       :arglists '([connection sql params])}
  execute-params! c/execute-params!)

(def ^{:doc      "Executes a query and returns a promise of a vector of Clojure maps."
       :arglists '([connection sql])}
  select c/select)

(def ^{:doc      "Executes a parameterized query and returns a promise of a vector of Clojure maps."
       :arglists '([connection sql params])}
  select-params c/select-params)

(def ^{:doc      "Imports `db-bytes` to a SQLite Database."
       :arglists '([connection sql])}
  import! c/import!)

(def ^{:doc      "Exports a SQLite Database as bytes to download/share."
       :arglists '([connection sql])}
  export! c/export!)

(def ^{:doc      "Downloads a SQLite Database."
       :arglists '([connection sql])}
  download! c/download!)
