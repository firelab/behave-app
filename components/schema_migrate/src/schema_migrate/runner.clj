(ns schema-migrate.runner
  (:require
   [clojure.java.io     :as io]
   [clojure.string      :as str]
   [datomic-store.main  :as ds]
   [datomic.api         :as d]
   [schema-migrate.core :as core]))

(defn migration-applied?
  "Check if a migration has already been applied."
  [db migration-id]
  (some? (d/q '[:find ?e .
                :in $ ?id
                :where [?e :bp/migration-id ?id]]
              db migration-id)))

(defn- clj-file? [^java.io.File f]
  (and (.isFile f)
       (.endsWith (.getName f) ".clj")
       (not (.startsWith (.getName f) "#"))
       (not (.startsWith (.getName f) "."))))

(defn- file->ns-sym [^java.io.File f]
  (-> (.getName f)
      (str/replace #"\.clj$" "")
      (str/replace "_" "-")
      (->> (str "migrations."))
      symbol))

(defn- read-ns-form
  "Read the first form from `file` without evaluating it. Returns nil on
   any read failure."
  [^java.io.File file]
  (try
    (with-open [r (java.io.PushbackReader. (io/reader file))]
      (binding [*read-eval* false]
        (read r)))
    (catch Exception _ nil)))

(defn- ns-ignored?
  "True when the file's `ns` form carries `:migrate/ignore? true` metadata,
   either as `^{…}` on the ns symbol or as an attr-map after an optional
   docstring. Reads only the first form — does not `require` the namespace."
  [^java.io.File file]
  (let [form (read-ns-form file)]
    (boolean
     (and (seq? form)
          (= 'ns (first form))
          (let [symbol-meta (meta (second form))
                attr-map    (->> (drop 2 form)
                                 (drop-while string?)
                                 first)]
            (or (:migrate/ignore? symbol-meta)
                (and (map? attr-map)
                     (:migrate/ignore? attr-map))))))))

(defn- resolve-dir
  "Resolve `dir` to a File. Tries `io/resource` (classpath) first so that the
   path works regardless of the JVM's working directory, then falls back to a
   raw filesystem path (used by tests that pass absolute temp-dir strings).
   Throws when neither resolves to an existing directory."
  ^java.io.File [dir]
  (let [f (if-let [url (io/resource dir)]
            (io/file (.toURI url))
            (io/file dir))]
    (when-not (.isDirectory f)
      (throw (ex-info (str "Migrations directory not found: " dir)
                      {:dir dir :resolved (str f)})))
    f))

(defn discover-migrations
  "Find all migration namespaces in `dir`.
   Returns them sorted by name.
   Skips the template and namespaces with `^{:migrate/ignore? true}`.

   The ignore check reads only each file's `ns` form, so top-level side
   effects in ignored migrations never fire.

   Each migration must export one of:
   - `payload-fn`    — a function of `db` returning transaction data
   - `payload`       — a def containing transaction data
   - `payload-steps` — a vector of `(fn [db] tx-data)` functions executed
                       in order; each receives a fresh `db` snapshot reflecting
                       all prior steps in the same migration"
  [dir]
  (->> (resolve-dir dir)
       (.listFiles)
       (filter clj-file?)
       (remove ns-ignored?)
       (sort-by #(.getName ^java.io.File %))
       (keep (fn [^java.io.File f]
               (let [ns-sym (file->ns-sym f)]
                 (when-not (= ns-sym 'migrations.template)
                   ;; load-file so migrations work from any directory,
                   ;; not just those on the classpath
                   (load-file (.getAbsolutePath f))
                   {:id          (name ns-sym)
                    :ns-sym      ns-sym
                    :payload-var (or (ns-resolve ns-sym 'payload-fn)
                                     (ns-resolve ns-sym 'payload))
                    :steps-var   (ns-resolve ns-sym 'payload-steps)}))))
       vec))

(defn- resolve-payload
  "Resolve a payload value — call it if it's a function, otherwise return as-is."
  [p db]
  (if (fn? p) (p db) p))

(defn- run-single-step!
  "Transact a single payload with a migration marker. Returns the tx result."
  [conn payload marker]
  (ds/transact conn (concat payload [marker])))

(defn- run-multi-step!
  "Transact each step in order. Each step must be `(fn [db] tx-data)`;
   `db` is a fresh snapshot taken after the prior step commits, so step N
   sees all writes from step N-1. If any step fails, roll back all previously
   completed steps in reverse order, then re-throw."
  [conn steps marker]
  (let [completed (atom [])]
    (try
      (doseq [step steps]
        (assert (fn? step) "payload-steps entries must be (fn [db] tx-data)")
        (let [db        (d/db conn)
              payload   (step db)
              tx-result (ds/transact conn payload)]
          (swap! completed conj tx-result)))
      ;; All steps succeeded — record the migration marker
      (ds/transact conn [marker])
      (catch Exception e
        ;; Roll back completed steps in reverse order
        (doseq [tx-result (reverse @completed)]
          (try
            (core/rollback-tx! conn tx-result)
            (catch Exception rollback-ex
              (println "  WARNING: rollback failed:" (.getMessage rollback-ex)))))
        (throw e)))))

(defn run-pending-migrations!
  "Run all pending migrations in order. Halts on failure.
   Multi-step migrations are rolled back if any step fails."
  [conn dir]
  (let [migrations (discover-migrations dir)]
    (doseq [{:keys [id payload-var steps-var]} migrations]
      (let [db (d/db conn)]
        (cond
          (migration-applied? db id)
          nil

          (and (nil? payload-var) (nil? steps-var))
          (println "Skipping" id "— no payload-fn, payload, or payload-steps defined")

          :else
          (do (println "Running migration:" id)
              (let [marker (core/->migration id)]
                (if steps-var
                  (run-multi-step! conn @steps-var marker)
                  (let [payload (resolve-payload @payload-var db)]
                    (run-single-step! conn payload marker))))
              (println "  Applied:" id)))))))
