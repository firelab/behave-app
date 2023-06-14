(ns behave-cms.socket)

(defonce socket (atom nil))

(defn connect! [{:keys [on-open on-close on-message]}]
  (let [host   (.-host js/location)]
    (reset! socket (js/WebSocket. (str "ws://" host "/happiness")))
    (when on-open (set! (.-onopen @socket) on-open))
    (when on-close (set! (.-onclose @socket) on-close))
    (when on-message (set! (.-onmessage @socket) on-message))))

(defn send! [message]
  (.send @socket message))
