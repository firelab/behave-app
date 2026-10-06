(ns behave.core
  (:gen-class)
  (:import [javax.imageio ImageIO]
           [javax.swing JFrame JPopupMenu SwingUtilities UIManager])
  (:require [behave.browser-log   :as browser-log]
            [behave.handlers      :refer [create-cef-handler-stack]]
            [behave.server        :as server]
            [clojure.java.io      :as io]
            [config.interface     :refer [get-config]]
            [file-utils.interface :refer [os-type app-data-dir]]
            [logging.interface    :as l :refer [log-str]]))

;;; Logging

(defn- log-system-start! []
  (log-str [:SYSTEM])
  (doseq [[k v] (into {} (System/getProperties))]
    (log-str k ": " v))
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

(defn- test-build?
  "True for test builds (`:build {:test? true}` in config.edn), which add the
  Help menu diagnostics."
  []
  (true? (get-config :build :test?)))

(defn- diagnostics-options
  "`create-cef-app!` options for the test-build Help menu: Show Browser Logs
  and Enable Solver Logging (re-applied on every page load)."
  [data-dir log-dir]
  {:on-load-end (fn [{:keys [browser]}]
                  (browser-log/push-solver-logging!
                   browser (browser-log/solver-logging? data-dir)))
   :menu        [{:title "Help"
                  :items [{:label     "Show Browser Logs"
                           :mnemonic  "B"
                           :on-select (fn [_] (browser-log/show-viewer! log-dir))}
                          {:label     "Enable Solver Logging"
                           :mnemonic  "S"
                           :checked   (browser-log/solver-logging? data-dir)
                           :on-select (fn [{:keys [app selected?]}]
                                        (browser-log/set-solver-logging!
                                         data-dir (:browser app) selected?))}]}]})

;;; Entry Points

(defn- start-cef!
  "Start the app in JCEF desktop mode."
  []
  ;; Lazy-require jcef so server mode never loads jcef namespaces or
  ;; org.cef.* classes. Only this code path pulls in the native bundle.
  (let [show-loader!           (requiring-resolve 'jcef.interface/show-loader!)
        create-cef-app!        (requiring-resolve 'jcef.interface/create-cef-app!)
        custom-request-handler (requiring-resolve 'jcef.interface/custom-request-handler)
        loader                 (show-loader! "Behave7" (io/resource "public/images/android-chrome-512x512.png"))
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
                                 :ring-handler (create-cef-handler-stack)})
        test?                  (test-build?)]

    (start-logging! log-config)
    (server/init-db! db-config)

    ;; The CEF browser is a heavyweight component; lightweight Swing menus
    ;; would render behind it.
    (when test?
      (JPopupMenu/setDefaultLightWeightPopupEnabled false))

    (create-cef-app!
     (merge
      {:title                                                (get-config :site :title)
       :url                                                  (str "http://localhost:" http-port)
       :cache-path                                           cache-path
       :fullscreen?                                          true
       :on-shown                                             (fn [app & _]
                                                               (reset! the-app app)
                                                               (.dispose (:frame loader)))
       :on-console-message                                   (fn [{:keys [level message source line] :as msg}]
                                                               (when test? (browser-log/record! msg))
                                                               (log-str "[BROWSER " level "] " source ":" line " - " message))
       :request-handler                                      request-handler
       :on-before-launch
       (fn [{:keys [frame]}]
         (on-before-launch frame (get-config :site :title)))}
      (when test?
        (diagnostics-options my-app-data-dir (:log-dir log-config)))))))

(defn -main
  "Unified entry point. Detects runtime environment and starts
   in CEF desktop mode or HTTP server mode."
  [& _args]
  (if (conveyor?)
    (do
      (server/init-config!)
      (server/enrich-config!)
      (start-cef!))
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
