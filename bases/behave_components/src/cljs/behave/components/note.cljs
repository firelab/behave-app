(ns behave.components.note
  (:require [behave.components.button :refer [button]]
            [behave.components.inputs :refer [text-input]]
            [reagent.core :as r]))

(defn note [{:keys [title-label title-placeholder body-placeholder limit on-save
                    title-value body-value]
             :or   {limit 500}}]
  (r/with-let [written-title (if title-value
                               (r/atom title-value)
                               (r/atom ""))
               written-body  (if body-value
                               (r/atom body-value)
                               (r/atom ""))]
    [:div {:class "note"}
     [:div {:class "note__input-title"}
      [text-input {:label       title-label
                   :placeholder title-placeholder
                   :value       @written-title
                   :on-change   #(reset! written-title (.. % -target -value))}]]
     [:textarea {:placeholder   body-placeholder
                 :on-change     #(reset! written-body (.. % -target -value))
                 :default-value @written-body}]
     [:div {:class "note__footer"}
      (str (count @written-body) " / " limit)
      [button {:variant   "secondary"
               :label     "Save"
               :size      "large"
               :disabled? (> (count @written-body) limit)
               :icon-name "save"
               :on-click  #(on-save {:title @written-title
                                     :body  @written-body})}]]]))
