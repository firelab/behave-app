(ns jcef.core
  (:require [jcef.setup :refer [jcef-builder]]
            [jcef.resource-handlers :as rh])
  (:import [org.cef CefApp]
           [org.cef.browser CefBrowser CefMessageRouter]
           [org.cef.handler CefDisplayHandlerAdapter CefFocusHandlerAdapter]
           [java.awt BorderLayout Cursor GraphicsEnvironment KeyboardFocusManager Toolkit]
           [java.awt.event ActionEvent ActionListener WindowAdapter]
           [javax.swing JFileChooser JFrame JMenu JMenuBar JMenuItem JTextField KeyStroke SwingUtilities]
           [javax.swing.filechooser FileNameExtensionFilter]))

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

(defn build-cef-app!
  "Creates a CEF app frame with the following options map:
   - `:title`         [Req.] - Title of the app.
   - `:url`           [Req.] - URL to start the browser at.
   - `:on-close`      [Opt.] - Function to execute when the window closes.
   - `:use-osr?`      [Opt.] - Use Windowless Rendering (Default: false)
   - `:transparent?`  [Opt.] - Transparent window (Default: false)
   - `:address-bar?`  [Opt.] - Show an address bar. (Default: false)

   Returns a map with:
  - `:frame`   - `JFrame` Application
  - `:browser` - `CefBrowser`
  - `:client`  - `CefClient`"
  [{:keys [title menu url use-osr transparent? address-bar? fullscreen? dev-tools? size on-close on-blur on-before-launch]
    :or   {use-osr false transparent? false address-bar? false fullscreen? false size [1024 768]}}]
  (let [builder       (jcef-builder)
        _             (set! (.-windowless_rendering_enabled (.getCefSettings builder)) use-osr)
        cef-app       (.build builder)
        client        (.createClient cef-app)
        msg-router    (CefMessageRouter/create)
        _             (.addMessageRouter client msg-router)
        browser       (.createBrowser client url use-osr transparent?)
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

    (.addDisplayHandler client (proxy [CefDisplayHandlerAdapter] []
                                 (onAddressChange [_ _ url]
                                   (.setText address url))

                                 (onCursorChange [browser cursorType]
                                   (.. browser
                                       (getUIComponent)
                                       (setCursor (Cursor/getPredefinedCursor cursorType)))
                                   false)))

    (.addFocusHandler client (proxy [CefFocusHandlerAdapter] []
                               (onGotFocus [_]
                                 (when-not @browser-focus
                                   (reset! browser-focus true)
                                   (.clearGlobalFocusOwner (KeyboardFocusManager/getCurrentKeyboardFocusManager))
                                   (.setFocus browser true)))
                               (onTakeFocus [_ _]
                                 (when (fn? on-blur) (on-blur))
                                 (reset! browser-focus false))))

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
  (require '[config.interface :refer [get-config load-config]])
  (require '[behave.core :refer [init-config! init-db! create-api-handler-stack]])
  (init-config!)
  (init-db! (get-config :database))

  (def app 
    (build-cef-app!
     {:title       "Behave-Dev"
      :url         "http://localhost:4242/"
      :fullscreen? true}))

  (show-dev-tools! (:browser app))
  (:client app)
  (.removeRequestHandler (:client app))
  (def local-req-handler (rh/custom-request-handler
                          {:protocol     "http"
                           :authority    "localhost:4242" 
                           :resource-dir "public"
                           :ring-handler (create-api-handler-stack)}))
  (.addRequestHandler (:client app) local-req-handler)
  #_(.loadURL (:browser app) "http://localhost:4242/")
  (.reloadIgnoreCache (:browser app))
  #_(.reload (:browser app))

  

  (println "hi")

  )
