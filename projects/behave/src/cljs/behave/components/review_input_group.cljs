(ns behave.components.review-input-group
  (:require [reagent.core            :as r]
            [re-frame.core           :as rf]
            [behave.components.core  :as c]
            [behave.translate        :refer [<t bp]]))

(defmulti wizard-input (fn [variable _ _ _ _] (:variable/kind variable)))

(defmethod wizard-input :continuous [{uuid     :bp/uuid
                                      var-name :variable/name
                                      help-key :group-variable/help-key}
                                     ws-uuid
                                     group-uuid
                                     repeat-id
                                     _repeat-group?
                                     edit-route]
  (let [values      (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])
        warn-limit? (true? @(rf/subscribe [:state :warn-continuous-input-limit]))]
    [:div.wizard-input
     [:div.wizard-review__input
      {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
      [c/text-input {:label     var-name
                     :value     (first @values)
                     :error?    warn-limit?
                     :disabled? true}]
      [c/button {:variant  "primary"
                 :label    @(<t (bp "change_values"))
                 :size     "small"
                 :on-click #(rf/dispatch [:wizard/edit edit-route repeat-id uuid])}]]]))

(defmethod wizard-input :discrete [{uuid     :bp/uuid
                                    var-name :variable/name
                                    help-key :group-variable/help-key}
                                   ws-uuid
                                   group-uuid
                                   repeat-id
                                   _repeat-group?
                                   edit-route]
  (let [values (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])]
    [:div.wizard-input {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [:div.wizard-review__input
      [:div.wizard-review__input--discrete
       [:div.wizard-review__input--discrete__label (str var-name ":")]
       [:div.wizard-review__input--discrete__value (first @values)]
       [c/button {:variant  "primary"
                  :label    @(<t (bp "change_selection"))
                  :size     "small"
                  :on-click #(rf/dispatch [:wizard/edit edit-route repeat-id uuid])}]]]]))

(defmethod wizard-input :text [{uuid     :bp/uuid
                                var-name :variable/name
                                help-key :group-variable/help-key}
                               ws-uuid
                               group-uuid
                               repeat-id
                               repeat-group?
                               edit-route]
  (let [values (rf/subscribe [:worksheet/input ws-uuid group-uuid repeat-id uuid])]
    (println "worksheet/input args: " ws-uuid " " group-uuid " " repeat-id " " uuid)
    [:div.wizard-input--review
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/text-input {:label     var-name
                    :value     (first @values)
                    :disabled? true}]
     (when-not repeat-group?
       [c/button {:variant  "primary"
                  :label    @(<t (bp "change_text"))
                  :size     "small"
                  :on-click #(rf/dispatch [:wizard/edit edit-route repeat-id uuid])}])]))

(defn- repeat-group-input [variable ws-uuid group-uuid repeat-id route]
  [:div.wizard-review-repeat-group__input
   [wizard-input variable ws-uuid group-uuid repeat-id true route]
   [:div.wizard-review-repeat-group__message
    [c/button {:label         @(<t (bp "managing_resources"))
               :variant       "transparent-highlight"
               :icon-name     :help2
               :icon-position "left"}]
    @(<t (bp "you_can_edit_andor_delete_resources"))
    [:div.wizard-review-repeat-group__manage
     [c/button {:variant   "primary"
                :label     @(<t (bp "delete"))
                :size      "small"
                :icon-name "delete"
                :on-click  #(rf/dispatch [:worksheet/delete-repeat-input-group ws-uuid group-uuid repeat-id])}]
     [c/button {:variant   "secondary"
                :label     @(<t (bp "edit"))
                :size      "small"
                :icon-name "edit"
                :on-click  #(rf/dispatch [:wizard/edit route repeat-id (:bp/uuid variable)])}]]]])

(defn repeat-group [ws-uuid group variables edit-route]
  (let [{group-name :group/name
         group-uuid :bp/uuid} group
        *repeat-ids           (rf/subscribe [:worksheet/group-repeat-ids ws-uuid group-uuid])]
    [:<>
     (map-indexed
       (fn [index repeat-id]
         ^{:key repeat-id}
         [:<>
          [:div.wizard-repeat-group
           [:div.wizard-repeat-group__header
            (str group-name " #" (inc index))]]
          [:div.wizard-group__inputs
           (let [variable (first (sort-by :group-variable/variable-order variables))]
             [:div.wizard-input
              [repeat-group-input variable ws-uuid group-uuid repeat-id edit-route]])]])
       @*repeat-ids)
     [:div {:style {:display         "flex"
                    :padding         "20px"
                    :align-items     "center"
                    :justify-content "center"}}]]))

(defn input-group [ws-uuid group variables edit-route]
  (r/with-let [variables (sort-by :group-variable/variable-order variables)]
    [:<>
     (if (:group/repeat? group)
       [repeat-group ws-uuid group variables edit-route]
       [:div.wizard-review-group__inputs
        (for [variable variables]
          ^{:key (:db/id variable)}
          [wizard-input variable ws-uuid (:bp/uuid group) 0 false edit-route])])]))
