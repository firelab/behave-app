(ns jcef.core
  (:require [clojure.string :as str]
            [clojure.java.browse :refer [browse-url]]
            [jcef.setup :refer [jcef-builder]]
            [jcef.resource-handlers :as rh]
            [me.raynes.fs :as fs])
  (:import [org.cef CefApp]
           [org.cef.browser CefBrowser CefMessageRouter]
           [org.cef.handler
            CefDownloadHandlerAdapter
            CefDisplayHandlerAdapter
            CefFocusHandlerAdapter
            CefLifeSpanHandlerAdapter
            CefLoadHandlerAdapter
            CefMessageRouterHandler]
           [java.awt BorderLayout Cursor GraphicsEnvironment KeyboardFocusManager Toolkit]
           [java.awt.event ActionListener ComponentAdapter WindowAdapter]
           [javax.swing JFileChooser JFrame JMenu JMenuBar JMenuItem JTextField KeyStroke SwingUtilities]
           [javax.swing.filechooser FileNameExtensionFilter]))

;;; Helpers

(defmacro cond-doto
  "Takes an Java object and set of test/form pairs.
   Performs `doto` with for each clause expression returns true."
  [expr & clauses]
  (assert (even? (count clauses))
          "cond-doto requires an even number of condition-expression pairs")
  (let [g (gensym)]
    `(let [~g ~expr]
       ~@(mapcat (fn [[clause form]]
                   `((when ~clause
                       (doto ~g ~form))))
                 (partition 2 clauses))
       ~g)))

(defn- max-screen-size
  "Obtains the max screen size."
  []
  (let [bounds (.getMaximumWindowBounds (GraphicsEnvironment/getLocalGraphicsEnvironment))]
    [(.-width bounds) (.-height bounds)]))

(defn- get-keycode [s]
  (.getKeyCode (KeyStroke/getKeyStroke s)))

(defn- ->key-shortcut [c]
  (KeyStroke/getKeyStroke (get-keycode c) (.. Toolkit (getDefaultToolkit) (getMenuShortcutKeyMask))))

(defn build-menu-bar
  "Build a JMenuBar using a collection of maps with keys:
   - `:title` - Title of the menu
   - `:items` - Vector of Menu Item maps

   Menu Item maps have keys:
   - :separator?  - If true, skips creating a Menu Item and inserts a separator
   - :label       - Label of the Menu Item
   - :description - Description for accessibility
   - :mnemonic    - Key that will automatically select the Menu Item
   - :on-select   - Function to be called when Menu Item is selected
   - :shortcut    - Optional key (along with CTRL/CMD) to trigger Menu Item"
  ([app menus]
   (let [menu-bar (JMenuBar.)]
     (build-menu-bar menu-bar app menus)))
  ([menu-bar app [{:keys [title items]} & remaining]]

   (let [menu (JMenu. title)]

     ;; Add Menu to the MenuBar
     (.add menu-bar menu)

     ;; Iterate over each Item
     (doseq [{:keys [separator? label description mnemonic on-select shortcut]} items]
       (if separator?
         (.addSeparator menu)
         ;; Create Menu Item
         (let [menu-item (JMenuItem. label)]
           (cond-doto menu-item
             mnemonic
             (.setMnemonic (get-keycode mnemonic))

             shortcut
             (.setAccelerator (->key-shortcut shortcut))

             on-select
             (.addActionListener (proxy [ActionListener] []
                                   (actionPerformed [e]
                                     (on-select {:event e :app app})))))
           (when description
             (.. menu-item
                 (getAccessibleContext)
                 (setAccessibleDescription description)))

           (.add menu menu-item))))

     (if remaining
       (build-menu-bar menu-bar remaining)
       menu-bar))))

;;; Views

(defn show-dev-tools!
  "Show the Developer Tools for a CefBrowser"
  [^CefBrowser browser]
  (let [jframe       (JFrame. "DevTools")
        content-pane (.getContentPane jframe)
        dev-tools    (.getDevTools browser)]
    (.add content-pane (.getUIComponent dev-tools) BorderLayout/CENTER)
    (SwingUtilities/invokeLater #(doto jframe
                                   (.pack)
                                   (.setSize 800 600)
                                   (.setVisible true)))))

