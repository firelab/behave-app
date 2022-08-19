(ns behave.help.views
  (:require [re-frame.core :as rf]
            [markdown2hiccup.interface :refer [md->hiccup]]
            [behave.components.core :as c]
            [behave.translate :refer [<t]]
            [behave.help.events]
            [behave.help.subs]))

(defn- test-guides []
  (let [guides-manuals (<t "behaveplus:guides_and_manuals")]
    [:div
     [:h1 @guides-manuals]
     [:h2 "Here are some guides."]]))

(defn- get-help-keys [params]
  (cond
    (:page params)
    (:page params)

    (and (:module params) (:submodule params))
    (let [{:keys [module submodule io]} params
          *module (rf/subscribe [:wizard/*module module])
          *submodule (rf/subscribe [:wizard/*submodule (:db/id @*module) submodule io])
          submodule-help-keys (rf/subscribe [:help/submodule-help-keys (:db/id @*submodule)])]
      [(:module/help-key @*module) @submodule-help-keys])

    :else
    "behaveplus:help"))

(defn- help-section [[help-key & content]]
  (let [help-content (rf/subscribe [:help/content help-key])]
    [:<>
     (when (not-empty @help-content)
       (md->hiccup (first @help-content)))

     (cond
       (nil? content)
       nil

       (vector? (first content))
       [help-section (first content)]

       :else
       [help-section content])]))

(defn- help-content [help-keys & [children]]
  [:div.help-area__content
   (cond
     (vector? children)
     [children]

     (string? help-keys)
     [help-section [help-keys]]

     :else
     [help-section help-keys])])

(defn help-area [params]
  (let [current-tab (rf/subscribe [:help/current-tab])
        loaded? (rf/subscribe [:state :loaded?])]
    [:div.help-area
     [:div.help-area__tabs
      [c/tab-group {:variant     "help"
                    :selected-fn #(= @current-tab (:tab %))
                    :on-select   #(rf/dispatch [:help/select-tab %])
                    :tabs        [{:label "Help" :tab :help}
                                  {:label "Guides & Manuals" :tab :guides}]}]]
     (cond
       (= @current-tab :guides)
       [help-content "behaveplus:guides" test-guides]

       :else
       (when @loaded?
         [help-content (get-help-keys params)]))]))
