(ns jcef.interface
  (:require [jcef.core :as c]
            [jcef.resource-handlers :as rh]
            [jcef.loading :as l]))

(def ^{:arglists '([options])
       :doc      "Initializes the CefApp singleton. Idempotent — returns the cached
             instance on subsequent calls. Takes an options map:
             - `:use-osr?`           - Use Windowless Rendering (Default: false)
             - `:cache-path`         - Path for browser cache
             - `:remote-debug-port`  - Port for remote debugging"}
  init-cef-app! c/init-cef-app!)

(def ^{:arglists '([cef-app options])
       :doc      "Creates a new JCEF browser window from an existing CefApp instance.
             See `jcef.core/open-window!` for the full options map."}
  open-window! c/open-window!)

(def ^{:arglists '([options])
       :doc      "Creates a CEF app frame. Backward-compatible wrapper around
             `init-cef-app!` and `open-window!`."}
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

(def ^{:arglists '([browser])
       :doc "Show the Developer Tools for a CefBrowser"}
  show-dev-tools! c/show-dev-tools!)
