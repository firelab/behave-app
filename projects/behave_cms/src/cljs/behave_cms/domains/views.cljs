(ns behave-cms.domains.views
  (:require [re-frame.core                     :as rf]
            [behave-cms.components.common      :refer [simple-table]]
            [behave-cms.components.entity-form :refer [entity-form]]
            [behave-cms.events]
            [behave-cms.subs]))

;; Domain
(defn- domain-editor [domain-set-eid domain-eid]
  (let [dimensions-options (rf/subscribe [:domains/options :dimension/name])
        units-options      (rf/subscribe [:domains/options :unit/name])]
    [:<>
     [:h3 (if domain-eid "Edit Domain" "Add Domain")]
     [entity-form {:entity       :domain
                   :parent-field :domain-set/_domains
                   :parent-id    domain-set-eid
                   :id           domain-eid
                   :fields       [{:label     "Name"
                                   :required? true
                                   :field-key :domain/name}

                                  {:label     "Decimals"
                                   :required? true
                                   :field-key :domain/decimals}

                                  {:label     "Dimension"
                                   :type      :select
                                   :required? true
                                   :field-key :domain/dimension-uuid
                                   :options   @dimensions-options}

                                  {:label     "Native Unit"
                                   :type      :select
                                   :required? true
                                   :field-key :domain/native-unit-uuid
                                   :options   @units-options}

                                  {:label     "English Unit"
                                   :type      :select
                                   :required? true
                                   :field-key :domain/english-unit-uuid
                                   :options   @units-options}

                                  {:label     "Metric Unit"
                                   :type      :select
                                   :required? true
                                   :field-key :domain/metric-unit-uuid
                                   :options   @units-options}]
                   :on-create #(cond-> %
                                 (:domain/decimals %)
                                 (update :domain/decimals long))}]]))

(defn- domain-table [domain-set-eid]
  (when domain-set-eid
    (let [domain    (rf/subscribe [:pull-children :domain-set/domains domain-set-eid])
          on-select #(rf/dispatch [:state/select :domain (:db/id %)])
          on-delete
          #(when (js/confirm (str "Are you sure you want to delete the domain " (:domain/name %) "?"))
             (rf/dispatch [:api/delete-entity %]))]
      (prn "domain:" @domain)
      [:div
       {:style {:height "400px" :overflow-y "scroll"}}
       [simple-table
        [:domain/name]
        (sort-by :domain/name @domain)
        {:on-select on-select
         :on-delete on-delete}]])))

;; Domain Set

(defn domain-set-table []
  (let [domain    (rf/subscribe [:pull-with-attr :domain/name '[*]])
        on-select #(rf/dispatch [:state/set-state :domain %])
        on-delete #(when (js/confirm (str "Are you sure you want to delete the domain " (:domain/name %) "?"))
                     (rf/dispatch [:api/delete-entity %]))]
    [simple-table
     [:dimension-set/name]
     (sort-by :domain/name @domain)
     {:on-select on-select
      :on-delete on-delete}]))

(defn- domain-set-editor [domain-set-eid domain-eid]
  [:<>
   [:div.row
    [:h3 (str (if domain-set-eid "Edit" "Add") " Domain Set")]]
   [:div.row
    [:div.col-6
     [entity-form {:entity    :domain-sets
                   :id        domain-set-eid
                   :disabled? (boolean domain-eid)
                   :fields    [{:label     "Name"
                                :required? true
                                :field-key :domain-set/name}]}]]]])

(defn- domain-sets-table []
  (let [domain-set (rf/subscribe [:pull-with-attr :domain-set/name])
        on-select  #(rf/dispatch [:state/select :domain-set (:db/id %)])
        on-delete  #(when (js/confirm (str "Are you sure you want to delete the domain set " (:domain-set/name %) "?"))
                      (rf/dispatch [:api/delete-entity %]))]
    [:div
     {:style {:height "400px" :overflow-y "scroll"}}
     [simple-table
      [:domain-set/name]
      (sort-by :domain-set/name @domain-set)
      {:on-select on-select
       :on-delete on-delete}]]))

(defn domains-page [_]
  (let [*domain-set (rf/subscribe [:selected :domain-set])
        *domain     (rf/subscribe [:selected :domain])]
    [:div.container
     [:div.row.mt-3
      [:h3 "Domain Sets"]
      [:div.row
       [:div.col-6
        [domain-sets-table]]
       [:div.col-6
        [:h3 "Domains"]
        [domain-table @*domain-set]]]]
     [:div.row.mt-3
      [:div.col-6
       [domain-set-editor @*domain-set @*domain]]
      [:div.col-6
       (when @*domain-set
         [domain-editor @*domain-set @*domain])]]]))
