(ns behave-cms.components.entity-form
  (:require [clojure.string :as str]
            [clojure.set    :as set]
            [reagent.core      :as r]
            [re-frame.core     :as rf]
            [string-utils.interface :refer [->kebab ->str]]
            [behave.schema.core :refer [all-schemas]]
            [behave-cms.components.common :refer [dropdown btn-sm]]
            [behave-cms.utils  :as u]))

;;; Constants
(def ^:private db-attrs                  (map :db/ident all-schemas))
(def ^:private db-cardinality-many-attrs (->> all-schemas
                                              (filter #(= :db.cardinality/many (:db/cardinality %)))
                                              (map :db/ident)
                                              (set)))
(def ^:private db-translation-attrs      (->> db-attrs
                                              (filter #(-> %
                                                           (->str)
                                                           (str/ends-with? "translation-key")))
                                              (set)))
(def ^:private db-help-attrs             (->> db-attrs
                                              (filter #(-> %
                                                           (->str)
                                                           (str/ends-with? "help-key")))
                                              (set)))

;;; Helpers

(defn- cardinality-many-fields? [fields state]
  (let [set-field-keys (->> fields
                            (filter (fn [f] (= :set (:type f))))
                            (map :field-key)
                            (set))
        state-keys     (set (keys state))]

    (and (set/subset? set-field-keys db-cardinality-many-attrs)
         (set/subset? state-keys db-cardinality-many-attrs))))

(defn- gen-cardinality-many-retract-statements [new-data original cardinality-many-attrs]
  (let [{id :db/id} original]
    (->> cardinality-many-attrs
         (map
          (fn [attr]
            (let [new-values (set (get new-data attr))
                  old-values (set (get original attr))]
              (map (fn [v] [:db/retract id attr v])
                   (set/difference old-values new-values)))))
         (apply concat))))

(defn- retract-cardinality-many-values [new-data original]
  (let [cardinality-many-attrs (->> original
                                    (keys)
                                    (filter db-cardinality-many-attrs))]
    (if-not (seq cardinality-many-attrs)
      new-data
      (into [new-data]
            (gen-cardinality-many-retract-statements new-data
                                                     original
                                                     cardinality-many-attrs)))))

