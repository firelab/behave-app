(ns behave.wizard.views
  (:require [behave.components.core         :as c]
            [behave.components.input-group  :refer [input-group]]
            [behave.components.review-input-group  :as review]
            [behave.components.navigation   :refer [wizard-navigation]]
            [behave.components.output-group :refer [output-group]]
            [behave-routing.main            :refer [routes]]
            [behave.translate               :refer [<t bp]]
            [behave.wizard.events]
            [behave.wizard.subs]
            [bidi.bidi                      :refer [path-for]]
            [behave.worksheet.events]
            [behave.worksheet.subs]
            [goog.string                    :as gstring]
            [goog.string.format]
            [re-frame.core                  :refer [dispatch subscribe]]
            [string-utils.interface         :refer [->kebab]]))

;;; Components

(defmulti submodule-page (fn [io _ _] io))

(defmethod submodule-page :input [_ ws-uuid groups on-back on-next]
  [:<>
   [:div.wizard-io
    (for [group groups]
      (let [variables (:group/group-variables group)]
        ^{:key (:db/id group)}
        [input-group ws-uuid group variables]))]])

(defmethod submodule-page :output [_ ws-uuid groups on-back on-next]
  [:<>
   [:div.wizard-io
    (for [group groups]
      (let [variables (:group/group-variables group)]
        ^{:key (:db/id group)}
        [output-group ws-uuid group variables]))]])

(defn- io-tabs [submodules {:keys [io] :as params}]
  (let [[i-subs o-subs] (partition-by #(:submodule/io %) submodules)
        first-submodule (:slug (first (if (= io :input) o-subs i-subs)))]
    [:div.wizard-header__io-tabs
     [c/tab-group {:variant   "outline-primary"
                   :flat-edge "top"
                   :align     "right"
                   :on-click  #(when (not= io (:tab %))
                                 (dispatch [:wizard/select-tab (assoc params
                                                                      :io (:tab %)
                                                                      :submodule first-submodule)]))
                   :tabs      [{:label "Outputs" :tab :output :selected? (= io :output)}
                               {:label "Inputs" :tab :input :selected? (= io :input)}]}]]))

