(ns behave.windows
  (:import [org.cef CefApp]))

(defonce ^:private *windows (atom {}))

(defn register-window!
  "Registers a window in the registry."
  [window-id window-state]
  (swap! *windows assoc window-id window-state)
  (.openDevTools (get-in window-state [:app :browser])))

(defn deregister-window!
  "Deregisters a window. If no windows remain, disposes CefApp and exits."
  [window-id]
  (let [remaining (swap! *windows dissoc window-id)]
    (when (empty? remaining)
      (.dispose (CefApp/getInstance))
      (.exit (Runtime/getRuntime) 0))))

(defn window-count
  "Returns the number of open windows."
  []
  (count @*windows))


(comment

    (map #(.openDevTools (get-in % [:app :browser])) (vals @*windows))


  )
