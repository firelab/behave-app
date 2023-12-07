(ns browser-utils.core
  (:require [cljs.reader    :as edn]
            [clojure.string :as str])
  (:import  [goog.async Debouncer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Functions - Browser Session
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce ^:private session-key (atom nil))

(defn- save-session-storage! [data]
  (.setItem (.-sessionStorage js/window) @session-key (pr-str data)))

(defn get-session-storage
  "Gets the pyregence session storage data."
  []
  (edn/read-string (.getItem (.-sessionStorage js/window) @session-key)))

(defn set-session-storage!
  "Sets the pyregence session storage given data to store."
  [data]
  (save-session-storage! (merge (get-session-storage) data)))

(defn remove-session-storage!
  "Removes the specified pyregence session storage data given keywords."
  [& keys]
  (let [data (get-session-storage)]
    (save-session-storage! (apply dissoc data keys))))

(defn clear-session-storage!
  "Clears the pyregence session storage data."
  []
  (save-session-storage! {}))

(defn create-session-storage!
  "Creates a session storage with key `k`."
  [k]
  (reset! session-key k)
  (clear-session-storage!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Functions - Local Storage
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce ^:private local-key (atom nil))

(defn- save-local-storage! [data]
  (.setItem (.-localStorage js/window) @local-key (pr-str data)))

(defn get-local-storage
  "Gets the pyregence local storage data."
  []
  (edn/read-string (.getItem (.-localStorage js/window) @local-key)))

(defn set-local-storage!
  "Sets the pyregence local storage given data to store."
  [data]
  (save-local-storage! (merge (get-local-storage) data)))

(defn assoc-in-local-storage!
  "Updates local storage given a path to where the given value shuld be set."
  [path value]
  (save-local-storage! (assoc-in (get-local-storage) path value)))

(defn remove-local-storage!
  "Removes the specified pyregence local storage data given keywords."
  [& keys]
  (let [data (get-local-storage)]
    (save-local-storage! (apply dissoc data keys))))

(defn clear-local-storage!
  "Clears the pyregence local storage data."
  []
  (save-local-storage! {}))

(defn create-local-storage!
  "Creates a local storage with key `k`."
  [k]
  (reset! local-key k)
  (clear-local-storage!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Functions - Browser Management
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn jump-to-url!
  "Redirects the current window to the given URL."
  ([url]
   (let [origin  (.-origin (.-location js/window))
         cur-url (str/replace (.-href (.-location js/window)) origin "")]
     (when-not (= cur-url url) (set! (.-location js/window) url))))
  ([url window-name]
   (if window-name
     (.open js/window url window-name)
     (jump-to-url! url))))

(defn redirect-to-login! [from-page]
  (set-session-storage! {:redirect-from from-page})
  (jump-to-url! "/login"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Functions - Debouncers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debounce [f interval]
  (let [js-f (fn [& args] (apply f (js->clj args)))
        dbnc (Debouncer. js-f interval)]
    ;; We use apply here to support functions of various arities
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility Functions - Add dynamic script
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-script [js-path]
  (let [script-el (.createElement js/document "script")]
    (set! (.-src script-el) js-path)
    (set! (.-type script-el) "text/javascript")
    (-> js/document
        (.-body)
        (.appendChild script-el))))

(defn script-exist? [js-path]
  (->> (js/document.getElementsByTagName "script")
       (js/Array.from)
       (filter #(= js-path (.-src %)))
       (count)
       (pos?)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Download
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn download
  "Function to download data to a file"
  [data filename type]
  (let [body (.-body js/document)
        file (js/Blob. #js [data] #js {:type type})
        a    (.createElement js/document "a")
        url  (.createObjectURL js/URL file)]
    (set! (.-href a) url)
    (set! (.-download a) filename)
    (.appendChild body a)
    (.click a)
    (js/setTimeout
     #(do (.removeChild body a)
          (js/URL.revokeObjectURL url)) 0)))
;; Usage
#_(download (str/join "\n" ["Header1,Header2" "1,2"])
            "example.csv"
            "application/text")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Format International Number
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn format-intl-number [locale number & [significant-digits]]
  (.format (js/Intl.NumberFormat. locale #js {:maximumSignificantDigits (or significant-digits 3)})
           number))

;; Usage
;; (format-intl-number "en-US" 0.09393923 2) ; => 0.09
