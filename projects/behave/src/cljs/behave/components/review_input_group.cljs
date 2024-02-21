(ns behave.components.review-input-group
  (:require [reagent.core            :as r]
            [re-frame.core           :as rf]
            [behave.components.core  :as c]
            [behave.translate        :refer [<t bp]]
            [clojure.string :as str]))

(defmulti wizard-input (fn [variable _ _ _ _] (if (:group-variable/discrete-multiple? variable)
                                                :multi-discrete
                                                (:variable/kind variable))))

(defmethod wizard-input :continuous [{gv-uuid  :bp/uuid
                                      help-key :group-variable/help-key}
                                     ws-uuid
                                     group-uuid
                                     repeat-id
                                     _repeat-group?
                                     edit-route]
  (let [values      (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])
        warn-limit? (true? @(rf/subscribe [:state :warn-multi-value-input-limit]))]
    [:div.wizard-input
     [:div.wizard-review__input
      {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
      [c/text-input {:label     @(rf/subscribe [:wizard/gv-uuid->variable-name-1 gv-uuid])
                     :value     @values
                     :error?    warn-limit?
                     :disabled? true}]
      [c/button {:variant  "primary"
                 :label    @(<t (bp "change_values"))
                 :size     "small"
                 :on-click #(rf/dispatch [:wizard/edit-input edit-route repeat-id gv-uuid])}]]]))

(defmethod wizard-input :discrete [{gv-uuid  :bp/uuid
                                    help-key :group-variable/help-key
                                    eid      :db/id}
                                   ws-uuid
                                   group-uuid
                                   repeat-id
                                   _repeat-group?
                                   edit-route]
  (let [*value               (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])
        *resolved-enum-value (rf/subscribe [:worksheet/resolve-enum-value eid @*value])]
    [:div.wizard-input {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [:div.wizard-review__input
      [:div.wizard-review__input--discrete
       [:div.wizard-review__input--discrete__label (str @(rf/subscribe [:wizard/gv-uuid->variable-name-1 gv-uuid]) ":")]
       [:div.wizard-review__input--discrete__value @*resolved-enum-value]
       [c/button {:variant  "primary"
                  :label    @(<t (bp "change_selection"))
                  :size     "small"
                  :on-click #(rf/dispatch [:wizard/edit-input edit-route repeat-id gv-uuid])}]]]]))

(defmethod wizard-input :multi-discrete [{gv-uuid  :bp/uuid
                                          help-key :group-variable/help-key
                                          eid      :db/id}
                                         ws-uuid
                                         group-uuid
                                         repeat-id
                                         _repeat-group?
                                         edit-route]
  (let [*value               (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])
        resolved-enum-values (->> (str/split @*value ",")
                                  (map #(deref (rf/subscribe [:worksheet/resolve-enum-value eid %]))))]
    [:div.wizard-input {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [:div.wizard-review__input
      [:div.wizard-review__input--discrete
       [:div.wizard-review__input--discrete__label (str @(rf/subscribe [:wizard/gv-uuid->variable-name-1 gv-uuid]) "s:")]
       [:div.wizard-review__input--multi-discrete
        (for [enum resolved-enum-values]
          [:div.wizard-review__input--discrete__value enum])]
       [c/button {:variant  "primary"
                  :label    @(<t (bp "change_selection"))
                  :size     "small"
                  :on-click #(rf/dispatch [:wizard/edit-input edit-route repeat-id gv-uuid])}]]]]))

(defmethod wizard-input :text [{gv-uuid  :bp/uuid
                                help-key :group-variable/help-key}
                               ws-uuid
                               group-uuid
                               repeat-id
                               repeat-group?
                               edit-route]
  (let [values (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id gv-uuid])]
    [:div.wizard-input--review
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/text-input {:label     @(rf/subscribe [:wizard/gv-uuid->variable-name-1 gv-uuid])
                    :value     @values
                    :disabled? true}]
     (when-not repeat-group?
       [c/button {:variant  "primary"
                  :label    @(<t (bp "change_text"))
                  :size     "small"
                  :on-click #(rf/dispatch [:wizard/edit-input edit-route repeat-id gv-uuid])}])]))

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
                :on-click  #(rf/dispatch [:wizard/edit-input route repeat-id (:bp/uuid variable)])}]]]])

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

(defn input-group [edit-route ws-uuid group variables]
  (r/with-let [variables (sort-by :group-variable/variable-order variables)]
    (when (seq variables)
      [:<>
       (if (:group/repeat? group)
         [repeat-group ws-uuid group variables edit-route]
         [:div.wizard-review-group__inputs
          (for [variable variables]
            ^{:key (:db/id variable)}
            [wizard-input variable ws-uuid (:bp/uuid group) 0 false edit-route])])])))
