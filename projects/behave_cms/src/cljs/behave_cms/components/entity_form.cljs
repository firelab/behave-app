(ns behave-cms.components.entity-form
  (:require [clojure.string :as str]
            [reagent.core      :as r]
            [re-frame.core     :as rf]
            [string-utils.interface :refer [->kebab ->str]]
            [behave.schema.core :refer [all-schemas]]
            [behave-cms.components.common :refer [dropdown]]
            [behave-cms.utils  :as u]))

;;; Constants
(def ^:private db-attrs             (map :db/ident all-schemas))
(def ^:private db-translation-attrs (->> db-attrs
                                         (filter #(-> %
                                                      (->str)
                                                      (str/ends-with? "translation-key")))
                                         (set)))
(def ^:private db-help-attrs        (->> db-attrs
                                         (filter #(-> %
                                                      (->str)
                                                      (str/ends-with? "help-key")))
                                         (set)))

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
           {parent-field     parent-id}
           (when (db-translation-attrs translation-attr) {translation-attr translation-key})
           (when (db-help-attrs help-attr) {help-attr help-key})
           ;; Prevent overwriting the translation/help keys once assigned
           (select-keys original [translation-attr help-attr]))))

;;; Sub-components

(defmulti field-input (fn [{type :type}] type))

(defmethod field-input :select [{:keys [label options on-change state]}]
  [:div.mb-3
   [dropdown
    {:label     label
     :options   options
     :on-select #(on-change (u/input-value %))
     :selected  @state}]])

(defmethod field-input :checkbox [{:keys [label options on-change state]}]
  (let [group-label label]
    [:div.mb-3
     [:label.form-label group-label]
     (doall
      (for [{:keys [label value]} options]
        (let [id       (u/sentence->kebab (str group-label ":" value))
              checked? (if (string? @state)
                         (seq @state)
                         @state)]
          ^{:key id}
          [:div.form-check
           [:input.form-check-input
            {:type      "checkbox"
             :id        id
             :checked   checked?
             :on-change #(on-change (.. % -target -checked))}]
           [:label.form-check-label {:for id} label]])))]))

(defmethod field-input :radio [{:keys [label options on-change state]}]
  (let [group-label label]
    [:div.mb-3
     [:label.form-label group-label]
     [:input {:type "hidden" :value (str @state)}]
     (doall
      (for [{:keys [label value]} options]
        (let [id (str value)]
          ^{:key id}
          [:div.form-check
           [:input.form-check-input
            {:type      "radio"
             :name      (u/sentence->kebab group-label)
             :id        id
             :value     value
             :checked   (= @state value)
             :on-change #(on-change value)}]
           [:label.form-check-label {:for id} label]])))]))

(defmethod field-input :number [{:keys [label autocomplete disabled? autofocus? required? placeholder on-change state]
                                 :or   {disabled? false required? false}}]
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
  "Supports simple forms to add/edit a new entity. Takes a map with the keys:
  - :entity         Used to create an editor in the app db ({:state {:editor {<:entity> ...}}})
  - :parent-field   Used in association with `:parent-id` to create a reverse parent relation
                    (e.g. A blog with many posts would `:parent-field` `:blog/_posts`,
                    with `:parent-id` set to `<blog-entity-id>` in DataScript.)
  - :parent-id      Used in association with `:parent-field` to create a reverse parent relation.
  - :fields         A vector of field maps. Uses text fields by default. Field can have a `:type`
                    of: `:number` `:radio`, `:checkbox`.
  - :on-create      Takes a function which is passed the state in the editor, whose result is
                    then to the default `:api/create-entity` event. Used to perform minor changes
                    before an entity is created.

  For example:
  ```clj
  [entity-form {:entity        :submodule
                :parent-field  :module/_submodules
                :parent-id     module-id
                :id            id
                :fields        [{:label     \"Name\"
                                 :required? true
                                 :field-key :submodule/name}
                                {:label     \"I/O\"
                                 :field-key :submodule/io
                                 :type      :radio
                                 :options   [{:label \"Input\" :value :input}
                                             {:label \"Output\" :value :output}]}]
                :on-create     #(assoc % :submodule/order num-submodules)})
  ```"
  [{:keys [entity parent-field parent-id fields id on-create] :as opts}]
  (let [original     @(rf/subscribe [:entity id])
        parent       @(rf/subscribe [:entity parent-id])
        update-state (fn [field] (fn [value] (rf/dispatch [:state/set-state [:editors entity field] value])))
        get-state    (fn [field] (r/track #(let [result (cond
                                                          (not (nil? @(rf/subscribe [:state [:editors entity field]])))
                                                          @(rf/subscribe [:state [:editors entity field]])

                                                          (not (nil? (get original field)))
                                                          (get original field)

                                                          :else
                                                          "")]
                                            result)))
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
