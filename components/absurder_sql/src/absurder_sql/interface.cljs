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

(def ^{:doc      "Executes `sql` on a SQLite database connection."
       :arglists '([connection sql])}
  execute! c/execute!)

(def ^{:doc      "Imports `db-bytes` to a SQLite Database."
       :arglists '([connection sql])}
  import! c/import!)

(def ^{:doc      "Exports a SQLite Database as bytes to download/share."
       :arglists '([connection sql])}
  export! c/export!)

(def ^{:doc      "Downloads a SQLite Database."
       :arglists '([connection sql])}
  download! c/download!)
