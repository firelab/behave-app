(ns jcef.interface
  (:require [jcef.core :as c]
            [jcef.resource-handlers :as rh]
            [jcef.loading :as l]))

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

(def ^{:arglists '([options])
       :doc      "Creates a custom request/response handler for CEF browser."}
  custom-request-handler rh/custom-request-handler)

(def ^{:arglists '([project-name icon-url])
       :doc "Displays a loader centered on the screen. Takes:
             - `project`  - Project Name to display on the loading screen
             - `icon-url` - Icon display on the loading screen, at least 256x256 px.
                            This can use a local resource URL (e.g. `(io/resource <path-to-icon>)`).

             Returns a map with keys:
              - `:frame`    - JFrame, which should be disposed with `(.dispose frame)`
              - `:progress` - JProgressBar, which can be updated with
                              `(.setValue progress-bar <value>)`"}
  show-loader! l/show-loader!)
