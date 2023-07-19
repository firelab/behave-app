(ns behave-cms.sqlite
  (:require [clojure.string       :as str]
            [clojure.java.io      :as io]
            [next.jdbc            :as jdbc]
            [next.jdbc.result-set :as rs]))

(def ^:private db-conn-url (atom "jdbc:sqlite::resource:behave.db"))
(def ^:private conn        (atom nil))

(defn db-conn []
  (when (nil? @conn)
    (reset! conn (jdbc/get-datasource @db-conn-url)))
  @conn)

;; SQLite specific
(defn call-sqlite!
  "Runs a sqllite3 sql command. An existing sqllite3 database must be provided."
  [query]
  (jdbc/execute! (db-conn)
                 (if (vector? query) query [query])
                 {:builder-fn rs/as-unqualified-lower-maps}))

(def exec! call-sqlite!)

(defn csv->table
  "Converts the CSV header into a 'CREATE TABLE...' statement."
  [csv-filename]
  (let [file        (io/file csv-filename)
        table-name  (str/replace (.. file toPath getFileName toString) ".csv" "")
        header-line (->> (slurp file) (str/split-lines) (first))
        columns     (-> header-line (str/replace "'" "") (str/split #","))]
    (format "CREATE TABLE IF NOT EXISTS %s (id INTEGER PRIMARY KEY AUTOINCREMENT, %s);"
            table-name
            (str/join ", " (map #(format "%s TEXT" %) columns)))))

(defn create-index!
  "Creates an index on a particular table with columns."
  [table columns]
  (exec! (format "CREATE UNIQUE INDEX IF NOT EXISTS %s ON %s (%s);"
                 (format "%s_%s_index" table (str/join "_" columns))
                 table
                 (str/join ", " columns))))

(defn version
  "Returns the SQLite version."
  []
  (exec! "SELECT sqlite_version();"))

(defn tables
  "Returns all of the tables from SQLite."
  []
  (exec! "SELECT name FROM sqlite_master WHERE type = 'table';"))

(defn indexes
  "Returns the indexes in the database."
  []
  (exec! (format "SELECT name, tbl_name FROM sqlite_master WHERE type = 'index';")))

(defn columns
  "Returns all columns from the table."
  [table]
  (exec! (format "PRAGMA table_info(%s);" table)))

(defn add-column!
  "Adds a column to the table if it does not exist."
  [table column data-type]
  (when-not ((set (map :name (columns table))) column)
    (exec! (format "ALTER TABLE %s ADD COLUMN %s %s" table column data-type))))

(defn- empty-value? [v] (or (empty? v) (= "''" v)))

(defn import-csv! [csv-filename table]
  (let [csv-lines (->> (slurp csv-filename) (str/split-lines))
        columns   (-> (first csv-lines) (str/replace "'" ""))
        fmt-row   (fn [row] (format "(%s)" (str/join "," (map #(if (empty-value? %) "NULL" %) (str/split row #",")))))]
    (exec! (format "INSERT INTO %s (%s) VALUES %s;"
            table
            columns
            (str/join ", " (map fmt-row (next csv-lines)))))))

;; https://sqlite.org/foreignkeys.html
(defn join-table [join-table-name t1 t2]
  (format "CREATE TABLE IF NOT EXISTS %s (id INTEGER PRIMARY KEY AUTOINCREMENT, %s);"
          join-table-name
          (format "%s_rid INTEGER NOT NULL, %s_rid INTEGER NOT NULL, FOREIGN KEY(%s_rid) REFERENCES %s(id), FOREIGN KEY(%s_rid) REFERENCES %s(id)" t1 t2 t1 t1 t2 t2)))

(defn- create-function-tables! []
  (exec! "CREATE TABLE IF NOT EXISTS functions (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, module TEXT);")
  (create-index! "functions" ["name"])

  (exec! (join-table "functions_outputs" "functions" "variables"))
  (add-column! "functions_outputs" "position" "INTEGER")
  (create-index! "functions_outputs" ["functions_rid" "variables_rid"])

  (exec! (join-table "functions_inputs" "functions" "variables"))
  (add-column! "functions_inputs" "position" "INTEGER")
  (create-index! "functions_inputs" ["functions_rid" "variables_rid"]))

(defn- import-functions-csv! []
  (let [csv-lines (-> (slurp "config/functions.csv") (str/split-lines))
        columns   (-> (first csv-lines) (str/replace "'" "") (str/split #","))
        map-keys  (vec (map keyword columns))
        fns       (map (fn [line] (->> (str/split line #",")
                                       (map #(str/replace % "'" ""))
                                       (map-indexed (fn [idx v] (assoc {} (get map-keys idx) v)))
                                       (reduce merge {})))
                       (next csv-lines))
        fns-map   (reduce (fn [acc v] (assoc acc (:name v) v)) {} fns)]

    (exec! (format "INSERT INTO functions (name, module) VALUES %s;"
                   (str/join ", " (map (fn [{:keys [name module]}] (format "('%s','%s')" name module)) fns))))

    (let [fns-with-ids (map (fn [{:keys [id name]}] (assoc (first (get fns-map name)) :id id)) (exec! "SELECT id, name FROM functions"))
          vars-grouped (group-by :name (exec! "SELECT id, name FROM variables"))
          fns-io-ids   (map (fn [{:keys [inputs outputs] :as f}]
                              (assoc f
                                     :inputs (vec (map #(assoc {:name %} :id (:id (get vars-grouped %))) inputs))
                                     :outputs (vec (map #(assoc {:name %} :id (:id (get vars-grouped %))) outputs))))
                            fns-with-ids)]

    (exec! (format "INSERT INTO functions_inputs (functions_rid, variables_rid, position) VALUES %s"
                   (str/join ", "
                             (flatten
                               (map (fn [{:keys [inputs] :as f}]
                                      (map-indexed (fn [idx io] (format "(%s, %s, %s)" (:id f) (:id io) idx)) inputs)) fns-io-ids)))))

    (exec! (format "INSERT INTO functions_outputs (functions_rid, variables_rid, position) VALUES %s"
                   (str/join ", "
                             (flatten
                               (map (fn [{:keys [outputs] :as f}]
                                      (map-indexed (fn [idx io] (format "(%s, %s, %s)" (:id f) (:id io) idx)) outputs)) fns-io-ids))))))))

(defn create-tables! []
  (exec! (csv->table "config/variables.csv"))
  (exec! (csv->table "config/modules.csv"))
  (exec! (csv->table "config/properties.csv"))

  (import-csv! "config/modules.csv" "modules")
  (import-csv! "config/properties.csv" "properties")
  (import-csv! "config/variables.csv" "variables")

  (create-index! "modules" ["name"])
  (create-index! "variables" ["name"])
  (create-index! "properties" ["name"])

  ; Create functions last
  (create-function-tables!)
  (import-functions-csv!))

(comment
  (version)
  (tables)
  (indexes)
  (columns "modules")
  (columns "properties")
  (io/file (.toURI (io/resource "behave.db")))

  (def new-columns ["cpp_class" "cpp_variable" "cpp_getter" "cpp_setter"])

  (columns "variables")
  (map :name *1)

  (for [c new-columns]
    (add-column! "variables" c "VARCHAR"))

)
