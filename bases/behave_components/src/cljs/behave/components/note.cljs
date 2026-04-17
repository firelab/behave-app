(ns behave.components.note
  (:require [behave.components.button :refer [button]]
            [behave.components.inputs :refer [text-input]]
            [reagent.core :as r]))

(def ^:private other-option "Other")

(defn- initial-dropdown-val [title-value categories]
  (cond
    (empty? title-value)             ""
    (some #{title-value} categories) title-value
    :else                            other-option))

(defn- initial-custom-val [title-value categories]
  (if (and (seq title-value)
           (not (some #{title-value} categories)))
    title-value
    ""))

(defn note [{:keys [title-label title-placeholder body-placeholder limit on-save
                    title-value body-value categories]
             :or   {limit 500}}]
  (r/with-let [dropdown-val (r/atom (if (seq categories)
                                      (initial-dropdown-val title-value categories)
                                      (or title-value "")))
               custom-val   (r/atom (if (seq categories)
                                      (initial-custom-val title-value categories)
                                      ""))
               written-body (r/atom (or body-value ""))]
    (let [other-selected? (and (seq categories) (= @dropdown-val other-option))
          effective-title  (if other-selected? @custom-val @dropdown-val)]
      [:div {:class "note"}
       [:div {:class "note__input-title"}
        (if (seq categories)
          [:<>
           [:div.note__category-select
            [:label title-label]
            [:select {:value     @dropdown-val
                      :on-change #(reset! dropdown-val (.. % -target -value))}
             [:option {:value "" :disabled true} "Select a category..."]
             (for [category categories]
               ^{:key category}
               [:option {:value category} category])
             [:option {:value other-option} other-option]]]
           (when other-selected?
             [text-input {:label       "Custom Category"
                          :placeholder "Enter category..."
                          :value       @custom-val
                          :on-change   #(reset! custom-val (.. % -target -value))}])]
          [text-input {:label       title-label
                       :placeholder title-placeholder
                       :value       @dropdown-val
                       :on-change   #(reset! dropdown-val (.. % -target -value))}])]
       [:textarea {:placeholder   body-placeholder
                   :on-change     #(reset! written-body (.. % -target -value))
                   :default-value @written-body}]
       [:div {:class "note__footer"}
        (str (count @written-body) " / " limit)
        [button {:variant   "secondary"
                 :label     "Save"
                 :size      "large"
                 :disabled? (or (> (count @written-body) limit)
                                (and other-selected? (empty? @custom-val)))
                 :icon-name "save"
                 :on-click  #(on-save {:title effective-title
                                       :body  @written-body})}]]])))
