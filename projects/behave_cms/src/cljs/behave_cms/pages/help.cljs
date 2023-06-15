(ns behave-cms.pages.help
  (:require [clojure.string             :as str]
            [clojure.set                :as set]
            [clojure.walk               :refer [postwalk]]
            [applied-science.js-interop :as j]
            [re-frame.core              :as rf]
            [herb.core                  :refer [<class]]
            [hickory.core               :refer [parse-fragment as-hiccup]]
            [behave-cms.markdown.core   :refer [md->html]]
            [behave-cms.utils           :as u]))

(defn get-files [data-tx]
  (if-let [items (.-items data-tx)]
    (keep #(when (= (.-kind %) "file")
        (.getAsFile %)) items)
    (.-files data-tx)))

(defn on-drop-image! [ev]
  ; Prevent default behavior (Prevent file from being opened)
  (.preventDefault ev)
  (js/console.log "-- DROPPED" ev)
  (let [files (get-files (j/get ev :dataTransfer))
        file  (first files)]
    (js/console.log file (j/get file :type))
    (when (str/starts-with? (j/get file :type) "image/")
      (rf/dispatch [:files/upload file [:state :editors :help-page]]))))

(defn file->url [file callback]
  (let [reader    (js/FileReader.)
        on-load   #(callback (j/get reader :result))]
    (.addEventListener reader "load" on-load)
    (.readAsDataURL reader file)))

(defn file->blob [file callback]
  (let [reader    (js/FileReader.)
        on-load   #(callback (j/get reader :result))]
    (.addEventListener reader "load" on-load)
    (.readAsArrayBuffer reader file)))

(defn on-select-image! [e]
  (let [file (first (array-seq (j/get-in e [:target :files])))]
    (file->url file #(rf/dispatch [:state/merge [:editors :help-page :images] [%]]))
    (rf/dispatch [:files/upload file [:state :editors :help-page]])))

(defn update-cursor! [e]
  (let [input (j/get e :target)
        start (j/get input :selectionStart)
        end   (j/get input :selectionEnd)]
    (rf/dispatch [:state/set-state [:editors :help-page :cursor] [start end]])))

(defn $textarea []
   {:font-family "var(--bs-font-monospace)"
    :width "100%"})

(defn textarea [state {:keys [on-drop on-change update-cursor]}]
  [:textarea.form-control
   {:class         (<class $textarea)
    :on-drop       on-drop
    :auto-focus    true
    :rows          10
    :value         @state
    :on-drag-over  update-cursor
    :on-key-down   update-cursor
    :on-key-up     update-cursor
    :on-mouse-down update-cursor
    :on-mouse-up   update-cursor
    :on-change     #(on-change (u/input-value %))}])

(defn editor-toolbar []
  [:div.mb-3
   [:label.form-label {:for "help-upload"} "Upload File"]
   [:input.form-control {:type "file"
                         :on-change #(on-select-image! %)
                         :accept "image/*"
                         :multiple false
                         :id "help-upload"}]])

(defn image-preview []
  (let [uploading? (rf/subscribe [:state [:editors :help-page :uploading?]])]
    ;images     (rf/subscribe [:state [:editors :help-page :images]])]
    [:<>
     (when @uploading?
       [:div.progress
        [:div {:class ["progress-bar" "progress-bar-striped" "progress-bar-animated"]
               :style {:width "100%"}}]])

     (when (vector? nil)
       [:div.row
        (for [image []]
          [:div.col.m-1
           [:img {:style {:background-image  (str "url(" image ")")
                          :height            "100px"
                          :width             "100px"
                          :background-size   "contain"
                          :background-repeat "no-repeat"
                          :resize            "both"}}]])])]))

(defn $markdown-editor []
  ^{:combinators {[:> :img] {:max-width "100%"
                             :height    "auto"}}}
  {})

(def special-svg-attrs
  {:zoomandpan          :zoom-and-pan,
   :surfacescale        :surface-scale,
   :keysplines          :key-splines,
   :markerheight        :marker-height,
   :attributetype       :attribute-type,
   :stitchtiles         :stitch-tiles,
   :calcmode            :calc-mode,
   :refy                :ref-y,
   :primitiveunits      :primitive-units,
   :diffuseconstant     :diffuse-constant,
   :patternunits        :pattern-units,
   :referrerpolicy      :referrer-policy,
   :keytimes            :key-times,
   :viewtarget          :view-target,
   :kernelmatrix        :kernel-matrix,
   :stddeviation        :std-deviation,
   :preservealpha       :preserve-alpha,
   :kernelunitlength    :kernel-unit-length,
   :numoctaves          :num-octaves,
   :refx                :ref-x,
   :specularconstant    :specular-constant,
   :glyphref            :glyph-ref,
   :viewbox             :view-box,
   :contentscripttype   :content-script-type,
   :baseprofile         :base-profile,
   :autoreverse         :auto-reverse,
   :pointsaty           :points-at-y,
   :repeatcount         :repeat-count,
   :markerunits         :marker-units,
   :targety             :target-y,
   :keypoints           :key-points,
   :basefrequency       :base-frequency,
   :targetx             :target-x,
   :attributename       :attribute-name,
   :patterncontentunits :pattern-content-units,
   :requiredextensions  :required-extensions,
   :repeatdur           :repeat-dur,
   :markerwidth         :marker-width,
   :maskunits           :mask-units,
   :filterres           :filter-res,
   :pathlength          :path-length,
   :ychannelselector    :y-channel-selector,
   :contentstyletype    :content-style-type,
   :filterunits         :filter-units,
   :xchannelselector    :x-channel-selector,
   :pointsatx           :points-at-x,
   :preserveaspectratio :preserve-aspect-ratio,
   :requiredfeatures    :required-features,
   :specularexponent    :specular-exponent,
   :maskcontentunits    :mask-content-units,
   :gradienttransform   :gradient-transform,
   :tablevalues         :table-values,
   :limitingconeangle   :limiting-cone-angle,
   :textlength          :text-length,
   :systemlanguage      :system-language,
   :gradientunits       :gradient-units,
   :pointsatz           :points-at-z,
   :lengthadjust        :length-adjust,
   :startoffset         :start-offset,
   :spreadmethod        :spread-method,
   :edgemode            :edge-mode,
   :clippathunits       :clip-path-units,
   :patterntransform    :pattern-transform})

(def special-svg-attrs-keys (-> special-svg-attrs (keys) (set)))

(defn style-map [s]
  (println s)
  (persistent!
    (as-> s $
      (str/split $ #";")
      (map #(str/split % #":") $)
      (reduce (fn [acc [k v]] (assoc! acc (keyword k) (str/trim v))) (transient {}) $))))

(defn md->hiccup [md]
  (->> md
       (md->html)
       (parse-fragment)
       (map as-hiccup)
       ;(postwalk #(if (and (= :svg (first %))
       ;                    (not-empty (set/intersection special-svg-attrs-keys (-> % (second) (keys) (set)))))
       ;             (assoc % 1 (persistent! (reduce (fn [acc [k v]] (assoc! acc (get special-svg-attrs k k) v)) (transient {}) (second %))))
       ;             %))
       (postwalk #(if-let [style-str (get-in % [1 :style])]
                    (assoc-in % [1 :style] (style-map style-str))
                    %))
       (map #(assoc-in % [1 :key] (-> % (last) (hash))))))

(defn markdown-editor [init-value]
  (when init-value
    (rf/dispatch [:state/set-state [:editors :help-page :content] init-value]))
  (let [content (rf/subscribe [:state [:editors :help-page :content]])
        on-change #(rf/dispatch [:state/set-state [:editors :help-page :content] %])]
    [:div.row {:class (<class $markdown-editor)}
     [:div.col-6
      [:h3 "Help Pages"]
      [:div.my-3
       [editor-toolbar]
       [image-preview]
       [textarea content {:on-change     on-change
                          :update-cursor update-cursor!
                          :on-drop       on-drop-image!}]]]
     [:div.col-6
      [:h3.mb-3 "Preview"]
      (when (some? @content)

      [:<> (md->hiccup @content)])]]))

(defn root-component [_]
  [:div.container
   [markdown-editor nil]])
