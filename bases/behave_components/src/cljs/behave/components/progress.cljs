(ns behave.components.progress
  (:require [behave.components.a11y :refer [on-space-enter]]))

(defn progress-nob [{:keys [selected? label on-select] :as s}]
  [:div {:on-click     #(on-select s)
         :tabindex     0
         :on-key-press (on-space-enter #(on-select s))
         :class        ["progress-nob"
                        (when selected? " progress-nob--selected")]}
   [:div {:class "progress-nob__circle"}
    [:div {:class "progress-nob__circle__dot"}]]
   [:div {:class "progress-nob__label"} label]])

(defn progress [{:keys [steps on-select completed-last-step-id]}]
  (let [steps (sort-by :step-id steps)]
    [:div {:class "progress"}
     [:div {:class "progress__header-bar"}]
     (for [{:keys [step-id completed?] :as s} (butlast steps)]
       [:div {:key   step-id
              :class ["progress__step"
                      (when (= completed-last-step-id step-id) "progress__step--completed-last")
                      (when completed? " progress__step--completed")]}
        [progress-nob (cond-> s
                        (nil? (:on-select s))
                        (assoc :on-select on-select))]])
     (let [{:keys [step-id completed?] :as s} (last steps)]
       [:div {:key   step-id
              :class ["progress__step"
                      "progress__step-last"
                      (when completed? " progress__step--completed")]}
        [progress-nob (cond-> s
                        (nil? (:on-select s))
                        (assoc :on-select on-select))]])
     [:div {:class "progress__footer-bar-1"}]
     [:div {:class "progress__footer-bar-2"}]]))
