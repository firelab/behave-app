(ns behave-cms.components.help-editor
  (:require [clojure.string             :as str]
            [clojure.set                :refer [rename-keys]]
            [clojure.walk               :refer [postwalk]]
            [applied-science.js-interop :as j]
            [reagent.core               :as r]
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
  (let [files (get-files (j/get ev :dataTransfer))
        file  (first files)]
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

(defn textarea [state {:keys [disabled? on-drop on-change update-cursor]}]
  [:textarea.form-control
   {:class         (<class $textarea)
    :disabled      disabled?
    :on-drop       on-drop
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

(defn str->style-map [s]
  (persistent!
    (as-> s $
      (str/split $ #";")
      (map #(str/split % #":") $)
      (reduce (fn [acc [k v]] (assoc! acc (keyword k) (str/trim v))) (transient {}) $))))

(defn fix-svg-attrs [el]
  (if (= :svg (first el))
    (assoc el 1 (rename-keys (second el) special-svg-attrs))
    el))

(defn fix-style-map [el]
  (if-let [style-str (get-in el [1 :style])]
    (assoc-in el [1 :style] (str->style-map style-str))
    el))

(defn add-key-attr [el]
  (if (-> el (second) (map?))
    (assoc-in el [1 :key] (-> el (last) (hash)))
    el))

(defn- md->hiccup [md]
  (->> md
       (md->html)
       (parse-fragment)
       (map as-hiccup)
       (postwalk #(if (vector? %)
                    (-> %
                        (fix-style-map)
                        (fix-svg-attrs)
                        (add-key-attr))
                    %))))

(defn upsert-help-page! [help-page]
  (let [data (select-keys help-page [:uuid :key :language_rid :content])]
    (if (:uuid data)
      (rf/dispatch [:api/update-entity :help-pages data])
      (rf/dispatch [:api/create-entity :help-pages data]))))

(defn language-selector [on-select]
  (rf/dispatch [:api/entities :languages])
  (r/with-let [languages (rf/subscribe [:entities :languages])
               on-select #(rf/dispatch [:state/set-state [:editors :help-page :language_rid] (-> % (u/input-value) (uuid))])]
    [:div.mb-3
     [:label.form-label "Language"]
     [:select.form-select {:on-change on-select}
      [:option {:key "none" :value nil :selected? true}
       (str "Select language...")]
      (for [[uuid {:keys [shortcode language]}] @languages]
        [:option {:key uuid :value uuid} (str language " (" shortcode ")")])]]))

(defn help-preview []
  (let [content (rf/subscribe [:state [:editors :help-page :content]])]
  [:div.col-6
   [:h6.mb-3 "Preview"]
   (when (some? @content)
     [:div {:class "help"} (md->hiccup @content)])]))

(defn help-text-editor [help-key save-page!]
  (let [language  (rf/subscribe [:state [:editors :help-page :language_rid]])
        help-page (first (filter (fn [{:keys [key language_rid]}]
                            (and (= help-key key)
                                 (= @language language_rid)))
                          (vals @(rf/subscribe [:entities :help-pages]))))
        _         (if (some? help-page)
                    (rf/dispatch [:state/merge [:editors :help-page] help-page])
                    (rf/dispatch [:state/set-state [:editors :help-page] {:key help-key
                                                                          :language_rid @language
                                                                          :content ""
                                                                          :cursor [0 0]}]))
        content   (rf/subscribe [:state [:editors :help-page :content]])
        dirty?    (rf/subscribe [:state [:editors :help-page :dirty?]])
        autosave! (u/debounce #(when @dirty? (save-page!)) 3000)
        on-change #(do
                     (rf/dispatch [:state/set-state [:editors :help-page :dirty?] true])
                     (rf/dispatch [:state/set-state [:editors :help-page :content] %])
                     (autosave!))]
    [textarea content {:disabled?     (nil? @language)
                       :on-change     on-change
                       :update-cursor update-cursor!
                       :on-drop       on-drop-image!}]))

(defn help-editor [help-key]
  (rf/dispatch [:api/entities :help-pages {:help-key help-key}])
  (rf/dispatch [:state/set-state [:editors :help-page :key] help-key])
  (r/with-let [dirty?      (rf/subscribe [:state [:editors :help-page :dirty?]])
               save-page!  #(do
                              (rf/dispatch [:state/set-state [:editors :help-page :dirty?] false])
                              (upsert-help-page! @(rf/subscribe [:state [:editors :help-page]])))
               on-submit   (u/on-submit save-page!)]
    [:div.row {:class (<class $markdown-editor)}
     [:div.col-6
      [:h6 "Editor"]
      [:form.my-3 {:on-submit on-submit}
       [editor-toolbar]
       [image-preview]
       [language-selector]
       [help-text-editor help-key save-page!]
       [:button.my-3.btn.btn-sm.btn-outline-primary
        {:type "submit"
         :disabled (not @dirty?)}
        "Save"]]]
     [help-preview]]))
