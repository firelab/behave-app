(ns behave.components.vega.core
  (:require [cljs.core.async.interop :refer-macros [<p!]]
            [cljsjs.vega-embed]
            [clojure.core.async :refer [go]]
            [clojure.walk       :as walk]
            [reagent.core       :as r]
            [reagent.dom        :as rd]))

(defn- set-param-value
  "`spec` with the `:value` of the selection param named `param-name` replaced by
  `value`. The param's location is an implementation detail of whichever spec
  builder produced it (`output-diagram` puts it on `[:layer 0]`, because a
  top-level param in a layered spec raises \"Duplicate signal name\"), so match on
  the name rather than walking a fixed path."
  [spec param-name value]
  (walk/postwalk
   (fn [x]
     (if (and (map? x) (= param-name (:name x)) (contains? x :select))
       (assoc x :value value)
       x))
   spec))

(defn- selection->set
  "The resolved value of a point-selection signal is `{<field> [v ...] :vlPoint ...}`,
  or `{}` when nothing is selected. Returns the selected values as a set."
  [js-value field]
  (-> (js->clj js-value) (get field) set))

(defn- ->store-tuples
  "Point-selection store tuples for `values` -- one per selected value, carrying
  the view's own `<param>_tuple_fields` descriptor so the field type is never
  hardcoded here."
  [view param values]
  (clj->js (mapv (fn [v] {:fields (.signal view (str param "_tuple_fields"))
                          :values #js [v]})
                 values)))

(defn- push-selection!
  "Replaces the selection store in the LIVE `view`. Going through the view rather
  than re-embedding means a selection change never tears down and re-draws the
  canvas; both the mark opacities and the legend graying just re-render."
  [view param values]
  (.data view (str param "_store") (->store-tuples view param values))
  (.runAsync view))

(defn- render-vega
  "Embeds `spec` in `elem`, with the current selection baked in, and resolves the
  embed into `state` so later selection changes can be pushed into the live view."
  [state elem {:keys [spec selection on-user-select]}]
  (let [{:keys [param field values]} selection
        ;; A re-embed can start while this one's promise is still pending. Stamp the
        ;; generation so the losing go-block discards its (finalized) view instead of
        ;; swapping it over the winner's -- after which every push would be a no-op on
        ;; a dead view.
        epoch                        (:epoch (swap! state update :epoch (fnil inc 0)))]
    (go
      (try
        (let [res  (<p! (js/vegaEmbed elem
                                      (clj->js (cond-> spec
                                                 param
                                                 (set-param-value
                                                  param
                                                  (mapv (fn [v] {field v}) values))))
                                      (clj->js {:renderer "canvas"
                                                :mode     "vega-lite"})))
              view (.-view res)]
          (if (not= epoch (:epoch @state))
            (when-let [f (.-finalize res)] (f))
            (do
              (swap! state assoc
                     :view     view
                     :finalize (.-finalize res)
                     :applied  (set values))
              (when (and param on-user-select)
                (.addSignalListener
                 view param
                 (fn [_ js-value]
                   (let [selected (selection->set js-value field)]
                     ;; Our own `push-selection!` echoes back through this listener, so
                     ;; only report a selection the app doesn't already know about --
                     ;; this is what makes the callback user-originated only.
                     (when (not= selected (:applied @state))
                       (swap! state assoc :applied selected)
                       (on-user-select selected))))))
              ;; A selection change may have landed while the embed promise was
              ;; pending; `:applied` is only authoritative once the view exists.
              (when param
                (let [pending (:pending-values @state)
                      latest  (set (if (some? pending) pending values))]
                  (when (not= latest (:applied @state))
                    (swap! state assoc :applied latest)
                    (push-selection! view param (vec latest))))))))
        (catch ExceptionInfo e (js/console.log (ex-cause e)))))))

(defn- finalize! [state]
  (when-let [f (:finalize @state)]
    (f))
  (swap! state dissoc :view :finalize :applied :pending-values))

(defn- vega-canvas []
  (let [state (atom {})]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (let [props (r/props this)]
          (swap! state assoc :pending-values (get-in props [:selection :values]))
          (render-vega state (rd/dom-node this) props)))

      :component-did-update
      (fn [this [_ old-props]]
        (let [{:keys [spec selection] :as props} (r/props this)
              {:keys [param values]}             selection]
          (swap! state assoc :pending-values values)
          (cond
            ;; A new spec (resize, new data) still needs a full re-embed.
            (not= spec (:spec old-props))
            (do (finalize! state)
                (render-vega state (rd/dom-node this) props))

            ;; Someone else changed the selection -- the modal and the inline
            ;; diagram share one app-db entry, and the inline one stays mounted
            ;; behind the modal -- so push it into the live view.
            (and param
                 (:view @state)
                 (not= (set values) (:applied @state)))
            (let [view (:view @state)]
              (swap! state assoc :applied (set values))
              ;; Mirrors the embed's catch: a throw here would otherwise leave the
              ;; stale selection on screen with no trace of why.
              (try
                (push-selection! view param values)
                (catch :default e (js/console.log e))))

            :else nil)))

      :component-will-unmount
      (fn [_] (finalize! state))

      :render
      (fn [this]
        [:div.vega-canvas
         {:style {:height (:box-height (r/props this))
                  :width  (:box-width  (r/props this))}}])})))

(defn vega-box
  "A function to create a Vega plot.

  `selection` (optional) binds a point-selection param to app state:
  `{:param \"visible_series\" :field \"series\" :values [\"A\" \"B\"]}`. The values
  are injected as the param's `:value` at embed time and pushed into the live view
  afterwards, so a change in app state never re-embeds the chart. How the param is
  driven -- a legend, a brush, a click on the marks -- is the caller's business.
  `on-user-select` (optional) is called with the set of selected values when the
  USER changes the selection. It is deliberately not called for the pushes this
  component makes on app state's behalf, so state and view cannot ping-pong. Omit
  it for a read-only chart."
  ([spec]
   (vega-box spec nil nil nil))
  ([spec box-height box-width]
   (vega-box spec box-height box-width nil))
  ([spec box-height box-width {:keys [selection on-user-select]}]
   [vega-canvas {:spec           spec
                 :box-height     box-height
                 :box-width      box-width
                 :selection      selection
                 :on-user-select on-user-select}]))