(defn- create-popup-window! [client title url & [width height]]
  (let [width        (or width 800)
        height       (or height 600)
        browser      (.createBrowser client url false true)
        jframe       (JFrame. title)
        content-pane (.getContentPane jframe)
        screen-size  (max-screen-size)]
    (.add content-pane (.getUIComponent browser) BorderLayout/CENTER)
    (doto jframe
      (.pack)
      (.setSize width height)
      (.setLocation (- (/ (first screen-size) 2) (/ width 2))
                    (- (/ (second screen-size) 2) (/ height 2))) ; Center the frame
      (.setVisible true))
    #_(show-dev-tools! browser)))

(defn- open-new-link! [client _browser _frame target-url _target-frame-name]
  (if (re-find #"localhost.*\/print" target-url)
    (create-popup-window! client "Print Results" target-url)
    ;; Otherwise, open in the native browser
    (browse-url target-url))
  true)

(defn build-cef-app!
  "Creates a CEF app frame with the following options map:
   - `:title`         [Req.] - Title of the app.
   - `:url`           [Req.] - URL to start the browser at.
   - `:cache-path`    [Req.] - Path to User's cache.
   - `:on-close`      [Opt.] - Function to execute when the window closes.
   - `:use-osr?`      [Opt.] - Use Windowless Rendering (Default: false)
   - `:transparent?`  [Opt.] - Transparent window (Default: false)
   - `:address-bar?`  [Opt.] - Show an address bar. (Default: false)

   Returns a map with:
  - `:frame`   - `JFrame` Application
  - `:browser` - `CefBrowser`
  - `:client`  - `CefClient`"
  [{:keys [title menu url use-osr? size request-handler cache-path
           transparent? address-bar? fullscreen? dev-tools?
           on-close on-blur on-focus on-hidden on-shown on-before-launch]
    :or   {use-osr? false transparent? false address-bar? false fullscreen? false size [1024 768]}}]
  (let [builder       (jcef-builder cache-path)
        _             (set! (.-windowless_rendering_enabled (.getCefSettings builder)) use-osr?)
        cef-app       (.build builder)
        client        (.createClient cef-app)
        msg-router    (CefMessageRouter/create)
        _             (.addMessageRouter client msg-router)
        browser       (.createBrowser client url use-osr? transparent?)
        browser-ui    (.getUIComponent browser)
        address       (doto (JTextField. url 100)
                        (.addActionListener (proxy [ActionListener] []
                                              (actionPerformed [_]
                                                (.loadURL browser url)))))
        size          (if fullscreen? (max-screen-size) size)
        browser-focus (atom true)
        jframe        (JFrame. title)
        content-pane  (.getContentPane jframe)
        app           {:browser      browser
                       :browser-ui   browser-ui
                       :cef-app      cef-app
                       :client       client
                       :content-pane content-pane
                       :frame        jframe
                       :msg-router   msg-router}
        menu-bar      (when menu (build-menu-bar app menu))]

    (when dev-tools?
      (show-dev-tools! browser))

    (when request-handler
      (.addRequestHandler client request-handler))


    (doto client 
      (.addDownloadHandler (proxy [CefDownloadHandlerAdapter] []
                             (onBeforeDownload​ [& args]
                               (println [:DOWNLOAD args])
                               (let [[callback filename] (reverse args)
                                     suggested-filename (str (fs/file (fs/home) (fs/base-name filename)))]
                                 (.Continue callback suggested-filename true)))))

      (.addDisplayHandler (proxy [CefDisplayHandlerAdapter] []
                            (onAddressChange [_ _ url]
                              (.setText address url))

                            (onCursorChange [browser cursorType]
                              (.. browser
                                  (getUIComponent)
                                  (setCursor (Cursor/getPredefinedCursor cursorType)))
                              false)))

      (.addFocusHandler (proxy [CefFocusHandlerAdapter] []
                          (onGotFocus [& args]
                            (when (fn? on-focus) (apply on-focus app args))
                            (when-not @browser-focus
                              (reset! browser-focus true)
                              (.clearGlobalFocusOwner (KeyboardFocusManager/getCurrentKeyboardFocusManager))
                              (.setFocus browser true)))
                          (onTakeFocus [& args]
                            (when (fn? on-blur) (apply on-blur app args))
                            (reset! browser-focus false))))

      (.addLifeSpanHandler (proxy [CefLifeSpanHandlerAdapter] []
                             (onBeforePopup [& args]
                               (apply open-new-link! client args)))))

    (.addComponentListener jframe (proxy [ComponentAdapter] []
                                    (componentHidden [& args]
                                      (when (fn? on-hidden)
                                        (apply on-hidden app args)))
                                    (componentShown [& args]
                                      (when (fn? on-shown)
                                        (apply on-shown app args)))))

    (when (fn? on-before-launch)
      (on-before-launch app))

    (if address-bar?
      (doto content-pane
        (.add address BorderLayout/NORTH)
        (.add browser-ui BorderLayout/CENTER))
      (.add content-pane browser-ui BorderLayout/CENTER))

    (when menu-bar
      (.setJMenuBar jframe menu-bar))

    (doto jframe
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE) ;; Exit on Close
      #_(.setUndecorated true) ;; Remove title bar
      (.pack)
      (.setSize (first size) (second size))
      (.setVisible true)
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowClosing [_]
                              (when (fn? on-close) (on-close))
                              (.dispose (CefApp/getInstance))))))
    app))

