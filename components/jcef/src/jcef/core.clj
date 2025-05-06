(ns jcef.core
  (:require [jcef.setup :refer [jcef-builder]])
  (:import [me.friwi.jcefmaven MavenCefAppHandlerAdapter]
           [org.cef CefApp CefApp$CefAppState]
           [org.cef.browser CefBrowser CefFrame CefMessageRouter]
           [org.cef.handler CefDisplayHandlerAdapter CefFocusHandlerAdapter]
           [java.awt BorderLayout Component KeyboardFocusManager]
           [java.awt.event ActionListener WindowAdapter]
           [javax.swing JFrame JTextField]))

(defn create-cef-app!
  "Creates a CEF app frame with the following options map:
   - `:title`           [Req.] - Title of the app.
   - `:url`             [Req.] - URL to start the browser at.
   - `:on-close`        [Opt.] - Function to execute when the window closes.
   - `:use-osr?`        [Opt.] - Use Windowless Rendering (Default: false)
   - `:is-transparent?` [Opt.] - Transparent window (Default: false)
   - `:address-bar?`    [Opt.] - Show an address bar. (Default: false)

   Returns a map with:
  - `:app`     - `JFrame` Application
  - `:browser` - `CefBrowser`
  - `:client`  - `CefClient`"
  [{:keys [title url use-osr is-transparent address-bar? on-close on-blur]
    :or   {use-osr false is-transparent false address-bar? false}}]
  (let [builder       (jcef-builder)
        _             (set! (.-windowless_rendering_enabled (.getCefSettings builder)) use-osr)
        _             (.setAppHandler builder
                                      (proxy [MavenCefAppHandlerAdapter] []
                                        (stateHasChanged [state]
                                          (when (= state CefApp$CefAppState/TERMINATED)
                                            (System/exit 0)))))
        cef-app       (.build builder)
        client        (.createClient cef-app)
        msg-router    (CefMessageRouter/create)
        _             (.addMessageRouter client msg-router)
        browser       (.createBrowser client url use-osr is-transparent)
        browser-ui    (.getUIComponent browser)
        address       (doto (JTextField. url 100)
                        (.addActionListener (proxy [ActionListener] []
                                              (actionPerformed [_]
                                                (.loadURL browser url)))))
        browser-focus (atom true)
        jframe        (JFrame. title)
        content-pane  (.getContentPane jframe)
        result        {:frame   jframe
                       :browser browser
                       :client  client}]

    (.addDisplayHandler client (proxy [CefDisplayHandlerAdapter] []
                                 (onAddressChange [_ _ url]
                                   (.setText address url))))

    (.addFocusHandler client (proxy [CefFocusHandlerAdapter] []
                               (onGotFocus [_]
                                 (when-not @browser-focus
                                   (reset! browser-focus true)
                                   (.clearGlobalFocusOwner (KeyboardFocusManager/getCurrentKeyboardFocusManager))
                                   (.setFocus browser true)))
                               (onTakeFocus [_ _]
                                 (when (fn? on-blur) (on-blur))
                                 (reset! browser-focus false))))
    
    (if address-bar?
      (doto content-pane
        (.add address BorderLayout/NORTH)
        (.add browser-ui BorderLayout/CENTER))
      (.add content-pane browser-ui BorderLayout/CENTER))

    (doto jframe
      (.pack)
      (.setSize 1024 768)
      (.setVisible true)
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowClosing [_]
                              (when (fn? on-close) (on-close))
                              (.dispose (CefApp/getInstance))))))
    result))
