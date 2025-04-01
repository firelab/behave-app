(ns behave.help.views
  (:require [clojure.walk           :refer [postwalk]]
            [clojure.string         :as str]
            [re-frame.core          :refer [subscribe dispatch dispatch-sync]]
            [reagent.core           :as r]
            [hickory.core           :as h]
            [behave.components.core :as c]
            [behave.translate       :refer [<t]]
            [behave.help.events]
            [behave.help.subs]))

;;; HTML Decoding

(defonce ^:private html-codes
  {"&amp;"   "&"
   "&lt;"    "<"
   "&gt;"    ">"
   "&quot;"  "\""
   "&apos;"  "'"})

(defn- decode-html-string [html-str]
  (str/replace html-str #"&\w+?;" html-codes))

;;; Helper Functions

(defn- test-guides []
  (let [guides-manuals (<t "behaveplus:guides_and_manuals")]
    [:div
     [:h1 @guides-manuals]
     [:h2 "Here are some guides."]]))

(defn- display-group? [ws-uuid group]
  (and (not (:group/research? group)) 
       @(subscribe [:wizard/show-group?
                    ws-uuid
                    (:db/id group)
                    (:group/conditionals-operator group)])))

(defn- flatten-help-keys [acc g-or-gv]
  (let [acc (conj acc (or (:group/help-key g-or-gv)
                          (:group-variable/help-key g-or-gv)))]
    (cond-> acc
      (seq (:group/group-variables g-or-gv))
      (concat (map (partial flatten-help-keys []) (sort-by :group-variables/order (:group/group-variables g-or-gv))))

      (seq (:group/children g-or-gv))
      (concat (map (partial flatten-help-keys []) (sort-by :group/order (:group/children g-or-gv)))))))

(defn- ->filter-groups-walk-fn [ws-uuid]
  (fn [node]
    (cond
      (:group/name node)
      (if (display-group? ws-uuid node) node nil)

      (or 
       (:group-variable/research? node)
       (:group-variable/conditionally-set? node))
      nil

      :else
      node)))

(defn- get-help-keys [params]
  (cond

    (= (:page params) :units)
    []

    (:page params)
    (:page params)

    (and (:module params) (:submodule params))
    (let [{:keys [ws-uuid module submodule io]} params
          [_ first-output-submodule]            @(subscribe [:wizard/first-module+submodule ws-uuid :output])
          second-module                         (-> @(subscribe [:worksheet ws-uuid])
                                                    (:worksheet/modules)
                                                    (set)
                                                    (disj :surface)
                                                    (first))
          module                                (if (and (= second-module :mortality) (= io :output) (= submodule "fire-behavior"))
                                                  (name second-module)
                                                  module)
          *module                               (subscribe [:wizard/*module module])
          *submodule                            (if (and (= second-module :mortality) (= io :output) (= submodule "fire-behavior"))
                                                  (subscribe [:wizard/*submodule (:db/id @*module) "mortality" :output])
                                                  (subscribe [:wizard/*submodule (:db/id @*module) submodule io]))
          submodule-id                          (:db/id @*submodule)
          *groups                               (subscribe [:vms/pull-children :submodule/groups submodule-id
                                                            '[* {:group/group-variables [*]} {:group/children 6}]])
          submodule-help-keys                   (->> @*groups
                                                     (sort-by :group/order)
                                                     (postwalk (->filter-groups-walk-fn ws-uuid))
                                                     (mapcat (partial flatten-help-keys []))
                                                     (flatten)
                                                     (filter some?)
                                                     (concat [(:submodule/help-key @*submodule)])
                                                     (vec))]

      (if (or (and (= second-module "mortality") (= io :output) (= submodule "fire-behavior"))
              (and (= io :output) (= first-output-submodule submodule)))
        (concat [(:module/help-key @*module)] submodule-help-keys)
        submodule-help-keys))

    :else
    "behaveplus:help"))

;;; Image Viewer

(defn- open-image-viewer-fn [e]
  (when (= "IMG" (.. e -target -nodeName))
    (let [el (.-target e)
          url (.-src el)
          alt (.-alt el)]
      (.log js/console el)
      (dispatch [:help/open-image-viewer url alt]))))

;;; Components

(defn- help-section
  "Displays a help section. Optionally takes `highlight?` to highlight a section."
  [current-key highlight?]
  (let [help-contents (subscribe [:help/content current-key])]
    ^{:key current-key}
    [:div {:id    current-key
           :class [(when highlight? "highlight")]}
     (when (not-empty @help-contents)
       [:div.help-section__content
        (-> @help-contents
            (h/parse-fragment)
            (->> (map h/as-hiccup)
                 (postwalk #(if (string? %) (decode-html-string %) %))))])]))

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

;;; Public

(defn help-area
  "Displays the Help Area for a particular page."
  [params]
  (r/with-let [_ (dispatch-sync [:help/select-tab {:tab :module}])]
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
         :on-click  open-image-viewer-fn
         :aria-live "polite"}
        [:div.help-area__tabs-container
         [:div.help-area__tabs
          [c/tab-group {:variant  "primary"
                        :on-click #(dispatch [:help/select-tab %])
                        :size     "small"
                        :tabs     [{:label     "Help" :icon-name "help2"
                                    :tab       :module
                                    :selected? (= @current-tab :module)}]}]]
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
            [help-content (get-help-keys params)]))]))))
