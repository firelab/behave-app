(ns behave-cms.server
  (:require [clojure.core.server :refer [start-server]]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io     :as io]
            [clojure.string      :as str]
            [clojure.tools.cli   :refer [parse-opts]]
            [ring.adapter.jetty  :refer [run-jetty]]
            [triangulum.config   :refer [get-config]]
            [triangulum.logging  :refer [log-str set-log-path!]]
            [behave.store        :as store]
            [behave-cms.handler  :refer [create-handler-stack]])
  (:gen-class))

(defonce server           (atom nil))
(defonce repl-server      (atom nil))
(defonce clean-up-service (atom nil))

(def ^:private expiration-time "1 hour in msecs" (* 1000 60 60))
(def ^:private keystore-scan-interval 60)


;; Helper functions

(defn init! []
  (load-config (io/resource "cms-config.edn"))
  (store/connect! (get-config :database :config)))

(defn- expired? [last-mod-time]
  (let [current-time (System/currentTimeMillis)]
    (> (- current-time last-mod-time) expiration-time)))

(defn- delete-tmp []
  (log-str "Removing temp files.")
  (let [tmp-dir (System/getProperty "java.io.tmpdir")
        dirs    (filter #(and (.isDirectory %)
                              (str/includes? (.getPath %) "behave-cms-tmp")
                              (expired? (.lastModified %)))
                        (.listFiles (io/file tmp-dir)))]
    (doseq [dir  dirs
            file (reverse (file-seq dir))]
      (io/delete-file file))))

(defn- start-clean-up-service! []
  (log-str "Starting temp file removal service.")
  (future
    (while true
      (Thread/sleep expiration-time)
      (try (delete-tmp)
           (catch Exception _)))))

(def ^:private cli-options
  {:http-port  ["-p" "--http-port PORT"  "Port for http, default 8080"
                :parse-fn #(if (int? %) % (Integer/parseInt %))]
   :https-port ["-P" "--https-port PORT" "Port for https (e.g. 8443)"
                :parse-fn #(if (int? %) % (Integer/parseInt %))]
   :mode       ["-m" "--mode MODE" "Production (prod) or development (dev) mode, default prod"
                :default "prod"
                :validate [#{"prod" "dev"} "Must be \"prod\" or \"dev\""]]
   :log-dir    ["-l" "--log-dir DIR" "Directory for log files. When a directory is not provided, output will be to stdout."
                :default ""]})

(defn- get-option-default [option]
  (loop [cur     (first option)
         tail    (next option)
         result  []]
    (cond
      (nil? cur)
      {:option result :default nil}

      (= :default cur)
      {:option  (vec (concat result (next tail)))
       :default (first tail)}

      :else
      (recur (first tail)
             (next tail)
             (conj result cur)))))

(defn- separate-options-defaults [options]
  (reduce (fn [acc [k v]]
            (let [{:keys [option default]} (get-option-default v)]
              (-> acc
                  (assoc-in [:options k] option)
                  (assoc-in [:defaults k] default))))
          {:options {} :defaults {}}
          options))

;; Public functions

(defn start-server! [{:keys [http-port https-port mode log-dir repl]}]
  (let [has-key?   (.exists (io/file "./.key/keystore.pkcs12"))
        ssl?       (and has-key? https-port)
        handler    (create-handler-stack ssl? (= mode "dev"))
        config     (merge
                     {:port  http-port
                      :join? false}
                     (when ssl?
                       {:ssl?          true
                        :ssl-port      https-port
                        :keystore      "./.key/keystore.pkcs12"
                        :keystore-type "pkcs12"
                        :keystore-scan-interval keystore-scan-interval
                        :key-password  "foobar"}))]
    (if (and (not has-key?) https-port)
      (log-str "ERROR:\n"
               "  An SSL key is required if an HTTPS port is specified.\n"
               "  Create an SSL key for HTTPS or run without the --https-port (-P) option.")
      (do
        (when repl
          (println "Starting REPL server on port 5555")
          (reset! repl-server (start-server {:name :pyr-repl :port 5555 :accept 'clojure.core.server/repl})))
        (reset! server (run-jetty handler config))
        (reset! clean-up-service (start-clean-up-service!))
        (set-log-path! log-dir)
        (when (= mode "prod")
          (browse-url (str "http://localhost:" http-port)))))))

(defn -main [& args]
  (let [{:keys [options defaults]} (separate-options-defaults cli-options)
        {:keys [summary errors options]} (->> options
                                              (vals)
                                              (parse-opts args))]
    (if (nil? errors)
      (start-server! (merge defaults (get-config :server) options))
      (println summary errors))))