(defn- upsert-entity! [data]
  (if (vector? data)
    (rf/dispatch [:ds/transact data])
    (rf/dispatch [:api/upsert-entity data])))

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
        translation-key    (str parent-translation
                                ":"
                                (when (= entity :submodule)
                                  (cond
                                    (= (:submodule/io state) :input)  "input:"
                                    (= (:submodule/io state) :output) "output:"
                                    :else                             nil))
                                (->kebab (get state name-attr)))
        help-key           (str translation-key ":help")]

    (merge state
           {parent-field parent-id}
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

(defmethod field-input :ref-select [{:keys [label options on-change state]}]
  [:div.mb-3
   [dropdown
    {:label     label
     :options   options
     :on-select #(on-change (long (u/input-value %)))
     :selected  (:db/id @state)}]])

(defmethod field-input :set [{:keys [label options on-change state disabled?]
                              :or   {disabled? false}}]
  (let [state-as-set (set (if (-> @state (first) (:db/id))
                            (map :db/id @state)
                            @state))
        group-label  label]
    [:div.mb-3
     [:label.form-label group-label]
     (doall
      (for [{:keys [label value]} options]
        (let [id       (u/sentence->kebab (str group-label ":" value))
              checked? (state-as-set value)]
          ^{:key id}
          [:div.form-check
           {:disabled disabled?}
           [:input.form-check-input
            {:type      "checkbox"
             :disabled  disabled?
             :id        id
             :checked   checked?
             :on-change #(let [enable? (.. % -target -checked)
                               state'  (vec (if enable?
                                              (conj state-as-set value)
                                              (disj state-as-set value)))]
                           (on-change state'))}]
           [:label.form-check-label {:for id} label]])))]))

(defmethod field-input :checkbox [{:keys [label options on-change state disabled?]
                                   :or   {disabled? false}}]
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
           {:disabled disabled?}
           [:input.form-check-input
            {:type      "checkbox"
             :disabled  disabled?
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
                                  :or   {type "text" disabled? false required? false}}]
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

(defmethod field-input :keywords
  [{:keys [label autocomplete disabled? autofocus? required? placeholder on-change state original-keywords
           on-delete-keyword]
    :or   {disabled? false required? false}}]
  [:div.my-3
   [:label.form-label {:for (u/sentence->kebab label)} label]
   [:div.field-input__keywords
    {:style {:display        :flex
             :flex-flow      "row wrap"
             :flex-direction :row
             :margin-bottom  "5px"}}
    (for [kkeyword @original-keywords]
      [:div.text-center
       {:style {:position      "relative"
                :padding       "2px 0px 2px 8px"
                :margin-right  "10px"
                :margin-bottom "2px"
                :border        "2px solid grey"
                :border-radius "10px"}}
       kkeyword
       [btn-sm :close nil #(on-delete-keyword kkeyword)]])]
   [:input.form-control
    {:auto-complete autocomplete
     :auto-focus    autofocus?
     :disabled      disabled?
     :required      required?
     :placeholder   placeholder
     :id            (u/sentence->kebab label)
     :type          type
     :value         @state
     :on-change     #(on-change (keyword (u/input-value %)))}]])

(defmethod field-input :keyword
  [{:keys [label autocomplete disabled? autofocus? required? placeholder on-change state]
    :or   {disabled? false required? false}}]
  [:div.my-3
   [:label.form-label {:for (u/sentence->kebab label)} label]
   [:div.field-input__keywords
    {:style {:display        :flex
             :flex-flow      "row wrap"
             :flex-direction :row
             :margin-bottom  "5px"}}]
   [:input.form-control
    {:auto-complete autocomplete
     :auto-focus    autofocus?
     :disabled      disabled?
     :required      required?
     :placeholder   placeholder
     :id            (u/sentence->kebab label)
     :type          type
     :value         @state
     :on-change     #(on-change (keyword (u/input-value %)))}]])

(defmethod field-input :color
  [{:keys [label disabled? autofocus? required? placeholder on-change state]
    :or   {disabled? false required? false}}]
  [:div.my-3
   [:label.form-label {:for (u/sentence->kebab label)} label]
   [:div.field-input__keywords
    {:style {:display        :flex
             :flex-flow      "row wrap"
             :flex-direction :row
             :margin-bottom  "5px"}}]
   [:input.form-control
    {:auto-focus    autofocus?
     :disabled      disabled?
     :required      required?
     :placeholder   placeholder
     :id            (u/sentence->kebab label)
     :type          "color"
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
                    then to the default `:api/upsert-entity` event. Used to perform minor changes
                    before an entity is created.
  - :on-update      Like `:on-create` but only applies when the entity already exists.

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
  [{:keys [entity parent-field parent-id fields id on-create on-update] :as opts}]
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
        on-submit (u/on-submit #(let [state @(rf/subscribe [:state [:editors entity]])]
                                  (cond-> state
                                    id
                                    (merge {:db/id id})

                                    (and id (fn? on-update))
                                    (on-update)

                                    (and (nil? id) parent-field parent-id)
                                    (merge-parent-fields original entity parent-field parent-id parent)

                                    (and (nil? id) (fn? on-create))
                                    (on-create)

                                    (and id (cardinality-many-fields? fields state))
                                    (retract-cardinality-many-values original)

                                    :always
                                    (upsert-entity!))
                                  (rf/dispatch [:state/set-state entity nil])
                                  (rf/dispatch [:state/set-state [:editors entity] {}])))]
    [:form {:on-submit on-submit}
     (for [{:keys [field-key type] :as field} fields]
       ^{:key field-key}
       (if (= type :keywords)
         [field-input (merge field
                             {:on-change (update-state field-key)
                              :state     (r/track #(let [result (cond
                                                                  (not (nil? @(rf/subscribe [:state [:editors entity field-key]])))
                                                                  @(rf/subscribe [:state [:editors entity field-key]])

                                                                  :else
                                                                  "")]
                                                     result))
                              :on-delete-keyword (fn [kkeyword]
                                                   (rf/dispatch [:api/retract-entity-attr-value
                                                                 id :list-option/tags kkeyword]))
                              :original-keywords (r/track #(get @(rf/subscribe [:entity id]) field-key))})]
         [field-input (assoc field
                             :on-change (update-state field-key)
                             :state     (get-state field-key))]))
     [:button.btn.btn-sm.btn-outline-primary
      {:type "submit"}
      (if id "Update" "Create")]]))
