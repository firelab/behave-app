(ns behave.tool.views
  (:require [re-frame.core :as rf]
            [dom-utils.interface    :refer [input-value]]
            [reagent.core            :as r]
            [string-utils.interface  :refer [->kebab]]
            [behave.components.core         :as c]))

(defn tool-selector
  "A Modal used for selecting a tool"
  []
  (let [tools @(rf/subscribe [:tool/all-tools])]
    [c/modal {:title          "Select Tools"
              :close-on-click #(rf/dispatch [:tool/close-tool-selector])
              :content        [c/card-group {:on-select      #(rf/dispatch [:tool/select-tool (:uuid %)])
                                             :flex-direction "column"
                                             :card-size      "small"
                                             :cards          (map-indexed (fn [idx tool]
                                                                            {:title     (:tool/name tool)
                                                                             :uuid      (:bp/uuid tool)
                                                                             :selected? false
                                                                             :order     idx})
                                                                          tools)}]}]))

(defmulti tool-input (fn [variable] (:variable/kind variable)))

(defmethod tool-input nil [variable] (println [:NO-KIND-VAR variable]))

(defmethod tool-input :continuous [{sv-uuid       :bp/uuid
                                    var-name      :variable/name
                                    native-units  :variable/native-units
                                    english-units :variable/english-units
                                    metric-units  :variable/metric-units
                                    help-key      :subtool-variable/help-key}
                                   tool-uuid
                                   subtool-uuid]
  (let [value      (rf/subscribe [:tool/input-value
                                  tool-uuid
                                  subtool-uuid
                                  sv-uuid])
        value-atom (r/atom @value)]
    [:div.tool-input
     [:div.tool-input__input
      {:on-mouse-over #(prn [:help/highlight-section help-key])}
      [c/number-input {:id         sv-uuid
                       :label      var-name
                       :value-atom value-atom
                       :required?  true
                       :on-change  #(reset! value-atom (input-value %))
                       :on-blur    #(rf/dispatch [:tool/upsert-input-value
                                                  tool-uuid
                                                  subtool-uuid
                                                  sv-uuid
                                                  @value-atom])}]]
     [:div.tool-input__description
      (str "Units used: " native-units)
      [:div.tool-input__description__units
       [:div (str "English Units: " english-units)]
       [:div (str "Metric Units: " metric-units)]]]]))

(defmethod tool-input :discrete [variable tool-uuid subtool-uuid]
  (r/with-let [{sv-uuid  :bp/uuid
                var-name :variable/name
                help-key :subtool-variable/help-key
                v-list   :variable/list} variable

               selected    (rf/subscribe [:tool/input-value
                                          tool-uuid
                                          subtool-uuid
                                          sv-uuid])
               on-change   #(rf/dispatch [:tool/upsert-input-value
                                          tool-uuid
                                          subtool-uuid
                                          sv-uuid
                                          (input-value %)])
               options     (:list/options v-list)
               num-options (count options)
               ->option    (fn [{value    :list-option/value
                                 l-name   :list-option/name
                                 default? :list-option/default}]
                             {:value     value
                              :label     l-name
                              :on-change on-change
                              :selected? (or (= @selected value) (and (nil? @selected) default?))
                              :checked?  (= @selected value)})]
    [:div.tool-input
     {:on-mouse-over #(prn [:help/highlight-section help-key])}
     (if (< num-options 4)
       [c/radio-group
        {:id      uuid
         :label   var-name
         :name    (->kebab var-name)
         :options (doall (map ->option options))}]
       [c/dropdown
        {:id        uuid
         :label     var-name
         :on-change on-change
         :name      (->kebab var-name)
         :options   (concat [{:label "Select..." :value "nil"}]
                            (map ->option options))}])]))

(defn tool
  "A view for displaying the selected tool's inputs and outputs."
  [tool-uuid]
  (let [{subtools  :tool/subtools
         tool-name :tool/name} @(rf/subscribe [:tool/entity tool-uuid])
        first-subtool-uuid     (:bp/uuid (first subtools))
        selected-subtool-uuid  @(rf/subscribe [:tool/selected-subtool-uuid])]
    (when (nil? selected-subtool-uuid)
      (rf/dispatch [:tool/select-subtool first-subtool-uuid]))
    [:div.tool
     [:div.accordion
      [:div.accordion__header
       [c/tab {:variant   "outline-primary"
               :selected? true
               :label     tool-name}]]
      (when (> (count subtools) 1)
        [c/tab-group {:variant  "outline-primary"
                      :on-click #(rf/dispatch [:tool/select-subtool (:tab %)])
                      :tabs     (map (fn [{s-name :subtool/name s-uuid :bp/uuid}]
                                       {:label     s-name
                                        :tab       s-uuid
                                        :selected? (= selected-subtool-uuid s-uuid)})
                                     subtools)}])
      (let [input-vars @(rf/subscribe [:subtool/input-variables (or selected-subtool-uuid
                                                                    first-subtool-uuid)])]
        (for [variable input-vars]
          [tool-input variable tool-uuid selected-subtool-uuid]))]]))
