(ns behave.tool.views
  (:require
   [behave.components.core :as c]
   [behave.translate :refer [<t bp]]
   [dom-utils.interface :refer [input-value]]
   [map-utils.interface :refer [index-by]]
   [number-utils.interface :refer [parse-float parse-int to-precision]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [string-utils.interface :refer [->kebab]]))

(defn- unit-selector
  "Dropdown component for selecting a unit from available units."
  [prev-unit-uuid units on-click]
  (let [*unit-uuid (atom prev-unit-uuid)]
    [:div.wizard-input__unit-selector
     [c/dropdown
      {:id            "unit-selector"
       :default-value @*unit-uuid
       :selected      @*unit-uuid
       :on-change     #(on-click (input-value %))
       :name          "unit-selector"
       :options       (->> units
                           (map (fn [unit]
                                  (cond->
                                      {:label (:unit/name unit)
                                       :value (:bp/uuid unit)}
                                    (= @*unit-uuid (:bp/uuid unit))
                                    (assoc :selected? true))))
                           (sort-by :label))}]]))

(defn unit-display
  "Displays the units for a continuous variable, and enables unit selection."
  [domain-uuid *unit-uuid dimension-uuid native-unit-uuid english-unit-uuid metric-unit-uuid dynamic-units? & [on-change-units]]
  (let [domain            @(rf/subscribe [:vms/entity-from-uuid domain-uuid])
        dimension         @(rf/subscribe [:vms/entity-from-uuid dimension-uuid])
        filtered-units    (set (:domain/filtered-unit-uuids domain))
        units             (cond->> (:dimension/units dimension)
                            (seq filtered-units)
                            (filter #(filtered-units (:bp/uuid %))))
        units-by-uuid     (index-by :bp/uuid units)
        cached-unit-uuid  @(rf/subscribe [:settings/cached-unit domain-uuid])
        cached-unit       @(rf/subscribe [:vms/entity-from-uuid cached-unit-uuid])
        native-unit       @(rf/subscribe [:vms/entity-from-uuid native-unit-uuid])
        english-unit      @(rf/subscribe [:vms/entity-from-uuid english-unit-uuid])
        metric-unit       @(rf/subscribe [:vms/entity-from-uuid metric-unit-uuid])
        show-selector?    (r/atom false)
        units-system      @(rf/subscribe [:settings/tool-units-system])
        default-unit      (or cached-unit
                              (case units-system
                                :english english-unit
                                :metric  metric-unit
                                native-unit))
        on-click          #(do
                             (on-change-units %)
                             (reset! show-selector? false))
        pre-selected-unit (or (get units-by-uuid *unit-uuid) default-unit)]
    [:div.wizard-input__units
     (if (or (>= 1 (count units)) (nil? dimension) (not dynamic-units?))
       [:div.wizard-input__units__text
        (str @(<t (bp "units_used")) " " (:unit/short-code pre-selected-unit))]
       [unit-selector (:bp/uuid pre-selected-unit) units on-click])]))

(defn tool-selector
  "A Modal used for selecting a tool"
  []
  (let [tools @(rf/subscribe [:tool/all-tools])]
    [c/modal {:title          @(<t (bp "select_calculator"))
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
(defn units-system-selector
  "A component for toggling to either english or metric units"
  [tool-uuid subtool-uuid auto-compute?]
  (let [units-system @(rf/subscribe [:settings/tool-units-system])]
    [:div.tool__units-system-selector
     [c/toggle
      {:label       @(<t (bp "units_system"))
       :left-label  @(<t (bp "english"))
       :right-label @(<t (bp "metric"))
       :checked?    (= units-system :metric)
       :on-change   #(rf/dispatch [:settings/set-tool-units-system
                                   tool-uuid
                                   subtool-uuid
                                   auto-compute?
                                   (if (= units-system :english) :metric :english)])}]]))

(defn luminance
  "Calculates the relative luminance of an RGB color."
  [r g b]
  (let [srgb (fn [component]
               (let [c (/ component 255.0)]
                 (if (<= c 0.03928)
                   (/ c 12.92)
                   (js/Math.pow (/ (+ c 0.055) 1.055) 2.4))))]
    (+ (* 0.2126 (srgb r))
       (* 0.7152 (srgb g))
       (* 0.0722 (srgb b)))))

(defn hex-to-rgb
  "Converts a hex color string to RGB values."
  [hex]
  (let [r (parse-int (subs hex 1 3) 16)
        g (parse-int (subs hex 3 5) 16)
        b (parse-int (subs hex 5 7) 16)]
    [r g b]))

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
         dynamic-units?    :subtool-variable/dynamic-units?
         help-key          :subtool-variable/help-key} variable
        domain                                         @(rf/subscribe [:vms/entity-from-uuid domain-uuid])
        unit-uuid                                      @(rf/subscribe [:tool/input-units tool-uuid subtool-uuid sv-uuid])
        value                                          @(rf/subscribe [:tool/input-value tool-uuid subtool-uuid sv-uuid])
        value-atom                                     (r/atom value)]
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
      unit-uuid
      (or (:domain/dimension-uuid domain) dimension-uuid)
      (or (:domain/native-unit-uuid domain) native-unit-uuid)
      (or (:domain/english-unit-uuid domain) english-unit-uuid)
      (or (:domain/metric-unit-uuid domain) metric-unit-uuid)
      dynamic-units?
      #(rf/dispatch [:tool/update-input-units
                     tool-uuid
                     subtool-uuid
                     sv-uuid
                     %
                     auto-compute?])]]))

