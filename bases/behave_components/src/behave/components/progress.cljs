(ns behave.components.progress)

(defn progress-nob [{:keys [selected? label on-select] :as s}]
  [:div {:on-click #(on-select s)
         :class    ["progress-nob"
                    (when selected? " progress-nob--selected")]}
   [:div {:class "progress-nob__circle"}
    [:div {:class "progress-nob__circle__dot"}]]
   [:div {:class "progress-nob__label"} label]])

(defn progress [{:keys [steps on-select]}]
  [:div {:class "progress"}
   [:div {:class "progress__header-bar"}]
   (for [{:keys [step-id selected? completed?] :as s} (sort-by :step-id steps)]
     [:div {:key   step-id
            :class ["progress__step"
                    (when selected? " progress__step--selected")
                    (when completed? " progress__step--completed")]}
      [progress-nob (assoc s :on-select on-select)]])
   [:div {:class "progress__footer-bar-1"}]
   [:div {:class "progress__footer-bar-2"}]])
