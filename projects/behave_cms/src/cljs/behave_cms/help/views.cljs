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

(defn $textarea []
   {:font-family "var(--bs-font-monospace)"
    :width "100%"})

(defn textarea [state {:keys [disabled? on-drop on-change update-cursor]}]
  [:textarea.form-control
   {:class         (<class $textarea)
    :disabled      disabled?
    :on-drop       on-drop
    :rows          10
    :default-value @state
    :on-drag-over  update-cursor
    :on-key-down   update-cursor
    :on-key-up     update-cursor
    :on-mouse-down update-cursor
    :on-mouse-up   update-cursor
    :on-change     #(on-change (u/input-value %))}])

(defn- select-image [event save-page!]
  (u/on-select-image
   event
   #(rf/dispatch [:files/upload % [:state :editors :help-page] save-page!])
   :url
   #(rf/dispatch [:state/merge [:editors :help-page :images] [%]])))

(defn editor-toolbar [save-page!]
  [:div.mb-3
   [:label.form-label {:for "help-upload"} "Upload File"]
   [:input.form-control {:type      "file"
                         :on-change #(select-image % save-page!)
                         :accept    "image/*"
                         :multiple  false
                         :id        "help-upload"}]])

(defn image-preview []
  (let [uploading? (rf/subscribe [:help-editor/state :uploading?])]
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

(defn language-selector [on-select]
  (let [languages (rf/subscribe [:languages])]
    [:div.mb-3
     [:label.form-label "Language"]
     [:select.form-select {:on-change #(rf/dispatch [:help-editor/set :language (-> % (u/input-value) (parse-int))])}
      [:option {:key "none" :value nil :selected true}
       (str "Select language...")]
      (for [{id :db/id shortcode :language/shortcode language :language/name} @languages]
        ^{:key id}
        [:option {:value id} (str language " (" shortcode ")")])]]))

(defn help-preview [page]
  (let [content (r/track #(or @(rf/subscribe [:help-editor/state :help-page/content]) (:help-page/content page) ""))]
        [:div.col-6
         [:h6.mb-3 "Preview"]
         (when (some? @content)
           [:div {:class "help"} (md->hiccup @content)])]))

(defn help-text-editor [help-key language page save-page!]
  (rf/dispatch [:help-editor/set :help-page/content (:help-page/content page)])
  (r/with-let [content   (rf/subscribe [:help-editor/state :help-page/content])
               dirty?    (rf/subscribe [:help-editor/state :dirty?])
               autosave! (u/debounce #(when @dirty? (save-page!)) 3000)
               on-change #(do
                            (rf/dispatch [:help-editor/set :dirty? true])
                            (rf/dispatch [:help-editor/set :help-page/content %])
                            (autosave!))
               on-drop   (fn [e]
                           (u/on-drop-image e #(rf/dispatch [:files/upload % [:state :editors :help-page] save-page!])))]
    [textarea content {:disabled?     (nil? language)
                       :on-change     on-change
                       :update-cursor #(rf/dispatch [:help-editor/set :cursor (u/cursor-location %)])
                       :on-drop       on-drop}]))

(defn help-editor [help-key]
  (rf/dispatch [:help-editor/set :help-page/key help-key])
  (let [dirty?     (rf/subscribe [:help-editor/state :dirty?])
        language   (rf/subscribe [:help-editor/state :language])
        page       (rf/subscribe [:help/page help-key @language])
        save-page! #(do
                      (rf/dispatch [:help-editor/set :dirty? false])
                      (rf/dispatch [:help-editor/save @page]))
        on-submit  (u/on-submit save-page!)]
    [:div.row {:class (<class $markdown-editor)}
     [:div.col-6
      [:h6 "Editor"]
      [:form.my-3 {:on-submit on-submit}
       [editor-toolbar save-page!]
       [image-preview]
       [language-selector]
       [help-text-editor help-key @language @page save-page!]
       [:button.my-3.btn.btn-sm.btn-outline-primary
          {:type     "submit"
           :disabled (not @dirty?)}
          "Save"]]]
     [help-preview @page]]))


(comment

  (rf/subscribe [:state :editors])

  (rf/subscribe [:pull '[*] 2968])
  (rf/subscribe [:query
                 '[:find  ?h .
                   :in    $ ?help-key
                   :where [?h :help/key ?help-key]]
                 ["test-app:help"]])

  (def help-key "behaveplus:help")
  (def language 129)
  (rf/subscribe [:state [:editors :help-page :cursor]])
  (rf/subscribe [:state [:editors :help-page :help/content]])

  (md->hiccup @(rf/subscribe [:help-editor/state :help/content]))

  (let [help-id   (rf/subscribe [:help/_page help-key language])
        help-page (rf/subscribe [:pull '[*] (first @help-id)])]
    @help-page)

  )
