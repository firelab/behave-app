(ns behave.core
  (:gen-class)
  (:import [javax.imageio ImageIO]
           [javax.swing JFrame SwingUtilities UIManager])
  (:require [behave.handlers      :refer [create-cef-handler-stack]]
            [behave.server        :as server :refer [jvm-uptime-ms]]
            [behave.views         :refer [warm-version-cache!]]
            [clojure.java.io      :as io]
            [config.interface     :refer [get-config]]
            [file-utils.interface :refer [os-type app-data-dir]]
            [logging.interface    :as l :refer [log-str timed]]))

;;; Logging

(defn- log-system-start! []
  (log-str [:SYSTEM {:java (System/getProperty "java.version")
                     :os   (str (System/getProperty "os.name") " " (System/getProperty "os.version"))
                     :arch (System/getProperty "os.arch")}])
  (log-str (get-config)))

(defn- start-logging! [log-opts]
  (io/make-parents (:log-dir log-opts))
  (l/start-logging! log-opts)
  (log-system-start!))

(defn- set-properties! [props]
  (doseq [[k v] props]
    (System/setProperty k v)))

(defn- get-icons []
  (->> ["public/images/android-chrome-192x192.png"
        "public/images/apple-touch-icon.png"
        "public/images/favicon-96x96.png"]
       (map #(ImageIO/read (io/resource %)))))

;; See: https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
(defn- on-before-launch [^JFrame jframe title]
  (UIManager/setLookAndFeel
   (UIManager/getSystemLookAndFeelClassName))
  (SwingUtilities/updateComponentTreeUI jframe)

  (condp = (os-type)

    ;; See: https://docs.oracle.com/javase/8/docs/technotes/guides/swing/1.4/w2k_props.html
    :windows
    (do
      (.setIconImages jframe (get-icons))
      (set-properties! {}))

    :linux
    (set-properties! {"sun.java2d.xrender" "true"})

    ;; See: https://alvinalexander.com/java/make-java-application-look-feel-native-mac-osx/
    :mac
    (set-properties!
     {"apple.laf.useScreenMenuBar"                      "true"
      "apple.awt.application.appearance"                "system"
      "com.apple.mrj.application.apple.menu.about.name" title})))

(defonce ^:private the-app (atom nil))

;;; Runtime Detection

(defn- conveyor?
  "True when running inside a Conveyor-packaged app (app.dir is set)."
  []
  (some? (System/getProperty "app.dir")))

;;; Entry Points

(defn start-cef!
  "Start the app in JCEF desktop mode. `loader` is an optional splash already
  created by `behave.Launcher` before this namespace tree loaded — either a
  raw `JFrame` (from Java) or a `{:frame ...}` map; when absent (dev/REPL),
  one is created here. `progress-consumer` is an optional
  `java.util.function.BiConsumer` of [percent message] used to report the
  startup checkpoints on the splash progress bar (see `behave.Launcher`)."
  [& [loader progress-consumer]]
  (log-str "[TIMING] app namespaces loaded " (jvm-uptime-ms) "ms after JVM start")
  (let [progress! (if progress-consumer
                    (fn [pct msg]
                      (.accept ^java.util.function.BiConsumer progress-consumer (int pct) msg))
                    (fn [_ _]))]
    (timed "load-config"
           (server/init-config!)
           (server/enrich-config!))
    (progress! 87 (rand-nth ["Checking the compass..."
                             "Consulting the field guide..."
                             "Reading the river..."]))
  ;; Lazy-require jcef so server mode never loads jcef namespaces or
  ;; org.cef.* classes. Only this code path pulls in the native bundle.
    (let [show-loader!           (requiring-resolve 'jcef.interface/show-loader!)
          create-cef-app!        (requiring-resolve 'jcef.interface/create-cef-app!)
          custom-request-handler (requiring-resolve 'jcef.interface/custom-request-handler)
          loader                 (cond
                                   (map? loader)  loader
                                   (some? loader) {:frame loader}
                                   :else          (timed "show-loader"
                                                         (show-loader! "Behave7" (io/resource "public/images/android-chrome-512x512.png"))))
          mode                   (get-config :server :mode)
          http-port              (or (get-config :server :http-port) 8080)
          org-name               (get-config :site :org-name)
          app-name               (get-config :site :app-name)
          my-app-data-dir        (app-data-dir org-name app-name)
          log-config             (if (= "prod" mode)
                                   (assoc (get-config :logging) :log-dir (str (io/file my-app-data-dir "logs")))
                                   (get-config :logging))
          db-config              (if (= "prod" mode)
                                   (assoc-in (get-config :database :config)
                                             [:store :path]
                                             (str (io/file my-app-data-dir "db.sqlite")))
                                   (get-config :database :config))
          cache-path             (str (io/file my-app-data-dir ".cache"))
          request-handler        (custom-request-handler
                                  {:protocol     "http"
                                   :authority    (format "localhost:%s" http-port)
                                   :resource-dir "public"
                                   :ring-handler (create-cef-handler-stack)})]

      (timed "start-logging" (start-logging! log-config))
    ;; DB restore and the layout.msgpack hash run in the background while
    ;; CEF/Chromium boots; API handlers gate on `behave.store/await-db!`.
      (server/init-db-async! db-config)
      (warm-version-cache!)
      (log-str "[TIMING] scheduling CEF app " (jvm-uptime-ms) "ms after JVM start")
      (progress! 90 (rand-nth ["Reeling it in..."
                               "Calling in the big one..."
                               "Netting the catch..."]))

      (create-cef-app!
       {:title                                                (get-config :site :title)
        :url                                                  (str "http://localhost:" http-port)
        :cache-path                                           cache-path
        :fullscreen?                                          true
        :on-shown                                             (fn [app & _]
                                                                (log-str "[TIMING] browser window shown "
                                                                         (jvm-uptime-ms) "ms after JVM start")
                                                                (progress! 100 nil)
                                                                (reset! the-app app)
                                                                (.dispose (:frame loader)))
        :request-handler                                      request-handler
        :on-before-launch
        (fn [{:keys [frame]}]
          (on-before-launch frame (get-config :site :title)))}))))

(defn -main
  "Legacy entry point (dev/REPL and `-m behave.core`). Packaged apps enter
   through `behave.Launcher` (pure Java), which shows the splash before this
   namespace tree loads. Detects runtime environment and starts in CEF
   desktop mode or HTTP server mode."
  [& _args]
  (log-str "[TIMING] -main entered " (jvm-uptime-ms) "ms after JVM start")
  (if (conveyor?)
    (start-cef!)
    (do
      (server/start-server!)
      ;; `server.core/start-server!` runs Jetty with `:join? false`, and
      ;; neither `vms-sync!` nor `watch-kill-signal!` blocks — so without
      ;; parking here the main thread would return and the JVM would exit
      ;; before any request could be served. Block until the process is
      ;; killed (Ctrl-C / SIGTERM).
      @(promise))))

(comment
  (-main)
  (start-cef!)
  ;; Dev Tools
  @the-app
  (require '[jcef.core :as jc])
  (jc/show-dev-tools! (:browser @the-app)))