(defn create-cef-app!
  "Wrap builder in `invokeLater` to ensure it executes on separate thread."
  [& args]
  (SwingUtilities/invokeLater #(apply build-cef-app! args)))

(defn- open-file-chooser [callback frame title & extensions]
  (let [file-filter (FileNameExtensionFilter. title (into-array String extensions))
        chooser     (JFileChooser.)]
  (.setFileFilter chooser file-filter)
  (let [return-val (.showOpenDialog chooser frame)]
    (when (= return-val JFileChooser/APPROVE_OPTION)
      (callback (.getSelectedFile chooser))))))

(defn- open-save-file [callback frame title & extensions]
  (let [file-filter (FileNameExtensionFilter. title (into-array String extensions))
        chooser     (JFileChooser.)]
    (.setFileFilter chooser file-filter)
    (let [return-val (.showSaveDialog chooser frame)]
      (when (= return-val JFileChooser/APPROVE_OPTION)
        (callback (.getSelectedFile chooser))))))


(comment
  (require '[config.interface :refer [get-config]])
  (require '[behave.server :refer [init-config! init-db!]])
  (require '[behave.handlers :refer [create-cef-handler-stack]])
  (require '[jcef.loading :refer [show-loader!]])
  (require '[clojure.java.io :as io])

  (init-config!)
  (init-db! (get-config :database :config))

  (def local-req-handler (rh/custom-request-handler
                          {:protocol     "http"
                           :authority    "localhost:4242" 
                           :resource-dir "public"
                           :ring-handler (create-cef-handler-stack)}))

  (def loader (show-loader! "Behave7" (io/resource "public/images/android-chrome-512x512.png")))

  (def app (atom nil))

  (create-cef-app!
   {:title           "Behave-Dev"
    :url             "http://localhost:4242/"
    :fullscreen?     true
    :cache-path      (str (io/file fs/*cwd* ".cache"))
    :on-shown        (fn [& _args] (println "Howdy there!")
                       (reset! app (first _args))
                       (.dispose (:frame loader)))
    :on-hidden       (fn [& _args] (println "Goodbye!"))
    :request-handler local-req-handler})

  (show-dev-tools! (:browser @app))

  (.removeDownloadHandler (:client @app))
  (.addDownloadHandler (:client @app)
                       (proxy [CefDownloadHandlerAdapter] []
                         (onBeforeDownload​ [& args]
                           (println [:DOWNLOAD args])
                           #_(let [[callback filename] (reverse args)
                                 suggested-filename  (str (fs/file (fs/home) (fs/base-name filename)))]
                             (.Continue callback suggested-filename true)))))

  (.loadURL (:browser @app) "http:/https://drive.google.com/drive/folders/1zfMDKek6-c7f0G9SZ3OhtG98_mFntyV-")

  (.startDownload (:browser @app) "https://github.com/jcefmaven/jcefmaven/releases/download/135.0.20/jcefmaven-135.0.20-sources.jar")

  (.removeMessageRouter (:client @app) msg-router)
  (def msg-router
    (CefMessageRouter/create
     (proxy [CefMessageRouterHandler] []
       (onQuery [browser frame query-id request persistent? callback]
         (println [:QUERY browser frame query-id request persistent? callback])
         (let [client (.getClient browser)]
           (print-to-pdf client request))
         true))))
  (.addMessageRouter (:client @app) msg-router)


  (.removeLifeSpanHandler (:client @app))
  (.addLifeSpanHandler (:client @app)
                       (proxy [CefLifeSpanHandlerAdapter] []
                         (onBeforePopup [& args]
                           (apply open-new-link! (:client @app) args)
                           true)))



  )
