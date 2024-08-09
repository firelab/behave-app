(ns behave.help.views
  (:require [re-frame.core             :refer [subscribe dispatch]]
            [markdown2hiccup.interface :refer [md->hiccup]]
            [behave.components.core    :as c]
            [behave.translate          :refer [<t]]
            [behave.help.events]
            [behave.help.subs]))

(defn- test-guides []
  (let [guides-manuals (<t "behaveplus:guides_and_manuals")]
    [:div
     [:h1 @guides-manuals]
     [:h2 "Here are some guides."]]))

(defn- get-help-keys [params]
  (cond

    (= (:page params) :units)
    []

    (:page params)
    (:page params)

    (and (:module params) (:submodule params))
    (let [{:keys [module submodule io]} params
          *module                       (subscribe [:wizard/*module module])
          *submodule                    (subscribe [:wizard/*submodule (:db/id @*module) submodule io])
          submodule-help-keys           (subscribe [:help/submodule-help-keys (:db/id @*submodule)])]
      (concat [(:module/help-key @*module)] @submodule-help-keys))

    :else
    "behaveplus:help"))

(defn- help-section
  "Displays a help section. Optionally takes `highlight?` to highlight a section."
  [current-key highlight?]
  (let [help-contents (subscribe [:help/content current-key])]
    ^{:key current-key}
    [:div {:id    current-key
           :class [(when highlight? "highlight")]}
     (when (not-empty @help-contents)
       [:div.help-section__content
        (md->hiccup (first @help-contents))])]))

(defn- help-content [help-keys & [children]]
  (let [help-highlighted-key (subscribe [:help/current-highlighted-key])]
    [:div.help-area__content
     {:tabindex 0}
     (cond
       children
       [children]

       (string? help-keys)
       [help-section help-keys (= help-keys @help-highlighted-key)]

       (seq help-keys)
       (doall (for [help-key help-keys]
                ^{:key help-key}
                [help-section help-key (= help-key @help-highlighted-key)])))]))

(defn help-area [params]
  (let [hidden?               (subscribe [:state [:help-area :hidden?]])
        current-tab           (subscribe [:help/current-tab])
        loaded?               (subscribe [:app/loaded?])
        selected-tool-uuid    (subscribe [:tool/selected-tool-uuid])
        selected-subtool-uuid (subscribe [:tool/selected-subtool-uuid])
        tool-help-keys        (subscribe [:help/tool-help-keys
                                          @selected-tool-uuid
                                          @selected-subtool-uuid])]
    (if @hidden?
      [:div.help-area__expand
       [c/button {:variant       "highlight"
                  :icon-name     "help2"
                  :icon-position "right"
                  :size          "small"
                  :flat-edge     "right"
                  :on-click      #(dispatch [:state/update [:help-area :hidden?] (partial not)])}]]
      [:div
       {:class     ["help-area"
                    (when @(subscribe [:state [:sidebar :hidden?]]) "help-area--sidebar-hidden")]
        :aria-live "polite"}
       [:div.help-area__tabs-container
        [:div.help-area__tabs
         [c/tab-group {:variant  "primary"
                       :on-click #(dispatch [:help/select-tab %])
                       :size     "small"
                       :tabs     (cond-> [{:label     "Help" :icon-name "help2"
                                           :tab       :module
                                           :selected? (= @current-tab :module)}
                                          {:label     "Guides & Manuals"
                                           :icon-name "help-manual"
                                           :tab       :guides
                                           :selected? (= @current-tab :guides)}]
                                   (some? selected-tool-uuid)
                                   (conj {:label     "Tools"
                                          :icon-name "help-manual"
                                          :tab       :tools
                                          :selected? (= @current-tab :tools)}))}]]
        [:div.help-area__close
         [:div.container__close
          [c/button {:icon-name "close"
                     :on-click  #(dispatch [:state/update [:help-area :hidden?] (partial not)])
                     :size      "small"
                     :variant   "secondary"}]]]]
       (cond
         (= @current-tab :guides)
         [help-content "behaveplus:guides" test-guides]

         (= @current-tab :tools)
         [help-content @tool-help-keys]

         :else
         (when @loaded?
           [help-content (get-help-keys params)]))])))
