(ns behave-cms.components.entity-form
  (:require [clojure.string :as str]
            [datascript.core   :refer [squuid]]
            [reagent.core      :as r]
            [re-frame.core     :as rf]
            [string-utils.interface :refer [->kebab ->str]]
            [behave-cms.routes :refer [singular]]
            [behave-cms.utils  :as u]))

;;; Helpers

(defn- upsert-entity! [data]
  (let [rf-event (if (nil? (:db/id data)) :api/create-entity :api/update-entity)]
    (rf/dispatch [rf-event data])))

(defn- parent-translation-key
  "Gets the translation key from `:<parent>/translation-key`,
  `:<parent>/help-key`, or generates it from the `<parent>/name` attribute."
  [parent]
  (let [attrs      (map ->str (keys parent))
        h-or-t-key (->> attrs
                        (filter #(or (str/ends-with? % "/translation-key")
                                     (str/ends-with? % "/help-key")))
                        (first)
                        (keyword))
        name-key   (->> attrs
                        (filter #(str/ends-with? % "/name"))
                        (first)
                        (keyword))
        name-kebab (->kebab (get parent name-key))]
    (str/replace (get parent h-or-t-key name-kebab) #":help$" "")))

(defn- merge-parent-fields [state original entity parent-field parent-id parent]
  (let [gen-attr           #(keyword (str (->str entity) "/" %))
        name-attr          (gen-attr "name")
        translation-attr   (gen-attr "translation-key")
        help-attr          (gen-attr "help-key")
        parent-translation (parent-translation-key parent)
        translation-key    (str parent-translation ":" (->kebab (get state name-attr)))
        help-key           (str translation-key ":help")]

    (merge state
           {parent-field     parent-id
            translation-attr translation-key
            help-attr        help-key}
           ;; Prevent overwriting the translation/help keys once assigned
           (select-keys original [translation-attr help-attr]))))

(defn entity-uuid-kw [entity-type]
  (-> entity-type
      (singular)
      (name)
      (str "-uuid")
      (keyword)))

;;; Sub-components

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


;;; Public Fns

(defn entity-form
  ""
  [{:keys [entity parent-field parent-id fields id on-create] :as opts}]
  (let [original     @(rf/subscribe [:entity id])
        parent       @(rf/subscribe [:entity parent-id])
        update-state (fn [field] (fn [value] (rf/dispatch [:state/set-state [:editors entity field] value])))
        get-state    (fn [field] (r/track #(or @(rf/subscribe [:state [:editors entity field]]) (get original field) "")))
        on-submit    (u/on-submit #(let [state @(rf/subscribe [:state [:editors entity]])]
                                     (cond-> state
                                       id
                                       (merge {:db/id id})

                                       (and (nil? id) parent-field parent-id)
                                       (merge-parent-fields original entity parent-field parent-id parent)

                                       (and (nil? id) (fn? on-create))
                                       (on-create)

                                       true
                                       (upsert-entity!))
                                     (rf/dispatch [:state/set-state entity nil])
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
