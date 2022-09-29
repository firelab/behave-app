(ns behave.wizard.views
  (:require [re-frame.core                  :refer [dispatch subscribe]]
            [behave.translate               :refer [<t bp]]
            [behave.components.core         :as c]
            [behave.components.output-group :refer [output-group]]
            [behave.components.input-group  :refer [input-group]]
            [behave.wizard.events]
            [behave.wizard.subs]))

;;; Components

(defmulti submodule-page (fn [io _ _] io))

(defmethod submodule-page :input [_ groups]
  [:div.wizard-io
   (for [group groups]
     (let [variables (:group/group-variables group)]
       ^{:key (:db/id group)}
       [input-group group variables]))])

(defmethod submodule-page :output [_ groups]
  [:div.wizard-io
   (for [group groups]
     (let [variables (:group/group-variables group)]
       ^{:key (:db/id group)}
       [output-group group variables]))])

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

(defn- wizard-navigation [*module *submodule all-submodules params]
  [:div.wizard-navigation
   [c/button {:label         "Back"
              :variant       "secondary"
              :on-click      #(dispatch [:wizard/prev-tab params])}]

   [c/button {:label         "Next"
              :variant       "highlight"
              :icon-name     "arrow2"
              :icon-position "right"
              :on-click      #(dispatch [:wizard/next-tab *module *submodule all-submodules params])}]])

(defn wizard-page [{:keys [module io submodule] :as params}]
  (let [*module    (subscribe [:wizard/*module module])
        submodules (subscribe [:wizard/submodules (:db/id @*module)])
        *submodule (subscribe [:wizard/*submodule (:db/id @*module) submodule io])
        *groups    (subscribe [:wizard/groups (:db/id @*submodule)])]

    [:div.wizard-page
     [wizard-header @*module @submodules params]
     [submodule-page io @*groups]
     [wizard-navigation @*module @*submodule @submodules params]]))

(defn wizard-review-page [params]
  [:div.accordion
   [:div.accordion__header
    [:div.accordion__header__title @(<t (bp "working_area"))]]
   [:div.wizard-page
    [:div.wizard-header
     [:div.wizard-header__banner {:style {:margin-top "20px"}}
      [:div.wizard-header__banner__icon
       [c/icon :modules]]
      [:div.wizard-header__banner__title "Review Modules"]]
     [:div.wizard-review
      "TODO: Add table of current values"]
     [:div.wizard-navigation
      [c/button {:label   "Back"
                 :variant "secondary"}]
      [c/button {:label   "Run"
                 :variant       "highlight"
                 :icon-name     "arrow2"
                 :icon-position "right"
                 :on-click      #(dispatch [:worksheet/solve params])}]]]]])

;;; Public Components

(defn root-component [params]
  (let [loaded? (subscribe [:state :loaded?])]
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
