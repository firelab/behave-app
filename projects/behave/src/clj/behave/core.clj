(ns behave.core
  (:gen-class)
  (:import [javax.swing JFrame SwingUtilities UIManager]
           [javax.imageio ImageIO])
  (:require [clojure.java.io      :as io]
            [behave.handlers      :refer [create-cef-handler-stack]]
            [behave.server        :refer [init-config! init-db!]]
            [file-utils.interface :refer [os-path os-type app-data-dir app-logs-dir]]
            [config.interface     :refer [get-config]]
            [jcef.interface       :refer [create-cef-app! custom-request-handler show-loader!]]
            [logging.interface    :as l :refer [log-str]]))

;;; Helpers

(defn- standalone-db-location [org-name app-name]
  (str (io/file (app-data-dir org-name app-name) "db.sqlite")))

;;; Logging

(defn- log-system-start! []
  (log-str [:SYSTEM])
  (doseq [[k v] (into {} (System/getProperties))]
    (log-str k ": " v))
  (log-str (get-config)))

(defn- start-logging! [log-opts]
  (let [log-opts (update log-opts :log-dir os-path)]
    (io/make-parents (:log-dir log-opts))
    (l/start-logging! log-opts)
    (log-system-start!)))

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
      (.setUndecorated jframe true)
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

(defn -main
  "CEF client start method."
  [& _args]
  (init-config!)
  (let [loader          (show-loader! "Behave7" (io/resource "public/images/android-chrome-512x512.png"))
        mode            (get-config :server :mode)
        http-port       (or (get-config :server :http-port) 8080)
        org-name        (get-config :site :org-name)
        app-name        (get-config :site :app-name)
        log-config      (if (= "prod" mode)
                            (assoc (get-config :logging) :log-dir (app-logs-dir org-name app-name))
                            (get-config :logging))
        db-config       (if (= "prod" mode)
                            (assoc-in (get-config :database :config)
                                      [:store :path]
                                      (standalone-db-location org-name app-name))
                            (get-config :database :config))
        request-handler (custom-request-handler
                         {:protocol     "http"
                          :authority    (format "localhost:%s" http-port)
                          :resource-dir "public"
                          :ring-handler (create-cef-handler-stack)})]

    (start-logging! log-config)
    (init-db! db-config)
    (create-cef-app!
     {:title           (get-config :site :title)
      :url             (str "http://localhost:" http-port)
      :fullscreen?     true
      :on-shown        (fn [app & _]
                         (reset! the-app app)
                         (.dispose (:frame loader)))
      :request-handler request-handler
      :menu            [{:title "File"
                         :items [{:label       "Open"
                                  :mnemonic    "O"
                                  :description "Opens a file"
                                  :shortcut    "O"
                                  :on-select   (fn [_]
                                                 (println "Hello Open File!"))}
                                 {:label       "Save"
                                  :mnemonic    "S"
                                  :description "Saves a file"
                                  :shortcut    "S"
                                  :on-select   (fn [_]
                                                 (println "Hello Save File!"))}]}]
      :on-before-launch
      (fn [{:keys [client frame]}]
        (.addRequestHandler client (custom-request-handler {:protocol     "http"
                                                            :authority    "localhost:4242"
                                                            :resource-dir "public"
                                                            :ring-handler (create-cef-handler-stack)}))
        (on-before-launch frame (get-config :site :title)))})))

(comment
  (-main)
  (require '[jcef.core :as jc])
  (jc/show-dev-tools! (:browser @the-app))
  )
