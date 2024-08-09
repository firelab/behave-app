(ns behave.components.tab
  (:require [behave.components.icon.core :refer [icon]]))

(defn tab [{:keys [label variant selected? on-click icon-name disabled? flat-edge icon-position]
            :or   {flat-edge     "bottom"
                   icon-position "left"}
            :as t}]
  [:div {:class ["tab"
                 (when variant (str "tab--" variant))
                 (when selected? "tab--selected")]}
   [:button {:class    ["tab__button"
                        (when variant (str "tab__button--" variant))
                        (when flat-edge (str "tab__button--flat-edge-" flat-edge))
                        (when selected? (str "tab__button--selected"))]
             :disabled disabled?
             :on-click #(on-click t)}
    (when (and icon-name (= icon-position "left"))
      [:div {:class "button__icon"}
       [icon icon-name]])
    (when (seq label)
      [:div {:class "button__label"} label])
    (when (and icon-name (= icon-position "right"))
      [:div {:class "button__icon"}
       [icon icon-name]])]])

(defn tab-group [{:keys [tabs variant flat-edge on-click align]
                  :or   {variant   "primary"
                         flat-edge "bottom"
                         align     "left"}}]
  (let [tabs (js->clj tabs :keywordize-keys true)]
    [:div {:class ["tab-group"
                   (str "tab-group--" variant)
                   (str "tab-group--flat-edge-" flat-edge)
                   (str "tab-group--align-" align)]}
     (for [t (sort-by :order-id tabs)]
       [tab (merge t {:variant   variant
                      :flat-edge flat-edge
                      :on-click  on-click})])]))
