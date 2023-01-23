(ns behave-cms.help.views
  (:require [clojure.string             :as str]
            [clojure.set                :refer [rename-keys]]
            [clojure.walk               :refer [postwalk]]
            [reagent.core               :as r]
            [re-frame.core              :as rf]
            [herb.core                  :refer [<class]]
            [hickory.core               :refer [parse-fragment as-hiccup]]
            [behave-cms.help.markdown   :refer [md->hiccup]]
            [data-utils.interface       :refer [parse-int]]
            [behave-cms.help.events]
            [behave-cms.help.subs]
            [behave-cms.utils           :as u]))

;;; Styling

(defn $textarea []
   {:font-family "var(--bs-font-monospace)"
    :width "100%"})

(defn $markdown-editor []
  ^{:combinators {[:> :img] {:max-width "100%"
                             :height    "auto"}}}
  {})

;;; Helpers

(defn on-image-uploaded-succes [help-key original save-page! file-path]
  (let [editor-path [:editors :help-page help-key]
        editor      (rf/subscribe [:state [:editors :help-page help-key]])
        cursor      (get @editor :cursor [0 0])
        content     (get @editor :help-page/content (or original ""))
        img-md      (str "![](" file-path ")")
        [c1 c2]     (split-at (first cursor) content)
        content     (str (str/join "" c1) "\n" img-md "\n" (str/join "" c2))]
    (println [:UPLOADED editor-path content])
    (rf/dispatch-sync [:state/merge
                       [:editors :help-page help-key]
                       {:images            [file-path]
                        :help-page/content content
                        :dirty?            true}])
    (save-page!)))

(defn- select-image [event help-key original save-page!]
  (u/on-select-image
   event
   #(rf/dispatch [:files/upload
                  %
                  [:state :editors :help-page help-key]
                  (partial on-image-uploaded-succes help-key original save-page!)])
   :url
   #(fn [file-path]
      (rf/dispatch-sync [:state/merge [:editors :help-page help-key :images] [file-path]]))))

;;; Components

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

(defn editor-toolbar [help-key draft save-page!]
  [:div.mb-3
   [:label.form-label {:for "help-upload"} "Upload File"]
   [:input.form-control {:type      "file"
                         :on-change #(select-image % help-key @draft save-page!)
                         :accept    "image/*"
                         :multiple  false
                         :id        "help-upload"}]])

(defn image-preview [help-key]
  (let [uploading? (rf/subscribe [:help-editor/state help-key :uploading?])]
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

(defn language-selector [help-key on-select]
  (let [languages (rf/subscribe [:languages])]
    [:div.mb-3
     [:label.form-label "Language"]
     [:select.form-select {:on-change #(-> % (u/input-value) (parse-int) (on-select))}
      [:option {:key "none" :value nil :selected true}
       (str "Select language...")]
      (for [{id :db/id shortcode :language/shortcode language :language/name} @languages]
        ^{:key id}
        [:option {:value id} (str language " (" shortcode ")")])]]))

(defn help-preview [draft]
  [:div.col-6
   [:h6.mb-3 "Preview"]
   (when (some? @draft)
     [:div {:class "help"} (md->hiccup @draft)])])

(defn help-text-editor [help-key language draft dirty? save-page!]
  (let [autosave! (u/debounce #(when @dirty? (save-page!)) 3000)
        on-change #(do
                     (rf/dispatch [:help-editor/set help-key :dirty? true])
                     (rf/dispatch [:help-editor/set help-key :help-page/content %])
                     (autosave!))
        on-drop   (fn [e]
                    (u/on-drop-image
                     e
                     #(rf/dispatch [:files/upload
                                    %
                                    [:state :editors :help-page help-key]
                                    (partial on-image-uploaded-succes help-key @draft save-page!)])))]
    [textarea draft {:disabled?     (nil? language)
                     :on-change     on-change
                     :update-cursor #(rf/dispatch [:help-editor/set help-key :cursor (u/cursor-location %)])
                     :on-drop       on-drop}]))

;;; Public Components

(defn help-editor [help-key]
  (let [dirty?          (rf/subscribe [:help-editor/state help-key :dirty?])
        language        (rf/subscribe [:help-editor/state help-key :language])
        page            (rf/subscribe [:help/page help-key @language])
        original        (:help-page/content @page)
        draft           (r/track #(or @(rf/subscribe [:help-editor/state help-key :help-page/content])
                                      original ""))
        select-language #(rf/dispatch [:help-editor/set help-key :language %])
        save-page!      #(do
                           (rf/dispatch [:help-editor/set help-key :dirty? false])
                           (rf/dispatch [:help-editor/save help-key @page]))
        on-submit       (u/on-submit save-page!)]
    [:div.row {:class (<class $markdown-editor)}
     [:div.col-6
      [:h6 "Editor"]
      [:form.my-3 {:on-submit on-submit}
       [editor-toolbar help-key draft save-page!]
       [image-preview help-key]
       [language-selector help-key select-language]
       [help-text-editor help-key @language draft dirty? save-page!]
       [:button.my-3.btn.btn-sm.btn-outline-primary
        {:type     "submit"
         :disabled (not @dirty?)}
        "Save"]]]
     [help-preview draft]]))

(comment


  (def help-key "behaveplus:contain:help")
  (def language (rf/subscribe [:help-editor/state help-key :language]))
  language
  (rf/subscribe [:help/page help-key @language])
  (rf/subscribe [:help-editor/state help-key])

  )
