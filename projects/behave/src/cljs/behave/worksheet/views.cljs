(ns behave.worksheet.views
  (:require
   [behave.components.core       :as c]
   [behave.components.navigation :refer [wizard-navigation]]
   [behave.translate             :refer [<t bp]]
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
  (let [*modules (rf/subscribe [:state [:worksheet :*modules]])
        title    @(<t "behaveplus:working_area")]
    [:<>
     [:div.workflow-select
      [workflow-select-header]
      [:div.workflow-select__content
       [c/card-group {:on-select #(rf/dispatch [:state/set [:worksheet :*modules] (:module %)])
                      :cards     [{:title     @(<t (bp "contain"))
                                   :selected? (contains? @*modules :contain)
                                   :icon-name "contain"
                                   :order     0
                                   :module    #{:contain}}]}]]
      [wizard-navigation {:next-label @(<t (bp "next"))
                          :back-label @(<t (bp "back"))
                          :on-back    #(.back js/history)
                          :on-next    #(rf/dispatch [:navigate "/worksheets/1/modules/contain/output/fire"])}]]]))

(defn guided-worksheet-page [params]
  [:<>
   [:div.workflow-select
    [:div.workflow-select__header
     [:h3 "TODO: FLESH OUT GUIDED WORKSHEET"]]]])

(defn import-worksheet-page [params]
  [:<>
   [:div.workflow-select
    [:div.workflow-select__header
     [:h3 "TODO: FLESH OUT IMPORT WORKSHEET"]]]])

;; TODO use title
(defn new-worksheet-page [params]
  (let [title @(<t "behaveplus:working_area")]
    [workflow-select params]))
