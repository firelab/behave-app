(ns behave.core
  (:gen-class)
  (:import [java.awt.event WindowEvent]
           [javax.swing JFrame SwingUtilities UIManager]
           [javax.imageio ImageIO])
  (:require [clojure.java.io      :as io]
            [clojure.string       :as str]
            [behave.handlers      :refer [create-cef-handler-stack]]
            [behave.server        :refer [init-config! init-db!]]
            [behave.windows       :as windows]
            [file-utils.interface :refer [os-type app-data-dir]]
            [config.interface     :refer [get-config]]
            [jcef.interface       :refer [init-cef-app! open-window!
                                          custom-request-handler show-loader!
                                          show-dev-tools!]]
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

;;; CefApp singleton (shared across windows)

(defonce ^:private *cef-app (atom nil))
(defonce ^:private *win-res (atom nil))

;;; Menu

(declare open-app-window!)

(defn- build-app-menu
  "Returns a menu definition vector for the app window."
  []
  [{:title "File"
    :items [{:label    "New Window"
             :shortcut "N"
             :on-select (fn [_] (SwingUtilities/invokeLater #(open-app-window!)))}
            {:separator? true}
            {:label    "Close Window"
             :shortcut "W"
             :on-select (fn [{:keys [app]}]
                          (let [^JFrame frame (:frame app)]
                            (.dispatchEvent frame
                                            (WindowEvent. frame WindowEvent/WINDOW_CLOSING))))}]}])

;;; Window Factory

(defn- open-app-window!
  "Opens a new independent app window."
  ([] (open-app-window! nil))
  ([loader]
   (let [window-id       (str (random-uuid))
         http-port       (or (get-config :server :http-port) 8080)
         my-app-data-dir (app-data-dir (get-config :site :org-name)
                                       (get-config :site :app-name))
         cache-path      (str (io/file my-app-data-dir ".cache"))
         request-handler (custom-request-handler
                          {:protocol     "http"
                           :authority    (format "localhost:%s" http-port)
                           :resource-dir "public"
                           :ring-handler (create-cef-handler-stack window-id)})]
     (open-window!
      @*cef-app
      {:title           (get-config :site :title)
       :url             (str "http://localhost:" http-port)
       :cache-path      cache-path
       :fullscreen?     true
       :menu            (build-app-menu)
       :request-handler request-handler
       :on-shown        (fn [app & _]
                          (windows/register-window! window-id {:app app})
                          (when loader (.dispose (:frame loader))))
       :on-close        (fn [] (windows/deregister-window! window-id))
       :on-telemetry    (fn [^String request]
                          (let [payload (subs request (count "telemetry:"))
                                sep-idx (.indexOf payload ":")
                                level   (if (pos? sep-idx) (subs payload 0 sep-idx) "info")
                                message (if (pos? sep-idx) (subs payload (inc sep-idx)) payload)]
                            (log-str "[BROWSER:" (str/upper-case level) "] " message)))
       :on-before-launch
       (fn [{:keys [frame]}]
         (on-before-launch frame (get-config :site :title)))}))))

(defn -main
  "CEF client start method."
  [& _args]
  (init-config!)
  (let [loader     (show-loader! "Behave7" (io/resource "public/images/android-chrome-512x512.png"))
        mode       (get-config :server :mode)
        log-config (if (= "prod" mode)
                     (let [dir (app-data-dir (get-config :site :org-name)
                                             (get-config :site :app-name))]
                       (assoc (get-config :logging) :log-dir (str (io/file dir "logs"))))
                     (get-config :logging))
        db-config  (if (= "prod" mode)
                     (let [dir (app-data-dir (get-config :site :org-name)
                                             (get-config :site :app-name))]
                       (assoc-in (get-config :database :config)
                                 [:store :path]
                                 (str (io/file dir "db"))))
                     (get-config :database :config))
        cef-app    (init-cef-app!
                    {:cache-path (str (io/file (app-data-dir (get-config :site :org-name)
                                                             (get-config :site :app-name))
                                               ".cache"))})]
    (start-logging! log-config)
    (init-db! db-config)
    (reset! *cef-app cef-app)
    (SwingUtilities/invokeLater #(let [res (open-app-window! loader)]
                                   (.openDevTools (:browser res))
                                   (reset! *win-res res)))))

(comment
  (-main)

  (def browser (:browser @*win-res))
  (.openDevTools browser)

  (show-dev-tools! (:browser @*win-res))

  (windows/window-count))
