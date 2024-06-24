(ns behave-cms.utils
  (:require [cljs.reader        :as edn]
            [clojure.string     :as str]
            [clojure.set        :as sets]
            [applied-science.js-interop :as j]
            [clojure.core.async :refer [alts! go <! timeout go-loop chan put!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [reagent.ratom      :refer [RAtom Reaction]])
  (:import  [goog.async Debouncer]))

;; JS Utils
(defn debounce [f interval]
  (let [dbnc (Debouncer. f interval)]
    ;; We use apply here to support functions of various arities
    (fn [& args] (.apply (.-fire dbnc) dbnc (to-array args)))))

;; Forms

(defn input-value
  "Returns the value property of the target property of an event."
  [event]
  (-> event .-target .-value))

(defn input-multi-select
  "Returns the values from a multi-select dropdown."
  [event]
  (let [options (-> event .-target .-options)]
    (reduce (fn [acc option]
              (if (.-selected option)
                (conj acc (or (.-value option) (.-text option)))
                acc))
            []
            options)))

(defn input-int-value
  "Given an event, returns the value as an integer."
  [event]
  (js/parseInt (input-value event)))

(defn input-float-value
  "Given an event, returns the value as a float."
  [event]
  (js/parseFloat (input-value event)))

(defn input-keyword
  "Given an event, returns the value as a Clojure keyword."
  [event]
  (keyword (input-value event)))

(defn input-file
  "Returns the file of the target property of an event."
  [event]
  (-> event .-target .-files (aget 0)))

(defn on-submit
  "Prevents the submit event from bubbling up, executes `f`."
  [f]
  (fn [event]
    (.preventDefault event)
    (.stopPropagation event)
    (f)))

(defn cursor-location
  "Returns the start/end of the selection of the cursor inside a textbox."
  [event]
  (let [input (j/get event :target)
        start (j/get input :selectionStart)
        end   (j/get input :selectionEnd)]
    [start end]))

;;; Uploading Images

(defn- get-files [data-tx]
  (if-let [items (.-items data-tx)]
    (keep #(when (= (.-kind %) "file")
        (.getAsFile %)) items)
    (.-files data-tx)))

(defn on-drop-image [event f]
   ; Prevent default behavior (Prevent file from being opened)
   (.preventDefault event)
   (let [files (get-files (j/get event :dataTransfer))
         file  (first files)]
     (when (str/starts-with? (j/get file :type) "image/")
       (f file))))

(defn file->url [file callback]
  (let [reader  (js/FileReader.)
        on-load #(callback (j/get reader :result))]
    (.addEventListener reader "load" on-load)
    (.readAsDataURL reader file)))

(defn file->blob [file callback]
  (let [reader  (js/FileReader.)
        on-load #(callback (j/get reader :result))]
    (.addEventListener reader "load" on-load)
    (.readAsArrayBuffer reader file)))

(defn on-select-image [e f & [convert-to convert-callback]]
  (let [file (first (array-seq (j/get-in e [:target :files])))]
    (f file)
    (when convert-to
      (if (= type :blob)
        (file->blob file convert-callback)
        (file->url file convert-callback)))))

;;; Text

(defn sentence->kebab
  "Converts a string to a kebab-case string."
  [string]
  (-> string
      (str/lower-case)
      (str/replace #"[\s-\.\,]+" "-")))

(defn snake->kebab
  "Converts a snake_case string to a kebab-case string."
  [s]
  (str/replace s #"_" "-"))

(defn end-with
  "Appends `end` to `s` as long as `s` doesn't already end with `end`."
  [s end]
  (str s
       (when-not (str/ends-with? s end)
         end)))

;;; Session

(def session-key "behaveplus")

(defn- save-session-storage! [data]
  (.setItem (.-sessionStorage js/window) session-key (pr-str data)))

(defn get-session-storage
  "Gets the session storage data."
  []
  (edn/read-string (.getItem (.-sessionStorage js/window) session-key)))

(defn set-session-storage!
  "Sets the session storage given data to store."
  [data]
  (save-session-storage! (merge (get-session-storage) data)))

(defn remove-session-storage!
  "Removes the specified session storage data given keywords."
  [& keys]
  (let [data (get-session-storage)]
    (save-session-storage! (apply dissoc data keys))))

(defn clear-session-storage!
  "Clears the session storage data."
  []
  (save-session-storage! {}))

;;; Local Storage

(defn- save-local-storage! [data]
  (.setItem (.-localStorage js/window) session-key (pr-str data)))

(defn get-local-storage
  "Gets the pyregence local storage data."
  []
  (edn/read-string (.getItem (.-localStorage js/window) session-key)))

(defn set-local-storage!
  "Sets the pyregence local storage given data to store."
  [data]
  (save-local-storage! (merge (get-local-storage) data)))

(defn remove-local-storage!
  "Removes the specified pyregence local storage data given keywords."
  [& keys]
  (let [data (get-local-storage)]
    (save-local-storage! (apply dissoc data keys))))

(defn clear-local-storage!
  "Clears the pyregence local storage data."
  []
  (save-local-storage! {}))

;;; Browser Management

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

;;; Fetch results

(defn- chan? [c]
  (= (type c) cljs.core.async.impl.channels/ManyToManyChannel))

(defn- promise? [p]
  (instance? js/Promise p))

(defn fetch
  "Launches a js/window.fetch operation. Returns a channel that will
  receive the response or nil if a network error occurs. The options
  map will be automatically converted to a JS object for the fetch
  call."
  ([url]
   (fetch url {}))
  ([url options]
   (go
     (try
       (<p! (.fetch js/window url (clj->js options)))
       (catch ExceptionInfo e (js/console.log "Network Error:" (ex-cause e)))))))

(defn fetch-and-process
  "Launches a js/window.fetch operation and runs process-fn on the
  successful result. HTTP Errors and Network Errors raised by the
  fetch are printed to the console. The options map will be
  automatically converted to a JS object for the fetch call. Returns a
  channel with the result of process-fn. If process-fn returns a
  channel or promise, these will be taken from using <! or <p!
  respectively."
  [url options process-fn]
  (go
    (when-let [response (<! (fetch url options))]
      (if (.-ok response)
        (try
          (let [result (process-fn response)]
            (cond (chan? result)    (<! result)
                  (promise? result) (<p! result)
                  :else             result))
          (catch ExceptionInfo e (js/console.log "Error in process-fn:" (ex-cause e))))
        (js/console.log "HTTP Error:" response)))))

(defmulti call-remote! (fn [method _ _] method))

(defmethod call-remote! :get [_ url data]
  (go
    #_(println "-- CALL REMOTE " url data)
    (let [query-string (->> data
                            (map (fn [[k v]] (str (pr-str k) "=" (pr-str v))))
                            (str/join "&")
                            (js/encodeURIComponent))
          fetch-params {:method  "get"
                        :headers {"Accept" "application/edn"
                                  "Content-Type" "application/edn"}}
          response     (<! (fetch (str url (when (not= query-string "") (str "?" query-string)))
                                  fetch-params))]
      (if response
        {:success (.-ok response)
         :status  (.-status response)
         :body    (or (edn/read-string (<p! (.text response))) "")}
        {:success false
         :status  nil
         :body    ""}))))

;; Combines status and error message into return value
(defmethod call-remote! :post [_ url data]
  (go
    #_(println "-- CALL REMOTE " url data)
    (let [fetch-params {:method  "post"
                        :headers (merge {"Accept" "application/edn"}
                                        (when-not (= (type data) js/FormData)
                                          {"Content-Type" "application/edn"}))
                        :body    (cond
                                   (= js/FormData (type data)) data
                                   data                        (pr-str data)
                                   :else                       nil)}
          response     (<! (fetch url fetch-params))]
      (if response
        {:success (.-ok response)
         :status  (.-status response)
         :body    (or (edn/read-string (<p! (.text response))) "")}
        {:success false
         :status  nil
         :body    ""}))))

;; Combines status and error message into return value
(defmethod call-remote! :put [_ url data]
  (go
    (let [fetch-params {:method  "put"
                        :headers (merge {"Accept" "application/edn"}
                                        (when-not (= (type data) js/FormData)
                                          {"Content-Type" "application/edn"}))
                        :body    (cond
                                   (= js/FormData (type data)) data
                                   data                        (pr-str data)
                                   :else                       nil)}
          response     (<! (fetch url fetch-params))]
      (if response
        {:success (.-ok response)
         :status  (.-status response)
         :body    (or (edn/read-string (<p! (.text response))) "")}
        {:success false
         :status  nil
         :body    ""}))))

;; Combines status and error message into return value
(defmethod call-remote! :delete [_ url data]
  (go
    (let [fetch-params {:method  "delete"
                        :headers (merge {"Accept" "application/edn"}
                                        (when-not (= (type data) js/FormData)
                                          {"Content-Type" "application/edn"}))
                        :body    (cond
                                   (= js/FormData (type data)) data
                                   data                        (pr-str data)
                                   :else                       nil)}
          response     (<! (fetch url fetch-params))]
      (if response
        {:success (.-ok response)
         :status  (.-status response)
         :body    (or (edn/read-string (<p! (.text response))) "")}
        {:success false
         :status  nil
         :body    ""}))))

(defmethod call-remote! :post-text [_ url data]
  (go
    (let [fetch-params {:method  "post"
                        :headers (merge {"Accept" "application/transit+json"}
                                        (when-not (= (type data) js/FormData)
                                          {"Content-Type" "application/edn"}))
                        :body    (cond
                                   (= js/FormData (type data)) data
                                   data                        (pr-str data)
                                   :else                       nil)}
          response     (<! (fetch (str url "?auth-token=883kljlsl36dnll9s9l2ls8xksl")
                                  fetch-params))]
      (if response
        {:success (.-ok response)
         :status  (.-status response)
         :body    (or (<p! (.text response)) "")}
        {:success false
         :status  nil
         :body    ""}))))

(defmethod call-remote! :post-blob [_ url data]
  (go
    (let [fetch-params {:method  "post"
                        :headers (merge {"Accept" "application/transit+json"}
                                        (when-not (= (type data) js/FormData)
                                          {"Content-Type" "application/edn"}))
                        :body    (cond
                                   (= js/FormData (type data)) data
                                   data                        (pr-str data)
                                   :else                       nil)}
          response     (<! (fetch (str url "?auth-token=883kljlsl36dnll9s9l2ls8xksl")
                                  fetch-params))]
      (if response
        {:success (.-ok response)
         :status  (.-status response)
         :body    (or (<p! (.blob response)) "")}
        {:success false
         :status  nil
         :body    ""}))))

(defmethod call-remote! :default [method _ _]
  (throw (ex-info (str "No such method (" method ") defined for pyregence.utils/call-remote!") {})))

;; TODO This whole routing should be more generic
(def ^:private post-options #{:get :post :post-text :post-blob})

(defn call-clj-async!
  "Calls a given function from the backend and returns a go block
   containing the function's response.

  Example:
  ```clojure
  ;; POST 'app-url.com/clj/command' with `[arg1 arg2]` in edn format.
  (let [res (<! (call-clj-async! \"command\" :get arg1 arg2))]
    (if (= 200 (:status res))
        (on-success (-> res (:body) (edn/read-string)))
        (on-error res)))
  ```"
  [clj-fn-name & args]
  (let [first-arg (first args)
        method    (or (post-options first-arg) :post-text)
        data      (cond
                    (= js/FormData (type first-arg))
                    first-arg

                    (= :post-text method)
                    {:clj-args args}

                    :else
                    {:clj-args (rest args)})]
    (call-remote! method
                  (str "/clj/" clj-fn-name)
                  data)))

;;; Process Returned Results

(def sql-primitive (comp val first first))

;;; Time processing

(defn pad-zero
  "Adds a zero in front of a given number if it's a single digit (eg. turn 2 into 02)."
  [num]
  (let [num-str (str num)]
    (if (= 2 (count num-str))
      num-str
      (str "0" num-str))))

(defn- get-time-zone
  "Returns the string code for the local timezone."
  [js-date]
  (-> js-date
      (.toLocaleTimeString "en-us" #js {:timeZoneName "short"})
      (str/split " ")
      (peek)))

(defn get-date-from-js
  "Formats the date portion of JS Date as the date portion of an ISO string."
  [js-date show-utc?]
  (if show-utc?
    (subs (.toISOString js-date) 0 10)
    (str (.getFullYear js-date)
         "-"
         (pad-zero (+ 1 (.getMonth js-date)))
         "-"
         (pad-zero (.getDate js-date)))))

(defn get-time-from-js
  "Formats the time portion of JS Date as the time portion of an ISO string."
  [js-date show-utc?]
  (if show-utc?
    (str (subs (.toISOString js-date) 11 16) " UTC")
    (str (pad-zero (.getHours js-date))
         ":"
         (pad-zero (.getMinutes js-date))
         " "
         (get-time-zone js-date))))

(defn- model-format->js-format
  "Formats date string given from GeoServer to one that can be used by JS Date."
  [date-str]
  (let [minutes (subs date-str 11 13)]
    (str (subs date-str 0 4) "-"
         (subs date-str 4 6) "-"
         (subs date-str 6 8) "T"
         (subs date-str 9 11) ":"
         (if (= 2 (count minutes)) minutes "00")
         ":00.000Z")))

(defn js-date-from-string
  "Converts a date string to a JS Date object."
  [date-str]
  (js/Date. (if (re-matches #"\d{8}_\d{2,6}" date-str)
              (model-format->js-format date-str)
              date-str)))

(defn js-date->iso-string
  "Returns a ISO date-time string for a given JS date object in local or UTC timezone."
  [js-date show-utc?]
  (str (get-date-from-js js-date show-utc?) " " (get-time-from-js js-date show-utc?)))

(defn time-zone-iso-date
  "Returns a ISO date-time string for a given date string in local or UTC timezone."
  [date-str show-utc?]
  (js-date->iso-string (js-date-from-string date-str) show-utc?))

(defn ms->hhmmss
  "Converts milliseconds into 'hours:minutes:seconds'."
  [ms]
  (let [sec     (/ ms 1000)
        hours   (js/Math.round (/ sec 3600))
        minutes (js/Math.round (/ (mod sec 3600) 60))
        seconds (js/Math.round (mod (mod sec 3600) 60))]
    (str (pad-zero hours)
         ":"
         (pad-zero minutes)
         ":"
         (pad-zero seconds))))

(defn ms->hr
  "Converts milliseconds to hours."
  [ms]
  (/ ms (* 1000 60 60)))

(defn current-date-ms
  "Returns the current date in milliseconds, with hour/minute/seconds/ms set to 0"
  []
  (-> (js/Date.)
      (.setHours 0 0 0 0)))

(defn current-timezone-shortcode
  "Returns the shortcode for the current timezone (e.g. PDT, EST)"
  []
  (-> (js/Date.)
      (.toLocaleTimeString "en-us" #js{:timeZoneName "short"})
      (.split " ")
      (last)))

(defn format-date
  "Formats a JS Date into MM/DD/YYYY"
  [js-date]
  (str (+ 1 (.getMonth js-date)) "/" (.getDate js-date) "/" (.getFullYear js-date)))

(defn camera-time->js-date
  "Converts a time from the cameras API (YYYY-MM-DD HH:MM:SS.MFS) into a JS Date in UTC."
  [camera-time]
  (js/Date. (as-> camera-time %
              (str/split % #" ")
              (interpose "T" %)
              (concat % "Z")
              (apply str %))))

(defn get-time-difference
  "Returns the difference in milliseconds between a JS Date object and the current time.
   Optionally returns the difference between two different JS Date objects."
  [js-date & [js-date-opt]]
  (if (some? js-date-opt)
    (- (.getTime js-date) (.getTime js-date-opt))
    (- (.getTime (js/Date.)) (.getTime js-date))))

;;; ->map HOF

(defn mapm
  "A version of `map` that uses transients."
  [f coll]
  (persistent!
   (reduce (fn [acc cur]
             (conj! acc (f cur)))
           (transient {})
           coll)))

(defn filterm
  "A version of `filter` that uses transients."
  [f coll]
  (persistent!
   (reduce (fn [acc cur]
             (if (f cur)
               (conj! acc cur)
               acc))
           (transient {})
           coll)))

;;; Keyword manipulation

(defn kebab-kw
  "Converts a :snake_case keyword to :kebab-case"
  [k]
  (-> k
      (name)
      (snake->kebab)
      (keyword)))

;;; Map manipulation

(defn kebab-map
  "Converts map keywords to kebab-case."
  [m]
  (reduce (fn [acc [k v]] (assoc acc (kebab-kw k) v)) {} m))

;;; Colors

(defn- to-hex-str [num]
  (let [hex-num (.toString (.round js/Math num) 16)]
    (if (= 2 (count hex-num))
      hex-num
      (str "0" hex-num))))

(defn interp-color
  "Returns a hex-code representing the interpolation of
   two provided colors and an interpolation ratio."
  [from to ratio]
  (when (and from to)
    (let [fr (js/parseInt (subs from 1 3) 16)
          fg (js/parseInt (subs from 3 5) 16)
          fb (js/parseInt (subs from 5 7) 16)
          tr (js/parseInt (subs to   1 3) 16)
          tg (js/parseInt (subs to   3 5) 16)
          tb (js/parseInt (subs to   5 7) 16)]
      (str "#"
           (to-hex-str (+ fr (* ratio (- tr fr))))
           (to-hex-str (+ fg (* ratio (- tg fg))))
           (to-hex-str (+ fb (* ratio (- tb fb))))))))

;;; Misc Functions

(defn- no-data? [x]
  (or
   (and (number? x) (.isNaN js/Number x))
   (and (string? x)
        (re-matches #"\d{4,}-\d{2}-\d{2}" x)
        (not (< 1990 (js/parseInt (first (str/split x #"-"))) 2200)))
   (and (string? x) (str/blank? x))
   (and (coll?   x) (empty? x))
   (nil? x)))

(defn has-data?
  "Checks if an input of any type has data."
  [x]
  (not (no-data? x)))

(defn missing-data?
  "Checks if an input of any type is missing specific data."
  [& args]
  (some no-data? args))

(defn- is-numeric? [v]
  (if (string? v)
    (re-matches #"^-?([\d]+[\d\,]*\.*[\d]+)$|^-?([\d]+)$" v)
    (number? v)))

(defn intersects?
  "Checks whether or not two sets intersect."
  [s1 s2]
  {:pre [(every? set? [s1 s2])]}
  (-> (sets/intersection s1 s2)
      (count)
      (pos?)))

(defn num-str-compare
  "Compares two strings as numbers if they are numeric."
  [asc x y]
  (let [both-numbers? (and (is-numeric? x) (is-numeric? y))
        sort-x        (if both-numbers? (js/parseFloat x) x)
        sort-y        (if both-numbers? (js/parseFloat y) y)]
    (if asc
      (compare sort-x sort-y)
      (compare sort-y sort-x))))

(defn find-key-by-id
  "Finds the value of a key by id if one exists."
  ([coll id]
   (find-key-by-id coll id :opt-label))
  ([coll id k]
   (some #(when (= (:opt-id %) id) (get % k)) coll)))

(defn find-by-id
  "Finds the value of a specific id if one exists."
  [coll id]
  (some #(when (= (:opt-id %) id) %) coll))

(defn try-js-aget
  "Trys to call `aget` on the specified object."
  [obj & values]
  (try
    (reduce
     (fn [acc cur]
       (if (and acc (.hasOwnProperty acc cur))
         (aget acc cur)
         (reduced nil)))
     obj
     values)
    (catch js/Error e (js/console.log e))))

(defn to-precision
  "Rounds a double to n significant digits."
  [n dbl]
  (let [factor (.pow js/Math 10 n)]
    (/ (Math/round (* dbl factor)) factor)))

(defn call-when
  "Returns a function calls `f` only when `x` passes `pred`. Can be used in
   mapping over a collection like so:
   `(map (only even? #(* % 2)) xs)`"
  [pred f]
  (fn [x]
    (if (pred x) (f x) x)))

(defn copy-input-clipboard!
  "Copies the contents of `element-id` into the user's clipboard. `element-id` must
   be the ID of an HTML element in the document."
  [element-id]
  {:pre [(string? element-id)]}
  (doto (js/document.getElementById element-id)
    (.select))
  (js/document.execCommand "copy"))

(defn reverse-sorted-map
  "Creates a sorted-map where the keys are sorted in reverse order."
  []
  (sorted-map-by (fn [a b] (* -1 (compare a b)))))

(defn refresh-on-interval!
  "Refreshes the specified function every specified interval (ms) of time.
   Exit the go-loop by doing `put! exit-chan :exit` elsewhere in the code.
   Use stop-refresh! for simplicity"
  [on-refresh-fn interval]
  (let [exit-chan (chan)]
    (go-loop []
      (let [[result _] (alts! [(timeout interval) exit-chan])]
        (when-not (= :exit result)
          (on-refresh-fn)
          (recur))))
    exit-chan))

(defn atom?
  "Returns true if `a` is one of: Atom, Reagant Atom (RAtom), or Re-Frame reaction."
  [a]
  (or (instance? Atom a)
      (instance? RAtom a)
      (instance? Reaction a)))

(defn stop-refresh!
  "Take a chan from refresh-on-interval! and stops the refresh."
  [exit-chan]
  (when (chan? exit-chan)
    (put! exit-chan :exit)))

(defn direction
  "Converts degrees to a direction."
  [degrees]
  (condp >= degrees
    22.5  "North"
    67.5  "Northeast"
    112.5 "East"
    157.5 "Southeast"
    202.5 "South"
    247.5 "Southwest"
    292.5 "West"
    337.5 "Northwest"
    360   "North"
    ""))

(defn find-boundary-values
  "Returns the two values from a sorted collection that bound v."
  [v coll]
  (loop [coll coll]
    (let [s (second coll)]
      (and s
           (if (< v s)
             (take 2 coll)
             (recur (next coll)))))))

(defn clean-units
  "Cleans units by adding/not adding a space when needed for units."
  [units]
  (if (#{"%" "\u00B0F" "\u00B0"} units)
    units
    (str " " units)))

(defn filter-no-data
  "Removes any nodata 'label' entries from the provided legend-list."
  [legend-list]
  (remove (fn [leg]
            (= "nodata" (get leg "label")))
          legend-list))

(defn replace-no-data-nil
  "Replaces any nodata 'band' entries from the provided last-clicked-info list
   with nil."
  [last-clicked-info no-data-quantities]
  (map (fn [entry]
        (let [band-val (:band entry)]
          (assoc entry :band (if (contains? no-data-quantities (str band-val))
                               nil
                               band-val))))
       last-clicked-info))

(defn ->text-input-value
  "Convert a value `v` to be compatible for a text input."
  [v]
  (let [v (if (atom? v) @v v)]
    (cond
      (nil? v)
      ""

      (vector? v)
      (first v)

      (string? v)
      v

      (number? v)
      (str v))))
