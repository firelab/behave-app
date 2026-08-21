(ns behave.components.modal
  (:require [behave.components.button    :refer [button]]
            [behave.components.icon.core :refer [icon]]
            [reagent.core                :as r]))

(defn- header [{:keys [icon-name]} close-on-click title]
  [:div {:class "modal__header"}
   (when icon-name
     [:div {:class "modal__icon"}
      [icon icon-name]])
   [:div {:class "modal__title"} title
    [:div {:class "modal__close"}
     [button {:icon-name "close"
              :on-click  close-on-click
              :shape     "round"
              :size      "small"
              :variant   "primary"}]]]])

(defn- escape-key? [event]
  (contains? #{"Escape" "Esc"} (.-key event)))

(defn- on-escape
  "Wraps `child` so that `f` is called whenever Escape is pressed while it is
  mounted. The listener is removed on unmount."
  [f _child]
  (let [handler #(when (escape-key? %) (f))]
    (r/create-class
     {:component-did-mount    #(.addEventListener js/document "keydown" handler)
      :component-will-unmount #(.removeEventListener js/document "keydown" handler)
      :reagent-render         (fn [_f child] child)})))

(defn modal
  "A modal dialog.

  Options:
  - `:title`                text shown in the header
  - `:icon`                 optional `{:icon-name \"…\"}` shown left of the title
  - `:close-on-click`       called when the user closes the modal
  - `:buttons`              optional footer buttons
  - `:content`              the modal's body
  - `:size`                 `:medium` (default), `:large` or `:fullscreen`,
                            controlling how much of the viewport the modal takes
  - `:dismiss-on-backdrop?` close when the backdrop is clicked (default true)
  - `:dismiss-on-escape?`   close when Escape is pressed (defaults to
                            `:dismiss-on-backdrop?`, so a modal that must be
                            acknowledged stays put either way)"
  [{:keys [title icon close-on-click buttons content size
           dismiss-on-backdrop? dismiss-on-escape?]
    :or   {size                 :medium
           dismiss-on-backdrop? true}}]
  (let [buttons          (js->clj buttons :keywordize-keys true)
        icon             (js->clj icon :keywordize-keys true)
        dismiss-on-esc?  (if (some? dismiss-on-escape?)
                           dismiss-on-escape?
                           dismiss-on-backdrop?)
        backdrop-handler (when (and dismiss-on-backdrop? close-on-click) close-on-click)
        escape-handler   (when (and dismiss-on-esc? close-on-click) close-on-click)
        body             [:<>
                          [:div (cond-> {:class "modal__background"}
                                  backdrop-handler (assoc :on-click backdrop-handler))]
                          [:div {:class      ["modal" (str "modal--" (name size))]
                                 :role       "dialog"
                                 :aria-modal "true"
                                 :aria-label title}
                           [header icon close-on-click title]
                           [:div {:class "modal__body"} content]
                           (when (seq buttons)
                             [:div {:class "modal__buttons"}
                              (for [btn buttons]
                                [button btn])])]]]
    (if escape-handler
      [on-escape escape-handler body]
      body)))
