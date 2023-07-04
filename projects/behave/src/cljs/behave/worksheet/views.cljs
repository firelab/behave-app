(ns behave.worksheet.views
  (:require [behave.components.core       :as c]
            [behave.components.navigation :refer [wizard-navigation]]
            [behave.translate             :refer [<t bp]]
            [behave.worksheet.events]
            [datascript.core              :refer [squuid]]
            [re-frame.core                :as rf]
            [reagent.core                 :as r]
            [dom-utils.interface          :refer [input-value]]
            [string-utils.interface       :refer [->str]]))

(defn- workflow-select-header [{:keys [icon header description]}]
  [:div.workflow-select__header
   [:div.workflow-select__header__title
    [c/tab {:variant   "outline-primary"
           :selected? true
           :label     @(<t "behaveplus:working_area")}]]
   [:div.workflow-select__header__content
    [c/icon {:icon-name icon}]
    [:div
     [:h3 header]
     [:p description]]]])

(defn workflow-select [_params]
  (let [*workflow (rf/subscribe [:state [:worksheet :*workflow]])]
    [:<>
     [:div.workflow-select
      [workflow-select-header
       {:icon        "existing-run" ;TODO update when LOGO is available
        :header      @(<t (bp "welcome_message"))
        :description @(<t (bp "please_select_a_work_style"))}]
      [:div.workflow-select__content
       [c/card-group {:on-select      #(rf/dispatch [:state/set [:worksheet :*workflow] (:workflow %)])
                      :flex-direction "column"
                      :card-size      "large"
                      :cards          [{:title     @(<t "behaveplus:workflow:guided_title")
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
(defn independent-worksheet-page [_params]
  (let [*modules   (rf/subscribe [:state [:worksheet :*modules]])
        *submodule (rf/subscribe [:worksheet/first-output-submodule-slug (first @*modules)])
        name       (rf/subscribe [:state [:worksheet :name]])]
    [:div.workflow-select
     [workflow-select-header
      {:icon        "modules"
       :header      @(<t (bp "module_selection"))
       :description @(<t (bp "please_select_from_the_following_options"))}]
     [:div.workflow-select__content
      [c/card-group {:on-select      #(rf/dispatch [:state/set [:worksheet :*modules] (:module %)])
                     :flex-direction "row"
                     :cards          [{:order     1
                                       :title     @(<t (bp "surface_and_crown"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "surface"}
                                                   {:icon-name "crown"}]
                                       :selected? (= @*modules [:surface :crown])
                                       :module    [:surface :crown]}
                                      {:order     2
                                       :title     @(<t (bp "surface_only"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "surface"}]
                                       :selected? (= @*modules [:surface])
                                       :module    [:surface]}
                                      {:order     3
                                       :title     @(<t (bp "surface_and_contain"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "surface"}
                                                   {:icon-name "contain"}]
                                       :selected? (= @*modules [:surface :contain])
                                       :module    [:surface :contain]}
                                      {:order     4
                                       :title     @(<t (bp "surface_and_mortality"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "surface"}
                                                   {:icon-name "mortality"}]
                                       :selected? (= @*modules [:surface :mortality])
                                       :module    [:surface :mortality]}
                                      {:order     5
                                       :title     @(<t (bp "mortality_only"))
                                       :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                       :icons     [{:icon-name "mortality"}]
                                       :selected? (= @*modules [:mortality])
                                       :module    [:mortality]}]}]
      [:div.workflow-select__content__name

       [c/text-input {:label     "Worksheet Name"
                      :on-change #(rf/dispatch [:state/set [:worksheet :name] (input-value %)])}]]]
     [wizard-navigation {:next-label     @(<t (bp "next"))
                         :back-label     @(<t (bp "back"))
                         :next-disabled? (some empty? [@name @*modules])
                         :on-back        #(.back js/history)
                         :on-next        #(do
                                            ;; Generate UUID
                                            (let [ws-uuid (str (squuid))]

                                              ;; Create the Worksheet
                                              (rf/dispatch [:worksheet/new {:name @name :modules (vec @*modules) :uuid ws-uuid}])

                                              ;; Look at modules that user has selected, find the first output submodule
                                              (rf/dispatch [:navigate (str "/worksheets/" ws-uuid "/modules/" (->str (first @*modules)) "/output/" @*submodule)])))}]]))

(defn guided-worksheet-page [_params]
  [:<>
   [:div.workflow-select
    [:div.workflow-select__header
     [:h3 "TODO: FLESH OUT GUIDED WORKSHEET"]]]])

(defn import-worksheet-page [_params]
  (let [file (r/track #(or @(rf/subscribe [:state [:worksheet :file]])
                           @(<t (bp "select_a_file"))))]
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
                           :on-next    #(rf/dispatch [:navigate "/worksheets/1/modules/contain/output/fire"])}]]]]))

;; TODO use title
(defn new-worksheet-page [params]
  (let [title @(<t "behaveplus:working_area")]
    [workflow-select params]))
