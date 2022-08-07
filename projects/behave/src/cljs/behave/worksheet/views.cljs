(ns behave.worksheet.views
  (:require [re-frame.core :as rf]
            [behave.components.core :as c]
            [behave.translate       :refer [<t]]))

(defn workflow-select [params]
  (let [selected-workflow (rf/subscribe [:state :selected-workflow])]
    [:div.workflow-select
     [:div.workflow-select__header
      [:h3 @(<t "behaveplus:welcome_message")]
      [:p @(<t "behaveplus:please_select_a_work_style")]]
     [:div.workflow-select__content
      [c/card-group {:on-select   #(rf/dispatch [:state/set :selected-workflow (:workflow %)])
                     :selected-fn #(= @selected-workflow (:workflow %))
                     :cards       [{:title    @(<t "behaveplus:workflow:guided_title")
                                    :content  @(<t "behaveplus:workflow:guided_desc")
                                    :order    0
                                    :workflow :guided}
                                   {:title    @(<t "behaveplus:workflow:independent_title")
                                    :content  @(<t "behaveplus:workflow:independent_desc")
                                    :order    1
                                    :workflow :independent}
                                   {:title    @(<t "behaveplus:workflow:import_title")
                                    :content  @(<t "behaveplus:workflow:import_desc")
                                    :order    2
                                    :workflow :import}]}]]]))

(defn worksheet-page
  [params]
  (let [working-area (<t "behaveplus:working_area")]
    [:div.working-area
     [:div.working-area__title @working-area]
     [:div.working-area__area
      [workflow-select params]]]))