(defmethod tool-input :discrete
  [{:keys [variable tool-uuid subtool-uuid auto-compute?]}]
  (let [units-system              @(rf/subscribe [:settings/tool-units-system])
        {sv-uuid  :bp/uuid
         var-name :variable/name
         help-key :subtool-variable/help-key
         v-list   :variable/list} variable
        selected                  @(rf/subscribe [:tool/input-value
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
        ->option                  (fn [{value               :list-option/value
                                        t-key               :list-option/translation-key
                                        english-units-t-key :list-option/english-units-translation-key
                                        metric-units-t-key  :list-option/metric-units-translation-key
                                        default?            :list-option/default}]
                                    (let [t-key-to-use (case units-system
                                                         :english (or english-units-t-key t-key)
                                                         :metric  (or metric-units-t-key t-key)
                                                         t-key)
                                          label        @(<t t-key-to-use)]
                                      {:value     value
                                       :label     label
                                       :on-change on-change
                                       :selected? (or (= selected value) (and (nil? selected) default?))
                                       :checked?  (= selected value)}))]
    (when (and (nil? selected) default-option)
      (rf/dispatch [:tool/upsert-input-value
                    tool-uuid
                    subtool-uuid
                    sv-uuid
                    (:list-option/value default-option)
                    false]))
    [:div.tool-input
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [:div.tool-input__input
      (if (< num-options 3)
        [c/radio-group
         {:id      uuid
          :label   var-name
          :name    (->kebab var-name)
          :options (doall (map ->option options))}]
        (let [select-label @(<t (bp "select"))]
          [c/dropdown
           {:id        uuid
            :label     var-name
            :on-change on-change
            :name      (->kebab var-name)
            :options   (concat [{:label select-label :value "nil"}]
                               (map ->option options))}]))]]))

(defmulti #^{:private true} tool-output
  (fn [{:keys [variable]}] (:variable/kind variable)))

(defmethod tool-output :continuous
  [{:keys [variable tool-uuid subtool-uuid auto-compute?]}]
  (let [{sv-uuid           :bp/uuid
         domain-uuid       :variable/domain-uuid
         var-name          :variable/name
         dimension-uuid    :variable/dimension-uuid
         native-unit-uuid  :variable/native-unit-uuid
         native-decimals   :variable/native-decimals
         english-unit-uuid :variable/english-unit-uuid
         metric-unit-uuid  :variable/metric-unit-uuid
         dynamic-units?    :subtool-variable/dynamic-units?
         help-key          :subtool-variable/help-key} variable
        domain                                         @(rf/subscribe [:vms/entity-from-uuid domain-uuid])
        output-value                                   @(rf/subscribe [:tool/output-value tool-uuid subtool-uuid sv-uuid])
        value                                          (when output-value (to-precision (parse-float output-value) (or (:domain/decimals domain) native-decimals)))]
    [:div.tool-output
     {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
     [:div.tool-output__output
      {:on-mouse-over #(prn [:help/highlight-section help-key])}
      [c/text-input {:id        sv-uuid
                     :disabled? true
                     :label     var-name
                     :value     (or value "")}]]
     [unit-display
      domain-uuid
      nil
      (or (:domain/dimension-uuid domain) dimension-uuid)
      (or (:domain/native-unit-uuid domain) native-unit-uuid)
      (or (:domain/english-unit-uuid domain) english-unit-uuid)
      (or (:domain/metric-unit-uuid domain) metric-unit-uuid)
      dynamic-units?
      #(rf/dispatch [:tool/update-output-units
                     tool-uuid
                     subtool-uuid
                     sv-uuid
                     %
                     auto-compute?])]]))

(defmethod tool-output :discrete
  [{:keys [variable tool-uuid subtool-uuid]}]
  (let [{sv-uuid  :bp/uuid
         var-name :variable/name
         list     :variable/list
         help-key :subtool-variable/help-key} variable
        value                                 @(rf/subscribe [:tool/output-value tool-uuid subtool-uuid sv-uuid])
        list-options                          (index-by :list-option/value (:list/options list))
        matching-option                       (get list-options value)
        background                            (get-in matching-option [:list-option/color-tag-ref :tag/color] "#FFFFFF")
        lum-bg                                (apply luminance (hex-to-rgb background))
        black-contrast                        (/ (+ lum-bg 0.05) 0.05)
        white-contrast                        (/ 1.05 (+ lum-bg 0.05))
        font-color                            (if (> white-contrast black-contrast) "#FFFFFF" "#000000")]
    [:div
     [:div.tool-output
      {:on-mouse-over #(rf/dispatch [:help/highlight-section help-key])}
      [:div.tool-output__output
       {:on-mouse-over #(prn [:help/highlight-section help-key])}
       [c/text-input {:id         sv-uuid
                      :disabled?  true
                      :label      var-name
                      :background background
                      :font-color font-color
                      :value      (get matching-option :list-option/name "")}]]]]))

(defn- auto-compute-subtool
  "Renders a subtool that automatically computes outputs as inputs change."
  [tool-uuid subtool-uuid]
  (rf/dispatch [:tool/solve tool-uuid subtool-uuid])
  (let [variables @(rf/subscribe [:subtool/encriched-subtool-variables subtool-uuid])]
    [:div
     (for [{io :subtool-variable/io :as variable} variables
           :let                                   [params {:variable      variable
                                                           :tool-uuid     tool-uuid
                                                           :subtool-uuid  subtool-uuid
                                                           :auto-compute? true}]]
       (if (= io :input)
         [tool-input params]
         [tool-output params]))
     [c/button {:label         @(<t (bp "close_calculator"))
                :variant       "secondary"
                :icon-name     "close"
                :icon-position "right"
                :on-click      #(rf/dispatch [:tool/close-tool])}]]))

(defn- manual-subtool
  "Renders a subtool that requires manual computation via a compute button."
  [tool-uuid subtool-uuid]
  (let [input-variables  @(rf/subscribe [:subtool/input-variables subtool-uuid])
        output-variables @(rf/subscribe [:subtool/output-variables subtool-uuid])]
    [:div
     (for [variable input-variables]
       [tool-input {:variable     variable
                    :tool-uuid    tool-uuid
                    :subtool-uuid subtool-uuid}])
     [:div.tool__compute
      [c/button {:label         @(<t (bp "compute"))
                 :variant       "highlight"
                 :icon-name     "arrow2"
                 :icon-position "right"
                 :on-click      #(rf/dispatch [:tool/solve tool-uuid subtool-uuid])}]]
     (for [variable output-variables]
       [tool-output
        {:variable      variable
         :tool-uuid     tool-uuid
         :subtool-uuid  subtool-uuid
         :auto-compute? false}])
     [c/button {:label         @(<t (bp "close_calculator"))
                :variant       "secondary"
                :icon-name     "close"
                :icon-position "right"
                :on-click      #(rf/dispatch [:tool/close-tool])}]]))

(defn tool
  "A view for displaying the selected tool's inputs and outputs."
  [tool-uuid]
  (let [tool-entity           @(rf/subscribe [:tool/entity tool-uuid])
        {subtools  :tool/subtools
         tool-name :tool/name
         tool-uuid :bp/uuid}  tool-entity
        filtered-subtools     (remove #(:subtool/hide? %) subtools)
        first-subtool-uuid    (:bp/uuid (first filtered-subtools))
        selected-subtool-uuid @(rf/subscribe [:tool/selected-subtool-uuid])
        subtool-uuid          (or selected-subtool-uuid first-subtool-uuid)
        subtool               @(rf/subscribe [:vms/entity-from-uuid subtool-uuid])]
    (when (nil? selected-subtool-uuid)
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
       [units-system-selector tool-uuid subtool-uuid (:subtool/auto-compute? subtool)]
       (when (> (count filtered-subtools) 1)
         [c/tab-group {:variant  "primary"
                       :on-click #(rf/dispatch [:tool/select-subtool (:tab %)])
                       :tabs     (map (fn [{s-name :subtool/name s-uuid :bp/uuid}]
                                        {:label     s-name
                                         :tab       s-uuid
                                         :selected? (= subtool-uuid s-uuid)})
                                      filtered-subtools)}])
       (if (:subtool/auto-compute? subtool)
         [auto-compute-subtool tool-uuid subtool-uuid]
         [manual-subtool tool-uuid subtool-uuid])]]]))
