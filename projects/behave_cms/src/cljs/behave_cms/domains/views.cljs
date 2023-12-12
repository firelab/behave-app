(ns behave-cms.domains.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.common      :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

(def columns [:domain/name])

(defn domains-table []
  (let [domains   (rf/subscribe [:pull-with-attr :domain/name '[*]])
        on-select #(rf/dispatch [:state/set-state :domain %])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the domain " (:domain/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))]
    [simple-table
     columns
     (sort-by :domain/name @domains)
     {:on-select on-select
      :on-delete on-delete}]))

(defn domain-form [domain]
  [:<>
   [:div.row
    [:h3 (if domain (str "Edit " (:domain/name domain)) "Add Domain")]]
   [:div.row
    [:div.col-3
     [entity-form {:entity :domains
                   :id     (when domain (:db/id domain))
                   :fields [{:label     "Name"
                             :required? true
                             :field-key :domain/name}]}]]]])

(defn list-domains-page [_]
  (let [loaded? (rf/subscribe [:state :loaded?])
        *domain (rf/subscribe [:state :domain])]
    (if @loaded?
      [:div.container
       [:div.row.my-3
        [:div.col-12
         [:h3 "Domains"]
         [:div
          {:style {:height "400px" :overflow-y "scroll"}}
          [domains-table]]]]
       [domain-form @*domain]]
      [:div "Loading..."])))
