(ns behave.worksheet.views
  (:require [re-frame.core                :as rf]
            [reagent.core                 :as r]
            [string-utils.interface       :refer [->str]]
            [behave.components.core       :as c]
            [behave.components.navigation :refer [wizard-navigation]]
            [behave.translate             :refer [<t bp]]
            [behave.worksheet.events]
            [behave.worksheet.subs]))

(defn workflow-select [params]
  (let [*workflow (rf/subscribe [:state [:worksheet :*workflow]])]
    [:<>
     [:div.workflow-select
      [:div.workflow-select__header
       [:h3 @(<t "behaveplus:welcome_message")]
       [:p @(<t "behaveplus:please_select_a_work_style")]]
      [:div.workflow-select__content
       [c/card-group {:on-select   #(rf/dispatch [:state/set [:worksheet :*workflow] (:workflow %)])
                      :cards       [{:title     @(<t "behaveplus:workflow:guided_title")
                                     :content   @(<t "behaveplus:workflow:guided_desc")
                                     :icons     [{:icon-name "guided-work"}]
                                     :selected? (= @*workflow :guided)
                                     :order     0
                                     :workflow  :guided}
                                    {:title     @(<t "behaveplus:workflow:independent_title")
                                     :content   @(<t "behaveplus:workflow:independent_desc")
                                     :icons     [{:icon-name "independent-work"}]
                                     :selected? (= @*workflow :independent)
                                     :order     1
                                     :workflow  :independent}
                                    {:title     @(<t "behaveplus:workflow:import_title")
                                     :content   @(<t "behaveplus:workflow:import_desc")
                                     :icons     [{:icon-name "existing-run"}]
                                     :selected? (= @*workflow :import)
                                     :order     2
                                     :workflow  :import}]}]]
      [wizard-navigation {:next-label "Next"
                          :on-next #(rf/dispatch [:navigate (str "/worksheets/" (->str @*workflow))])}]]]))

(defn independent-worksheet-page [params]
  (let [*modules (rf/subscribe [:state [:worksheet :modules]])]
    [c/accordion
     {:title @(<t "behaveplus:working_area")}
     [:<>
      [:div.workflow-select
       [:div.workflow-select__header
        [:h3 @(<t "behaveplus:module_selection")]
        [:p @(<t "behaveplus:please_select_from_the_following_options")]]
       [:div.workflow-select__content
        [c/card-group {:on-select   #(rf/dispatch [:state/set [:worksheet :modules] (:module %)])
                       :cards       [{:title     @(<t (bp "surface_and_crown"))
                                      :content   "Lorem Ipsum"
                                      :selected? (contains? @*modules :crown)
                                      :icons     [{:icon-name "surface"} {:icon-name "crown"}]
                                      :order     0
                                      :module    #{:surface :crown}}
                                     {:title     @(<t (bp "surface_only"))
                                      :content   "Lorem Ipsum"
                                      :selected? (and (= 1 (count @*modules)) (contains? @*modules :surface))
                                      :icons     [{:icon-name "surface"}]
                                      :order     1
                                      :module    #{:surface}}
                                     {:title     @(<t (bp "surface_and_contain"))
                                      :content   "Lorem Ipsum"
                                      :selected? (contains? @*modules :contain)
                                      :icons     [{:icon-name "surface"} {:icon-name "contain"}]
                                      :order     2
                                      :module    #{:surface :contain}}
                                     {:title     @(<t (bp "surface_and_mortality"))
                                      :content   "Lorem Ipsum"
                                      :selected? (and (contains? @*modules :mortality) (contains? @*modules :surface))
                                      :icons     [{:icon-name "surface"} {:icon-name "mortality"}]
                                      :order     3
                                      :module    #{:surface :mortality}}
                                     {:title     @(<t (bp "mortality_only"))
                                      :content   "Lorem Ipsum"
                                      :selected? (and (= 1 (count @*modules)) (contains? @*modules :mortality))
                                      :icons     [{:icon-name "mortality"}]
                                      :order     4
                                      :module    #{:mortality}}]}]]
       [wizard-navigation {:next-label @(<t (bp "next"))
                           :back-label @(<t (bp "back"))
                           :on-back #(.back js/history)
                           :on-next #(rf/dispatch [:navigate "/worksheets/1/modules/contain/output/fire"])}]]]]))

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

(defn new-worksheet-page [params]
  [c/accordion
   {:title @(<t "behaveplus:working_area")}
   [:<>
    [workflow-select params]]])

