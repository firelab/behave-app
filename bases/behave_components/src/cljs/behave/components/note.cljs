(ns behave.components.note
  (:require [behave.components.button :refer [button]]
            [behave.components.inputs :refer [dropdown text-input]]
            [reagent.core :as r]))

(def ^:private other-option "Other")

(defn note [{:keys [title-label title-placeholder body-placeholder limit on-save on-cancel
                    title-value body-value categories category-label other-label
                    cancel-label save-label discard-confirm-message]
             :or   {limit                   500
                    cancel-label            "Cancel"
                    save-label              "Save"
                    discard-confirm-message "Discard unsaved changes to this note?"}}]
  (r/with-let [categories-set  (set (map :value categories))
               category-title? (categories-set title-value)
               other-label     (or other-label other-option)
               dropdown-val    (r/atom (cond
                                         category-title?   title-value
                                         (seq title-value) other-label
                                         :else             ""))
               custom-val      (r/atom (if category-title? "" title-value))
               categories      (concat [{:disabled true
                                         :value    ""
                                         :label    "Select a category..."}]
                                       categories
                                       [{:label other-label :value other-label}])
               written-body    (r/atom (or body-value ""))]
    (let [other-selected? (and (seq categories) (= @dropdown-val other-option))
          effective-title (if other-selected? @custom-val @dropdown-val)
          dirty?          (or (seq effective-title) (seq @written-body))
          cancel!         #(when (or (not dirty?)
                                     (js/confirm discard-confirm-message))
                             (on-cancel))]
      [:div {:class "note"}
       [:div {:class "note__category-select"}
        (if (seq categories)
          [:<>
           [dropdown {:label     (or category-label "Category")
                      :id        "note-category"
                      :value     @dropdown-val
                      :on-change #(reset! dropdown-val (.. % -target -value))
                      :options   categories}]
           [:div {:class "note__category-select__input-title"}
            (when other-selected?
              [text-input {:label       "Custom Category"
                           :placeholder "Enter category..."
                           :value       @custom-val
                           :on-change   #(reset! custom-val (.. % -target -value))}])]]
          [:div {:class "note__category-select__input-title"}
           [text-input {:label       title-label
                        :placeholder title-placeholder
                        :value       @dropdown-val
                        :on-change   #(reset! dropdown-val (.. % -target -value))}]])]
       [:textarea {:placeholder   body-placeholder
                   :on-change     #(reset! written-body (.. % -target -value))
                   :default-value @written-body}]
       [:div {:class "note__footer"}
        (str (count @written-body) " / " limit)
        (when on-cancel
          [button {:variant   "outline-secondary"
                   :label     cancel-label
                   :size      "large"
                   :icon-name "close"
                   :on-click  cancel!}])
        [button {:variant   "secondary"
                 :label     save-label
                 :size      "large"
                 :disabled? (or (> (count @written-body) limit)
                                (empty? effective-title))
                 :icon-name "save"
                 :on-click  #(on-save {:title effective-title
                                       :body  @written-body})}]]])))
