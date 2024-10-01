(ns behave.tool.views
  (:require [behave.components.core :as c]
            [behave.components.unit-selector :refer [unit-display]]
            [behave.translate       :refer [<t bp]]
            [dom-utils.interface    :refer [input-value]]
            [reagent.core           :as r]
            [string-utils.interface :refer [->kebab]]
            [re-frame.core          :as rf]))

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

(defmulti #^{:private true} tool-input
  (fn [{:keys [variable]}] (:variable/kind variable)))

(defmethod tool-input nil [variable] (println [:NO-KIND-VAR variable]))

(defmethod tool-input :continuous
  [{:keys [variable tool-uuid subtool-uuid auto-compute?]}]
  (let [{sv-uuid           :bp/uuid
         domain-uuid       :variable/domain-uuid
         var-name          :variable/name
         dimension-uuid    :variable/dimension-uuid
         native-unit-uuid  :variable/native-unit-uuid
         english-unit-uuid :variable/english-unit-uuid
         metric-unit-uuid  :variable/metric-unit-uuid
         help-key          :subtool-variable/help-key} variable

        *domain    (rf/subscribe [:vms/entity-from-uuid domain-uuid])
        *unit-uuid (rf/subscribe [:tool/input-units tool-uuid subtool-uuid sv-uuid])
        value      (rf/subscribe [:tool/input-value tool-uuid subtool-uuid sv-uuid])
        value-atom (r/atom @value)]
    [:div.tool-input
     [:div.tool-input__input
      {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
      [c/number-input {:id         sv-uuid
                       :label      var-name
                       :value-atom value-atom
                       :required?  true
                       :on-change  #(reset! value-atom (input-value %))
                       :on-blur    #(rf/dispatch [:tool/upsert-input-value
                                                  tool-uuid
                                                  subtool-uuid
                                                  sv-uuid
                                                  @value-atom
                                                  auto-compute?])}]]

     [unit-display
      domain-uuid
      @*unit-uuid
      (or (:domain/dimension-uuid @*domain) dimension-uuid)
      (or (:domain/native-unit-uuid @*domain) native-unit-uuid)
      (or (:domain/english-unit-uuid @*domain) english-unit-uuid)
      (or (:domain/metric-unit-uuid @*domain) metric-unit-uuid)
      #(rf/dispatch [:tool/update-input-units
                     tool-uuid
                     subtool-uuid
                     sv-uuid
                     %
                     auto-compute?])]]))

