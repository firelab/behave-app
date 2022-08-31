(ns behave.components.tab
  (:require [behave.components.button :refer [button]]))

(defn tab [{:keys [label variant selected? on-click icon-name disabled? flat-edge size]
            :or   {flat-edge "bottom"
                   size      "normal"}
            :as   t}]
  [:div {:class ["tab"
                 (when variant (str "tab--" variant))
                 (when selected? "tab--selected")]}
   [button (cond-> {:variant   variant
                    :label     label
                    :flat-edge flat-edge
                    :size      size
                    :selected? selected?
                    :disabled? disabled?
                    :on-click  #(on-click t)}
             icon-name (assoc :icon-name icon-name :icon-position "left"))]])

(defn tab-group [{:keys [tabs variant flat-edge size on-click align]
                  :or   {variant   "outline-primary"
                         flat-edge "bottom"
                         size      "normal"
                         align     "left"}}]
  (let [tabs (js->clj tabs :keywordize-keys true)]
    [:div {:class ["tab-group"
                   (str "tab-group--" variant)
                   (str "tab-group--flat-edge-" flat-edge)
                   (str "tab-group--align-" align)]}
     (for [t (sort-by :order-id tabs)]
       ^{:key (:order-id t)}
       [tab (merge t {:variant   variant
                      :flat-edge flat-edge
                      :size      size
                      :on-click  on-click})])]))
