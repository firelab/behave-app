(ns schema-migrate.runner
  (:require
   [clojure.java.io :as io]
   [clojure.string  :as str]
   [datomic.api     :as d]
   [datomic-store.main :as ds]
   [schema-migrate.core :as core]))

(defn migration-applied?
  "Check if a migration has already been applied."
  [conn migration-id]
  (some? (d/q '[:find ?e .
                :in $ ?id
                :where [?e :bp/migration-id ?id]]
              (d/db conn) migration-id)))

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

(defn discover-migrations
  "Find all migration namespaces in `dir`.
   Returns them sorted by name.
   Skips the template and namespaces with `^{:migrate/ignore? true}`.

   Each migration must export one of:
   - `payload-fn`    — a function of `conn` returning transaction data
   - `payload`       — a def containing transaction data
   - `payload-steps` — a vector of functions or payloads, executed in order"
  [dir]
  (->> (io/file dir)
       (.listFiles)
       (filter clj-file?)
       (map file->ns-sym)
       (sort)
       (remove #(= % 'migrations.template))
       (keep (fn [ns-sym]
               (require ns-sym)
               (let [ns-meta (meta (find-ns ns-sym))]
                 (when-not (:migrate/ignore? ns-meta)
                   {:id          (name ns-sym)
                    :ns-sym      ns-sym
                    :payload-var (or (ns-resolve ns-sym 'payload-fn)
                                    (ns-resolve ns-sym 'payload))
                    :steps-var   (ns-resolve ns-sym 'payload-steps)}))))
       vec))

(defn- resolve-payload
  "Resolve a payload value — call it if it's a function, otherwise return as-is."
  [p conn]
  (if (fn? p) (p conn) p))

(defn- run-single-step!
  "Transact a single payload with a migration marker. Returns the tx result."
  [conn payload marker]
  (ds/transact conn (concat payload [marker])))

(defn- run-multi-step!
  "Transact each step in order. If any step fails, roll back all
   previously completed steps in reverse order, then re-throw."
  [conn steps marker]
  (let [completed (atom [])]
    (try
      (doseq [step steps]
        (let [payload (resolve-payload step conn)
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
      (when-not (migration-applied? conn id)
        (println "Running migration:" id)
        (let [marker (core/->migration id)]
          (if steps-var
            (run-multi-step! conn @steps-var marker)
            (let [payload (resolve-payload @payload-var conn)]
              (run-single-step! conn payload marker))))
        (println "  Applied:" id)))))
