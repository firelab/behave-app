(ns behave.components.progress)

(defn progress-nob [{:keys [completed? selected? label]}]
  [:div {:class (str "progress-nob"
                     (when selected? " progress-nob--selected")
                     (when completed? " progress-nob--completed"))}
   [:div {:class "progress-nob__circle"}
    [:div {:class "progress-nob__circle__dot"}]]
   [:div {:class "progress-nob__label"} label]])

(defn progress [{:keys [steps]}]
  [:div {:class "progress"}
   (for [{:keys [step selected? completed?] :as s} (sort-by :step steps)]
     [:div {:key step
            :class (str "progress__step"
                        (when selected? " progress__step--selected")
                        (when completed? " progress__step--completed"))}
      [progress-nob s]])])
