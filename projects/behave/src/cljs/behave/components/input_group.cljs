(ns behave.components.input-group
  (:require [reagent.core            :as r]
            [re-frame.core           :as rf]
            [behave.components.core  :as c]
            [dom-utils.interface    :refer [input-value]]
            [string-utils.interface  :refer [->kebab]]))

(defn upsert-input [ws-uuid group-uuid repeat-id gv-uuid value & [units]]
  (rf/dispatch-sync [:worksheet/add-input-group ws-uuid group-uuid repeat-id])
  (rf/dispatch [:worksheet/upsert-input-variable ws-uuid group-uuid repeat-id gv-uuid value units]))

(defmulti wizard-input (fn [variable _ _] (:variable/kind variable)))

(defmethod wizard-input nil [variable] (println [:NO-KIND-VAR variable]))

(defmethod wizard-input :continuous [{uuid          :bp/uuid
                                      var-name      :variable/name
                                      var-max       :variable/maximum
                                      var-min       :variable/minimum
                                      native-units  :variable/native-units
                                      english-units :variable/english-units
                                      metric-units  :variable/metric-units
                                      help-key      :group-variable/help-key}
                                     ws-uuid
                                     group-uuid
                                     repeat-id
                                     repeat-group?]
  (let [value                 (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id uuid])
        value-atom            (r/atom @value)
        warn-limit?           (true? @(rf/subscribe [:state :warn-multi-value-input-limit]))
        acceptable-char-codes (set (map #(.charCodeAt % 0) "0123456789., "))]
    [:div.wizard-input
     [:div.wizard-input__input
      {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
      [c/text-input {:id           (str repeat-id "-" uuid)
                     :label        (if repeat-group? var-name "Values:")
                     :placeholder  (when repeat-group? "Values")
                     :value-atom   value-atom
                     :required?    true
                     :min          var-min
                     :max          var-max
                     :error?       warn-limit?
                     :on-change    #(reset! value-atom (input-value %))
                     :on-key-press (fn [event]
                                     (when-not (contains? acceptable-char-codes (.-charCode event))
                                       (.preventDefault event)))
                     :on-blur      #(upsert-input ws-uuid
                                                  group-uuid
                                                  repeat-id
                                                  uuid
                                                  @value-atom)}]]
     [:div.wizard-input__description
      (str "Units used: " native-units)
      [:div.wizard-input__description__units
       [:div (str "English Units: " english-units)]
       [:div (str "Metric Units: " metric-units)]]]]))

(defmethod wizard-input :discrete [variable ws-uuid group-uuid repeat-id repeat-group?]
  (r/with-let [{uuid     :bp/uuid
                var-name :variable/name
                help-key :group-variable/help-key
                list     :variable/list} variable
               selected                  (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id uuid])
               on-change                 #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))
               options                   (:list/options list)
               num-options               (count options)
               ->option                  (fn [{value :list-option/value name :list-option/name default? :list-option/default}]
                                           {:value     value
                                            :label     name
                                            :on-change on-change
                                            :selected? (or (= @selected value) (and (nil? @selected) default?))
                                            :checked?  (= @selected value)})]
    [:div.wizard-input
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     (if (>= 4 num-options)
       [c/radio-group
        {:id      (str repeat-id "-" uuid)
         :label   (when repeat-group? var-name)
         :name    (->kebab var-name)
         :options (doall (map ->option options))}]
       [c/dropdown
        {:id        (str repeat-id "-" uuid)
         :label     (when repeat-group? var-name)
         :on-change on-change
         :name      (->kebab var-name)
         :options   (map ->option options)}])]))

(defmethod wizard-input :text [{uuid     :bp/uuid
                                var-name :variable/name
                                help-key :group-variable/help-key}
                               ws-uuid
                               group-uuid
                               repeat-id
                               repeat-group?]
  (let [value      (rf/subscribe [:worksheet/input-value ws-uuid group-uuid repeat-id uuid])
        value-atom (r/atom @value)]
    [:div.wizard-input
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [c/text-input {:id            (str repeat-id "-" uuid)
                    :label         (if repeat-group? var-name "Values:")
                    :placeholder   (when repeat-group? "Value")
                    :default-value (first @value)
                    :on-change     #(reset! value-atom (input-value %))
                    :on-blur       #(upsert-input ws-uuid group-uuid repeat-id uuid (input-value %))
                    :required?     true}]]))

(defn repeat-group [ws-uuid group variables]
  (let [{group-name :group/name
         group-uuid :bp/uuid} group
        *repeat-ids           (rf/subscribe [:worksheet/group-repeat-ids ws-uuid group-uuid])
        next-repeat-id        (or  (some->> @*repeat-ids seq (apply max) inc)
                                   0)]
    [:<>
     (map-indexed
       (fn [index repeat-id]
         ^{:key repeat-id}
         [:<>
          [:div.wizard-repeat-group
           [:div.wizard-repeat-group__header
            (str group-name " #" (inc index))]]
          [:div.wizard-group__inputs
           (for [variable variables]
             ^{:key (:db/id variable)}
             [wizard-input variable ws-uuid group-uuid repeat-id true])]])
       @*repeat-ids)
     [:div {:style {:display         "flex"
                    :padding         "20px"
                    :align-items     "center"
                    :justify-content "center"}}
      [c/button {:variant  "primary"
                 :label    "Add Resource"
                 :on-click #(rf/dispatch [:worksheet/add-input-group ws-uuid group-uuid next-repeat-id])}]]]))

(defn input-group [ws-uuid group variables level]
  [:div.wizard-group
   {:class (str "wizard-group--level-" level)}
   [:div.wizard-group__header (:group/name group)]
   (if (:group/repeat? group)
     [repeat-group ws-uuid group variables]
     [:div.wizard-group__inputs
      (for [variable variables]
        ^{:key (:db/id variable)}
        [wizard-input variable ws-uuid (:bp/uuid group) 0])])])
