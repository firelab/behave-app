(ns jcef.interface
  (:require [jcef.core :as c]))

(def ^{:arglists '([options])
       :doc      "Creates a CEF app frame with the following options map:
             - `:title`           [Req.] - Title of the app.
             - `:url`             [Req.] - URL to start the browser at.
             - `:on-close`        [Opt.] - Function to execute when the window closes.
             - `:use-osr?`        [Opt.] - Use Windowless Rendering (Default: false)
             - `:is-transparent?` [Opt.] - Transparent window (Default: false)
             - `:address-bar?`    [Opt.] - Show an address bar. (Default: false)

             Returns a map with:
             - `:app`     - `JFrame` Application
             - `:browser` - `CefBrowser`
             - `:client`  - `CefClient`"}

  create-cef-app! c/create-cef-app!)
