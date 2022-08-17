(ns behave.help.views
  (:require [re-frame.core :as rf]
            [behave.components.core :as c]
            [behave.translate       :refer [<t]]
            [behave.help.events]
            [behave.help.subs]))

(defn test-guides []
  (let [guides-manuals (<t "behaveplus:guides_and_manuals")]
    [:div
     [:h1 @guides-manuals]
     [:h2 "Here are some guides."]]))

(defn test-content []
  [:div
   [:h1 "Help Area"]
   [:h2 "Here is help content."]])

(defn help-content [help-key & [children]]
  ;(let [help-content (rf/subscribe [:query '[:find ?help
  ;                                           :in $ ?help-key
  ;                                           :where [?e :help/key ?help-key]
  ;                                                  [?e :help/content ?help]] help-key])]
    [:div.help-area__content [children]])

(defn help-area [{:keys [io module submodule] :as params}]
  (let [current-tab (rf/subscribe [:help/current-tab])]
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

       (and (= @current-tab :help) (some? help-key))
       [help-content help-key test-content]

       :else
       [help-content nil test-content])]))
