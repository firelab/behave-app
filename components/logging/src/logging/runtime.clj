(ns logging.runtime)

(def MEGABYTE-FACTOR (* 1024 1024))

(defn- bytes-to-mib [bytes]
  (/ (double bytes) MEGABYTE-FACTOR))

(defn get-max-memory []
  (.maxMemory (Runtime/getRuntime)))

(defn get-free-memory []
  (.freeMemory (Runtime/getRuntime)))

(defn get-used-memory []
  (- (get-max-memory) (get-free-memory)))

(defn get-total-memory []
  (.totalMemory (Runtime/getRuntime)))

(defn get-total-memory-in-mib []
  (let [total-mib (bytes-to-mib (get-total-memory))]
    (str (format "%.2f" total-mib) " MiB")))

(defn get-free-memory-in-mib []
  (let [free-mib (bytes-to-mib (get-free-memory))]
    (str (format "%.2f" free-mib) " MiB")))

(defn get-used-memory-in-mib []
  (let [used-mib (bytes-to-mib (get-used-memory))]
    (str (format "%.2f" used-mib) " MiB")))

(defn get-max-memory-in-mib []
  (let [max-mib (bytes-to-mib (get-max-memory))]
    (str (format "%.2f" max-mib) " MiB")))

(defn get-percentage-used []
  (* (/ (double (get-used-memory)) (get-max-memory)) 100))

(defn get-percentage-used-formatted []
  (let [used-percentage (get-percentage-used)]
    (str (format "%.2f" used-percentage) "%")))

(defn get-system-information []
  (format "SystemInfo=Current heap:%s; Used:%s; Free:%s; Maximum Heap:%s; Percentage Used:%s"
          (get-total-memory-in-mib)
          (get-used-memory-in-mib)
          (get-free-memory-in-mib)
          (get-max-memory-in-mib)
          (get-percentage-used-formatted)))
