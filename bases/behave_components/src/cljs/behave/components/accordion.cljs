(ns behave.components.accordion
  (:require [behave.components.icon.core :refer [icon]]))

(defn- toggle []
  [:div {:class "accordion-toggle-vertical-bar"}
   [icon {:icon-name "minus"}]
   [:div {:class "accordion-toggle-horizontal-bar"}
    [icon {:icon-name "minus"}]]])

(defn- accordion-item [id label content default-open?]
  [:div {:class "accordion__item"}
   [:input {:id              id
            :class           "accordion-toggle"
            :type            "checkbox"
            :default-checked default-open?}]
   [:label {:class "accordion__item__label" :for id} label
    [toggle]]
   [:div {:class ["accordion__item__collapse"]}
    [:div {:class "accordion__item__body"}
     content]]])

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
