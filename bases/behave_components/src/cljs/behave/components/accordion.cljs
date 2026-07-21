(ns behave.components.accordion
  (:require [behave.components.icon.core :refer [icon]]
            [reagent.core                :as r]))

(defn- toggle []
  [:div {:class "accordion-toggle-vertical-bar"}
   [icon {:icon-name "minus"}]
   [:div {:class "accordion-toggle-horizontal-bar"}
    [icon {:icon-name "minus"}]]])

(defn- accordion-item [id label content default-open?]
  (r/with-let [open? (r/atom default-open?)]
    [:div {:class "accordion__item"}
     [:input {:id              id
              :class           "accordion-toggle"
              :type            "checkbox"
              :default-checked default-open?
              ;; Announced as an expandable button, not a checkbox
              :role            "button"
              :aria-expanded   @open?
              :aria-controls   (str id "-panel")
              :on-change       #(reset! open? (.. % -target -checked))
              ;; Toggle on Enter (Space toggles natively)
              :on-key-down     (fn [e]
                                 (when (= (.-key e) "Enter")
                                   (.click (.-currentTarget e))))}]
     [:label {:class "accordion__item__label" :for id} label
      [toggle]]
     [:div {:class           ["accordion__item__collapse"]
            :id              (str id "-panel")
            :role            "region"
            :aria-labelledby id}
      [:div {:class "accordion__item__body"}
       content]]]))

(defn accordion [{:keys [accordion-items default-open?]
                  :or   {default-open? false}}]
  (let [accordion-items (js->clj accordion-items :keywordize-keys true)]
    [:div {:class (str "accordion")}
     (map-indexed (fn [idx item]
                    (let [{:keys [label content]} item
                          id                      (str "accordion_collapsible" idx)]
                      ^{:key id}
                      [accordion-item id label content default-open?]))
                  accordion-items)]))