(defn- wizard-header [{module-name :module/name} all-submodules {:keys [io submodule] :as params}]
  (let [submodules (filter #(= (:submodule/io %) io) all-submodules)]
    [:div.wizard-header
     [io-tabs all-submodules params]
     [:div.wizard-header__banner
      [:div.wizard-header__banner__icon
       [c/icon :modules]]
      [:div.wizard-header__banner__title
       (str module-name " Module")]]
     [:div.wizard-header__submodule-tabs
      [c/tab-group {:variant  "outline-primary"
                    :on-click #(dispatch [:wizard/select-tab (assoc params :submodule (:tab %))])
                    :tabs     (map (fn [{s-name :submodule/name slug :slug}]
                                     {:label     s-name
                                      :tab       slug
                                      :selected? (= submodule slug)}) submodules)}]]]))

(defn wizard-page [{:keys [module io submodule] :as params}]
  (let [*ws-uuid                (subscribe [:worksheet/latest])
        *module                 (subscribe [:wizard/*module module])
        module-id               (:db/id @*module)
        *submodules             (subscribe [:wizard/submodules module-id])
        *submodule              (subscribe [:wizard/*submodule module-id submodule io])
        *groups                 (subscribe [:wizard/groups (:db/id @*submodule)])
        *warn-limit?            (subscribe [:state :warn-continuous-input-limit])
        *multi-value-input-limit (subscribe [:wizard/multi-value-input-limit])
        *multi-value-input-count (subscribe [:wizard/multi-value-input-count @*ws-uuid])
        *all-inputs-entered?    (subscribe [:worksheet/all-inputs-entered? @*ws-uuid module-id submodule])
        on-back                 #(dispatch [:wizard/prev-tab params])
        on-next                 #(dispatch [:wizard/next-tab @*module @*submodule @*submodules params])]
    [:div.wizard-page
     [wizard-header @*module @*submodules params]
     [submodule-page io @*ws-uuid @*groups on-back on-next]
     (when (true? @*warn-limit?)
       [:div.wizard-warning
        (gstring/format  @(<t (bp "warn_input_limit")) @*multi-value-input-count @*multi-value-input-limit)])
     [wizard-navigation {:next-label     @(<t (bp "next"))
                         :on-next        on-next
                         :next-disabled? (and (= io :input) (not @*all-inputs-entered?))
                         :back-label     @(<t (bp "back"))
                         :on-back        on-back}]]))

(defn run-description []
  (let [*ws-uuid (subscribe [:worksheet/latest])
        *values  (subscribe [:worksheet/get-attr @*ws-uuid :worksheet/run-description])]
    [:div.wizard-review__run-desciption
     [:div.wizard-review__run-description__header
      @(<t "behaveplus:run_description")]
     [:div.wizard-review-group__inputs
      [:div.wizard-review__run-description__input
       [c/text-input {:label       @(<t (bp "run_description"))
                      :placeholder @(<t (bp "type_description"))
                      :id          (->kebab @(<t (bp "run_description")))
                      :value       (or (first @*values) "")
                      :on-change   #(dispatch [:worksheet/update-attr
                                               @*ws-uuid
                                               :worksheet/run-description
                                               (-> % .-target .-value)])}]
       [:div.wizard-review__run-description__message
        [c/button {:label         (gstring/format "*%s"  @(<t (bp "optional")))
                   :variant       "transparent-highlight"
                   :icon-name     :help2
                   :icon-position "left"}]
        @(<t (bp "a_brief_phrase_documenting_the_run"))]]]]))

(defn wizard-review-page [{:keys [id] :as params}]
  (let [*ws-uuid                 (subscribe [:worksheet/latest])
        *modules                 (subscribe [:worksheet/modules @*ws-uuid])
        *warn-limit?             (subscribe [:wizard/warn-limit? @*ws-uuid])
        *multi-value-input-limit (subscribe [:wizard/multi-value-input-limit])
        *multi-value-input-count (subscribe [:wizard/multi-value-input-count @*ws-uuid])]
    [:div.accordion
     [:div.accordion__header
      [c/tab {:variant   "outline-primary"
              :selected? true
              :label     @(<t (bp "working_area"))}]]
     [:div.wizard-page
      [:div.wizard-header
       [:div.wizard-header__banner {:style {:margin-top "20px"}}
        [:div.wizard-header__banner__icon
         [c/icon :modules]]
        [:div.wizard-header__banner__title "Review Modules"]]
       [:div.wizard-review
        [run-description]
        (for [module-kw @*modules
              :let      [module-name (name module-kw)
                         module @(subscribe [:wizard/*module module-name])]]
          [:div
           [:div.wizard-review__module
            (gstring/format "%s Inputs"  @(<t (:module/translation-key module)))]
           [:div.wizard-review__submodule
            (for [submodule @(subscribe [:wizard/submodules-io-input-only (:db/id module)])]
              [:<>
               [:div.wizard-review__submodule-header (:submodule/name submodule)]
               (for [group (:submodule/groups submodule)
                     :when (seq (:group/group-variables group))
                     :let  [variables  (:group/group-variables group)
                            edit-route (path-for routes
                                                 :ws/wizard
                                                 :id        id
                                                 :module    module-name
                                                 :io        :input
                                                 :submodule (:slug submodule))]]
                 ^{:key (:db/id group)}
                 [review/input-group @*ws-uuid group variables edit-route])])]])]
       (when (true? @*warn-limit?)
         [:div.wizard-warning
          (gstring/format  @(<t (bp "warn_input_limit")) @*multi-value-input-count @*multi-value-input-limit)])
       [wizard-navigation {:next-label @(<t (bp "next"))
                           :back-label @(<t (bp "back"))
                           :on-back    #(dispatch [:wizard/prev-tab params])
                           :on-next    #(dispatch [:worksheet/solve @*ws-uuid])}]]]]))

(defn wizard-results-page [_]
  (let [results   (subscribe [:state [:worksheet :results]])
        variables (subscribe [:pull-many '[* {:variable/_group-variables [*]}] (keys (:contain @results))])
        results   (map (fn [value v]
                         {:name  (-> v (:variable/_group-variables) (first) (:variable/name))
                          :value value})
                       (vals (:contain @results))
                       @variables)]
    [:div.accordion
     [:div.accordion__header
      [:div.accordion__header__title @(<t (bp "working_area"))]]
     [:div.wizard-page
      [:div.wizard-header
       [:div.wizard-header__banner {:style {:margin-top "20px"}}
        [:div.wizard-header__banner__icon
         [c/icon :modules]]
        [:div.wizard-header__banner__title "Results"]]
       [:div.wizard-review
        [c/table {:title   "Contain Results"
                  :headers ["Variable Name" "Value"]
                  :columns [:name :value]
                  :rows    results}]]
       [:div.wizard-navigation
        [c/button {:label   "Back"
                   :variant "secondary"}]
        [c/button {:label         "Next"
                   :variant       "highlight"
                   :icon-name     "arrow2"
                   :icon-position "right"}]]]]]))

;; TODO Might want to set this in a config file to the application
(def ^:const multi-value-input-limit 3)

;;; Public Components
(defn root-component [params]
  (let [loaded? (subscribe [:app/loaded?])]
    [:div.accordion
     [:div.accordion__header
      [c/tab {:variant   "outline-primary"
              :selected? true
              :label     @(<t "behaveplus:working_area")}]]
     [:div.wizard
      (if @loaded?
        [wizard-page params]
        [:div.wizard__loading
         [:h2 "Loading..."]])]]))
