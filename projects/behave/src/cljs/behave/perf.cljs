(ns behave.perf
  "Startup timing instrumentation (Phase 0 of STARTUP.org).

  Records `js/performance` marks under the `bhp:` prefix — the same prefix
  used by the marks set in `resources/onload.js` — and prints a startup
  report to the console once both boot stores have hydrated. All times are
  relative to `performance.timeOrigin` (navigation start).")

(defn mark!
  "Records a performance mark named `bhp:<label>`."
  [label]
  (when (exists? js/performance)
    (.mark js/performance (str "bhp:" (name label)))))

(defonce ^:private loaded-stores (atom #{}))

(defn- report!
  "Prints all `bhp:` marks as a console table, ordered by time."
  []
  (when (exists? js/performance)
    (->> (.getEntriesByType js/performance "mark")
         (filter #(.startsWith (.-name ^js %) "bhp:"))
         (sort-by #(.-startTime ^js %))
         (map (fn [^js entry]
                #js {:mark (subs (.-name entry) 4)
                     :ms   (js/Math.round (.-startTime entry))}))
         (into-array)
         (.table js/console))))

(defn store-loaded!
  "Marks boot store `store-key` (`:vms` or `:sync`) as hydrated. Once both
  stores have loaded, marks `app-ready` and prints the startup report."
  [store-key]
  (mark! (str (name store-key) "-loaded"))
  (when (= (swap! loaded-stores conj store-key) #{:vms :sync})
    (mark! "app-ready")
    (report!)))
