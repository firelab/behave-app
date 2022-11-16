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
    (:page params)
    (:page params)

    (and (:module params) (:submodule params))
    (let [{:keys [module submodule io]} params
          *module                       (subscribe [:wizard/*module module])
          *submodule                    (subscribe [:wizard/*submodule (:db/id @*module) submodule io])
          submodule-help-keys           (subscribe [:help/submodule-help-keys (:db/id @*submodule)])]
      [(:module/help-key @*module) @submodule-help-keys])

    :else
    "behaveplus:help"))

(defn- help-section
  "Recursivley builds a tree of elements given a nested vector of keywords.
  (i.e. [a [b [c d]] [e [f g]]]).

      a
  ---------
   |  |  |
   b  c  e
      | ---
      | | |
      d f g

  Elements will be built using preorder depth first search traversal.
  For the above example the order is: a b c d e f g."
  [current-node]
  (let [last-child?          (string? current-node)
        current-key          (if last-child? current-node (first current-node))
        help-contents        (subscribe [:help/content current-key])
        help-highlighted-key (subscribe [:help/current-highlighted-key])]
    [:div {:id    current-key
           :class [(when (= @help-highlighted-key current-key) "highlight")]}
     (when (not-empty @help-contents)
       [:div.help-section__content
        (md->hiccup (first @help-contents))])
     (when (not last-child?)
       (map help-section (next current-node)))]))

(defn- help-content [help-keys & [children]]
  [:div.help-area__content
   (cond
     children
     [children]

     (string? help-keys)
     [help-section [help-keys]]

     :else
     [help-section help-keys])])

(defn help-area [params]
  (let [current-tab (subscribe [:help/current-tab])
        loaded?     (subscribe [:state :loaded?])]
    [:div.help-area
     [:div.help-area__tabs
      [c/tab-group {:variant  "outline-secondary"
                    :on-click #(dispatch [:help/select-tab %])
                    :tabs     [{:label "Help" :icon-name "help2" :tab :help}
                               {:label "Guides & Manuals" :icon-name "help-manual" :tab :guides}]}]]
     (cond
       (= @current-tab :guides)
       [help-content "behaveplus:guides" test-guides]

       :else
       (when @loaded?
         [help-content (get-help-keys params)]))]))
