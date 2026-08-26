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
  "Calls `f` on Escape while `child` is mounted."
  [f _child]
  (let [handler #(when (escape-key? %) (f))]
    (r/create-class
     {:component-did-mount    #(.addEventListener js/document "keydown" handler)
      :component-will-unmount #(.removeEventListener js/document "keydown" handler)
      :reagent-render         (fn [_f child] child)})))

(defn modal
  "A modal dialog.

  Options: `:title`, `:icon`, `:close-on-click`, `:buttons`, `:content`, `:size`
  (`:medium`, `:large` or `:fullscreen`), `:dismiss-on-backdrop?` (default true)
  and `:dismiss-on-escape?` (defaults to `:dismiss-on-backdrop?`)."
  [{icon-opts :icon
    :keys     [title close-on-click buttons content size
               dismiss-on-backdrop? dismiss-on-escape?]
    :or       {size                 :medium
               dismiss-on-backdrop? true}}]
  (let [buttons          (js->clj buttons :keywordize-keys true)
        icon-opts        (js->clj icon-opts :keywordize-keys true)
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
                           [header icon-opts close-on-click title]
                           [:div {:class "modal__body"} content]
                           (when (seq buttons)
                             [:div {:class "modal__buttons"}
                              (for [btn buttons]
                                [button btn])])]]]
    (if escape-handler
      [on-escape escape-handler body]
      body)))