(defmethod tool-input :discrete
  [{:keys [variable tool-uuid subtool-uuid auto-compute?]}]
  (let [{sv-uuid  :bp/uuid
         var-name :variable/name
         help-key :subtool-variable/help-key
         v-list   :variable/list} variable
        selected                  (rf/subscribe [:tool/input-value
                                                 tool-uuid
                                                 subtool-uuid
                                                 sv-uuid])
        options                   (:list/options v-list)
        default-option            (first (filter #(true? (:list-option/default %)) options))
        on-change                 #(rf/dispatch [:tool/upsert-input-value
                                                 tool-uuid
                                                 subtool-uuid
                                                 sv-uuid
                                                 (input-value %)
                                                 auto-compute?])
        num-options               (count options)
        ->option                  (fn [{value    :list-option/value
                                        t-key    :list-option/translation-key
                                        default? :list-option/default}]
                                    {:value     value
                                     :label     @(<t t-key)
                                     :on-change on-change
                                     :selected? (or (= @selected value) (and (nil? @selected) default?))
                                     :checked?  (= @selected value)})]
    (when (and (nil? @selected) default-option)
      (rf/dispatch [:tool/upsert-input-value
                    tool-uuid
                    subtool-uuid
                    sv-uuid
                    (:list-option/value default-option)
                    false]))
    [:div.tool-input
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     (if (< num-options 3)
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

(defn- tool-output
  [{:keys [variable tool-uuid subtool-uuid auto-compute?]}]
  (let [{sv-uuid           :bp/uuid
         domain-uuid       :variable/domain-uuid
         var-name          :variable/name
         dimension-uuid    :variable/dimension-uuid
         native-unit-uuid  :variable/native-unit-uuid
         english-unit-uuid :variable/english-unit-uuid
         metric-unit-uuid  :variable/metric-unit-uuid
         help-key          :subtool-variable/help-key} variable
        *domain                                        (rf/subscribe [:vms/entity-from-uuid domain-uuid])
        value                                          (rf/subscribe [:tool/output-value tool-uuid subtool-uuid sv-uuid])]
    [:div.tool-output
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [:div.tool-output__output
      {:on-mouse-over #(prn [:help/highlight-section help-key])}
      [c/text-input {:id        sv-uuid
                     :disabled? true
                     :label     var-name
                     :value     (or @value "")}]]
     [unit-display
      domain-uuid
      nil
      (or (:domain/dimension-uuid @*domain) dimension-uuid)
      (or (:domain/native-unit-uuid @*domain) native-unit-uuid)
      (or (:domain/english-unit-uuid @*domain) english-unit-uuid)
      (or (:domain/metric-unit-uuid @*domain) metric-unit-uuid)
      #(rf/dispatch [:tool/update-output-units
                     tool-uuid
                     subtool-uuid
                     sv-uuid
                     %
                     auto-compute?])]]))

(defn- auto-compute-subtool [tool-uuid subtool-uuid]
  (rf/dispatch [:tool/solve tool-uuid subtool-uuid])
  (let [variables (rf/subscribe [:subtool/encriched-subtool-variables subtool-uuid])]
    [:div
     (for [{io :subtool-variable/io :as variable} @variables
           :let                                   [params  {:variable      variable
                                                            :tool-uuid     tool-uuid
                                                            :subtool-uuid  subtool-uuid
                                                            :auto-compute? true}]]
       (if (= io :input)
         [tool-input params]
         [tool-output params]))
     [c/button {:label         @(<t (bp "close_tool"))
                :variant       "secondary"
                :icon-name     "close"
                :icon-position "right"
                :on-click      #(rf/dispatch [:tool/close-tool])}]]))

(defn- manual-subtool [tool-uuid subtool-uuid]
  (let [input-variables  (rf/subscribe [:subtool/input-variables subtool-uuid])
        output-variables (rf/subscribe [:subtool/output-variables subtool-uuid])]
    [:div
     (for [variable @input-variables]
       [tool-input {:variable     variable
                    :tool-uuid    tool-uuid
                    :subtool-uuid subtool-uuid}])
     [:div.tool__compute
      [c/button {:label         @(<t (bp "compute"))
                 :variant       "highlight"
                 :icon-name     "arrow2"
                 :icon-position "right"
                 :on-click      #(rf/dispatch [:tool/solve tool-uuid subtool-uuid])}]]
     (for [variable @output-variables]
       [tool-output
        {:variable      variable
         :tool-uuid     tool-uuid
         :subtool-uuid  subtool-uuid
         :auto-compute? false}])
     [c/button {:label         @(<t (bp "close_tool"))
                :variant       "secondary"
                :icon-name     "close"
                :icon-position "right"
                :on-click      #(rf/dispatch [:tool/close-tool])}]]))

(defn tool
  "A view for displaying the selected tool's inputs and outputs."
  [tool-uuid]
  (let [{subtools  :tool/subtools
         tool-name :tool/name
         tool-uuid :bp/uuid}  @(rf/subscribe [:tool/entity tool-uuid])
        first-subtool-uuid    (:bp/uuid (first subtools))
        selected-subtool-uuid (rf/subscribe [:tool/selected-subtool-uuid])
        subtool-uuid          (or @selected-subtool-uuid first-subtool-uuid)
        subtool               (rf/subscribe [:vms/entity-from-uuid subtool-uuid])]
    (when (nil? @selected-subtool-uuid)
      (rf/dispatch [:tool/select-subtool first-subtool-uuid]))
    [:div.tool
     [:div.accordion
      [:div.accordion__header
       tool-name
       [:div.tool__close
        [c/button {:icon-name "close"
                   :on-click  #(rf/dispatch [:tool/close-tool])
                   :size      "small"
                   :variant   "secondary"}]]]
      [:div.accordion__body
       (when (> (count subtools) 1)
         [c/tab-group {:variant  "primary"
                       :on-click #(rf/dispatch [:tool/select-subtool (:tab %)])
                       :tabs     (map (fn [{s-name :subtool/name s-uuid :bp/uuid}]
                                        {:label     s-name
                                         :tab       s-uuid
                                         :selected? (= subtool-uuid s-uuid)})
                                      subtools)}])
       (if (:subtool/auto-compute? @subtool)
         [auto-compute-subtool tool-uuid subtool-uuid]
         [manual-subtool tool-uuid subtool-uuid])]]]))
