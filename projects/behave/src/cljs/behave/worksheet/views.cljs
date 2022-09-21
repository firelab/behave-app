(ns behave.worksheet.views
  (:require [behave.components.core       :as c]
            [behave.components.navigation :refer [wizard-navigation]]
            [behave.translate             :refer [<t bp]]
            [behave.worksheet.events]
            [re-frame.core                :as rf]
            [string-utils.interface       :refer [->str]]))

(defn- workflow-select-header []
  [:div.workflow-select__header
   [c/tab {:variant   "outline-primary"
           :selected? true
           :label     @(<t "behaveplus:working_area")}]
   [:div.workflow-select__header__title
    [c/icon "modules"]
    [:div
     [:h3 @(<t "behaveplus:module_selection")]
     [:p @(<t "behaveplus:please_select_from_the_following_options")]]]])

(defn workflow-select [params]
  (let [*workflow (rf/subscribe [:state [:worksheet :*workflow]])]
    [:<>
     [:div.workflow-select
      [workflow-select-header]
      [:div.workflow-select__content
       [c/card-group {:on-select #(rf/dispatch [:state/set [:worksheet :*workflow] (:workflow %)])
                      :cards     [{:title     @(<t "behaveplus:workflow:guided_title")
                                   :selected? (= @*workflow :guided)
                                   :content   @(<t "behaveplus:workflow:guided_desc")
                                   :icons     [{:icon-name "guided-work"
                                                :checked?  (= @*workflow :guided)}]
                                   :order     0
                                   :workflow  :guided}
                                  {:title     @(<t "behaveplus:workflow:independent_title")
                                   :content   @(<t "behaveplus:workflow:independent_desc")
                                   :icons     [{:icon-name "independent-work"
                                                :checked?  (= @*workflow :independent)}]
                                   :selected? (= @*workflow :independent)
                                   :order     1
                                   :workflow  :independent}
                                  {:title     @(<t "behaveplus:workflow:import_title")
                                   :content   @(<t "behaveplus:workflow:import_desc")
                                   :icons     [{:icon-name "existing-run"
                                                :checked?  (= @*workflow :import)}]
                                   :selected? (= @*workflow :import)
                                   :order     2
                                   :workflow  :import}]}]]
      [wizard-navigation {:next-label "Next"
                          :on-next    #(rf/dispatch [:navigate (str "/worksheets/" (->str @*workflow))])}]]]))

;; TODO use title
(defn independent-worksheet-page [params]
  (let [*modules (rf/subscribe [:state [:worksheet :*modules]])]
    [:div.workflow-select
     [workflow-select-header]
     [:div.workflow-select__content
      [c/card-group {:on-select      #(rf/dispatch [:state/set [:worksheet :*modules] (:module %)])
                     :flex-direction "row"
                     :cards          [{:order     1
                                       :title     @(<t (bp "surface_and_crown"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "surface"}
                                                   {:icon-name "crown"}]
                                       :selected? (= @*modules #{:surface :crown})
                                       :module    #{:surface :crown}}
                                      {:order     2
                                       :title     @(<t (bp "surface_only"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "surface"}]
                                       :selected? (= @*modules #{:surface})
                                       :module    #{:surface}}
                                      {:order     3
                                       :title     @(<t (bp "surface_and_contain"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "surface"}
                                                   {:icon-name "contain"}]
                                       :selected? (= @*modules #{:surface :contain})
                                       :module    #{:surface :contain}}
                                      {:order     4
                                       :title     @(<t (bp "surface_and_mortality"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "surface"}
                                                   {:icon-name "mortality"}]
                                       :selected? (= @*modules #{:surface :mortality})
                                       :module    #{:surface :mortality}}
                                      {:order     5
                                       :title     @(<t (bp "mortality_only"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "mortality"}]
                                       :selected? (= @*modules #{:mortality})
                                       :module    #{:mortality}}]}]]
     [wizard-navigation {:next-label @(<t (bp "next"))
                         :back-label @(<t (bp "back"))
                         :on-back    #(.back js/history)
                         :on-next    #(rf/dispatch [:navigate "/worksheets/1/modules/contain/output/fire"])}]]))

(defn guided-worksheet-page [params]
  [:<>
   [:div.workflow-select
    [:div.workflow-select__header
     [:h3 "TODO: FLESH OUT GUIDED WORKSHEET"]]]])

(defn import-worksheet-page [params]
  (let [file (r/track #(or @(rf/subscribe [:state [:worksheet :file]])
                           @(<t (bp "select_a_file"))))]
    [:<>
     [c/accordion
      {:title @(<t (bp "working_area"))}
      [:<>
       [:div.workflow-select
        [:div.workflow-select__header
         [:h3 @(<t (bp "module_selection"))]
         [:p @(<t (bp "please_select_from_the_following_options"))]]
        [:div.workflow-select__content
         [c/browse-input {:button-label @(<t (bp "browse"))
                          :accept       ".bpr,bpw,.bp6,.bp7"
                          :label        @file
                          :on-change    #(rf/dispatch [:ws/worksheet-selected (.. % -target -files)])}]
         [wizard-navigation {:next-label @(<t (bp "next"))
                             :back-label @(<t (bp "back"))
                             :on-back    #(.back js/history)
                             :on-next    #(rf/dispatch [:navigate "/worksheets/1/modules/contain/output/fire"])}]]]]]]))

;; TODO use title
(defn new-worksheet-page [params]
  (let [title @(<t "behaveplus:working_area")]
    [workflow-select params]))
