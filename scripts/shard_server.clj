;; One figwheel-free BehavePlus app server for a single cucumber shard.
;;
;; Serves the precompiled static assets (resources/public/cljs — build once with
;; `clojure -M:compile-cljs`) via the non-figwheel handler stack, with a per-shard DB and
;; port so N of these run in parallel, fully isolated. The DB path comes from
;; -Dbehave.store.path (honored again on every /api/init via behave.init/init!, which reloads
;; config.edn), the port from -Dshard.port. vms-sync! is intentionally skipped (the suite
;; serves the pre-exported layout.msgpack), matching scripts/cucumber_ci.clj.
;;
;; Launched by scripts/cucumber_shard.clj:
;;   clojure -J-Dbehave.store.path=<db> -J-Dshard.port=<port> -M:dev:behave/app scripts/shard_server.clj
(require '[behave.server    :as server]
         '[server.interface :as srv]
         '[behave.handlers  :refer [server-handler-stack]]
         '[config.interface :refer [get-config]])

(server/init-config!)
(server/enrich-config!)
(let [port (Integer/parseInt (or (System/getProperty "shard.port") "8091"))
      dbp  (System/getProperty "behave.store.path")]
  (server/init-db! (cond-> (get-config :database :config)
                     dbp (assoc-in [:store :path] dbp)))
  (srv/start-server! {:handler (server-handler-stack {:figwheel? false}) :port port})
  (println (format "SHARD-SERVER-UP port=%d db=%s" port dbp))
  @(promise))                                  ; keep the JVM alive until the orchestrator kills it
