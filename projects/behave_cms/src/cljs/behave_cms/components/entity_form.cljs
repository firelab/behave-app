(ns behave-cms.components.entity-form
  (:require [datascript.core   :refer [squuid]]
            [reagent.core      :as r]
            [re-frame.core     :as rf]
            [behave-cms.routes :refer [singular]]
            [behave-cms.utils  :as u]))

(defn dropdown
  "A component for dropdowns."
  [label options value-id on-select]
  [:div.mb-3
   [:label.form-label label]
   [:select.form-select {:on-change on-select}
    [:option {:key "none" :value nil :selected? true}
     (str "Select " label "...")]
    (for [[uuid {value value-id}] options]
      ^{:key uuid}
      [:option {:key uuid :value uuid} value])]])

(defn entity-selector [entity-label entity-type name-key & [data]]
  (let [_        (when (or (nil? data) (some vals data))
                   (rf/dispatch [:api/entities data]))
        entities (rf/subscribe [:entities entity-type])]
    [dropdown
     entity-label
     @entities
     name-key
     #(rf/dispatch [:state/set-state (singular entity-type) (u/input-value %)])]))

(defn- upsert-entity! [data]
  (let [rf-event (if (nil? (:db/id data)) :api/create-entity :api/update-entity)]
    (rf/dispatch [rf-event data])))

(defmulti field-input (fn [{type :type}] type))

(defmethod field-input :checkbox [{:keys [label options on-change state]}]
  (let [group-label label]
    [:div.mb-3
     [:label.form-label group-label]
     (for [{:keys [label value]} options]
       (let [id (u/sentence->kebab (str group-label ":" value))]
         ^{:key id}
         [:div.form-check
          [:input.form-check-input
           {:type      "checkbox"
            :id        id
            :value     value
            :checked   (get @state value)
            :on-change #(on-change (assoc @state value (not (get @state value))))}]
          [:label.form-check-label {:for id} label]]))]))

(defmethod field-input :radio [{:keys [label options on-change state]}]
  (let [group-label label]
    [:div.mb-3
     [:label.form-label group-label]
     [:input {:type "hidden" :value (str @state)}]
     (for [{:keys [label value]} options]
       (let [id (u/sentence->kebab (str group-label ":" value))]
         ^{:key id}
         [:div.form-check
          [:input.form-check-input
           {:type      "radio"
            :name      (u/sentence->kebab group-label)
            :id        id
            :value     (str value)
            :checked   (= @state value)
            :on-change #(on-change value)}]
          [:label.form-check-label {:for id} label]]))]))

(defmethod field-input :number [{:keys [label autocomplete disabled? autofocus? required? placeholder on-change state]
                                  :or {disabled? false required? false}}]
  [:div.my-3
   [:label.form-label {:for (u/sentence->kebab label)} label]
   [:input.form-control
    {:auto-complete autocomplete
     :auto-focus    autofocus?
     :disabled      disabled?
     :required      required?
     :placeholder   placeholder
     :id            (u/sentence->kebab label)
     :type          "number"
     :value         @state
     :on-change     #(on-change (u/input-int-value %))}]])

(defmethod field-input :default [{:keys [type label autocomplete disabled? autofocus? required? placeholder on-change state]
                                  :or {type "text" disabled? false required? false}}]
  [:div.my-3
   [:label.form-label {:for (u/sentence->kebab label)} label]
   [:input.form-control
    {:auto-complete autocomplete
     :auto-focus    autofocus?
     :disabled      disabled?
     :required      required?
     :placeholder   placeholder
     :id            (u/sentence->kebab label)
     :type          type
     :value         @state
     :on-change     #(on-change (u/input-value %))}]])

(defn entity-uuid-kw [entity-type]
  (-> entity-type
      (singular)
      (name)
      (str "-uuid")
      (keyword)))

(defn entity-form
  ""
  [{:keys [entity parent-field parent-id fields id]}]
  (let [original       @(rf/subscribe [:entity id])
        update-state   (fn [field] (fn [value] (rf/dispatch [:state/set-state [:editors entity field] value])))
        get-state      (fn [field] (r/track #(or @(rf/subscribe [:state [:editors entity field]]) (get original field) "")))
        on-submit      (u/on-submit #(let [state @(rf/subscribe [:state [:editors entity]])]
                                       (upsert-entity! (cond id
                                                             (merge {:db/id id} state)

                                                             (and parent-field parent-id)
                                                             (assoc state parent-field parent-id)

                                                             :else state))
                                       (rf/dispatch [:state/set-state (singular entity) nil])
                                       (rf/dispatch [:state/set-state [:editors entity] {}])))]
    [:form {:on-submit on-submit}
     (for [{:keys [field-key] :as field} fields]
       ^{:key field-key}
       [field-input (assoc field
                           :on-change (update-state field-key)
                           :state     (get-state field-key))])
     [:button.btn.btn-sm.btn-outline-primary
      {:type "submit"}
      (if id "Update" "Create")]]))
