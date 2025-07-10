(ns behave.worksheet.views
  (:require [behave.components.core       :as c]
            [behave.components.navigation :refer [wizard-navigation]]
            [behave.tool.views            :refer [tool tool-selector]]
            [behave.translate             :refer [<t bp]]
            [behave.wizard.views          :refer [wizard-expand]]
            [goog.string                  :as gstring]
            [re-frame.core                :as rf]
            [reagent.core                 :as r]))

(defn- workflow-select-header [{:keys [icon header description]}]
  [:div.accordion
   [:div.accordion__header @(<t "behaveplus:working_area")]
   [wizard-expand]
   [:div.workflow-select__header__content
    [c/icon {:icon-name icon}]
    [:div
     [:h3 header]
     [:p description]]]])

(defn home-page [_params]
  (let [*new-or-import      (rf/subscribe [:wizard/get-cached-new-worksheet-or-import])
        show-tool-selector? @(rf/subscribe [:tool/show-tool-selector?])
        selected-tool-uuid  @(rf/subscribe [:tool/selected-tool-uuid])]
    [:<>
     (when show-tool-selector?
       [tool-selector])
     (when (some? selected-tool-uuid)
       [tool selected-tool-uuid])
     [:div.workflow-select
      [workflow-select-header
       {:icon        "existing-run" ;TODO update when LOGO is available
        :header      @(<t (bp "welcome_message"))
        :description @(<t (bp "create_a_new-worksheet_or_import_an_existing_one"))}]
      [:div.workflow-select__content
       [c/card-group {:on-select      #(rf/dispatch [:wizard/update-cached-new-worksheet-or-import (:workflow %)])
                      :flex-direction "column"
                      :card-size      "large"
                      :cards          [{:title     @(<t (bp "new_worksheet"))
                                        :content   @(<t (bp "behaveplus:create_a_new_worksheet"))
                                        :icons     [{:icon-name "worksheet"
                                                     :checked?  (= @*new-or-import :new-worksheet)}]
                                        :selected? (= @*new-or-import :new-worksheet)
                                        :order     0
                                        :workflow  :new-worksheet}
                                       {:title     @(<t "behaveplus:workflow:import_title")
                                        :content   @(<t "behaveplus:workflow:import_desc")
                                        :icons     [{:icon-name "import-files"
                                                     :checked?  (= @*new-or-import :import)}]
                                        :selected? (= @*new-or-import :import)
                                        :order     1
                                        :workflow  :import}]}]]
      [wizard-navigation {:next-label     "Next"
                          :next-disabled? (nil? @*new-or-import)
                          :on-next        #(rf/dispatch [:navigate "/worksheets/workflow-selection"])}]]]))

;; TODO use title
(defn module-selection-page [_params]
  (let [*workflow           (rf/subscribe [:wizard/get-cached-workflow])
        *modules            (rf/subscribe [:local-storage/get-in [:state  :worksheet :*modules]])
        ;; *modules            (rf/subscribe [:state [:worksheet :*modules]])
        *submodule          (rf/subscribe [:worksheet/first-output-submodule-slug (first @*modules)])
        name                (rf/subscribe [:state [:worksheet :name]])
        show-tool-selector? @(rf/subscribe [:tool/show-tool-selector?])
        selected-tool-uuid  @(rf/subscribe [:tool/selected-tool-uuid])]
    [:<>
     (when show-tool-selector?
       [tool-selector])
     (when (some? selected-tool-uuid)
       [tool selected-tool-uuid])
     [:div.workflow-select
      [workflow-select-header
       {:icon        "modules"
        :header      @(<t (bp "module_selection"))
        :description @(<t (bp "please_select_from_the_following_options"))}]
      [:div.workflow-select__content
       [c/card-group {:on-select #(do
                                    (rf/dispatch [:local-storage/update-in [:state :worksheet :*modules] (:module %)])
                                    (rf/dispatch [:state/set [:sidebar :*modules] (set (:module %))])
                                    (rf/dispatch [:state/set [:worksheet :*modules] (:module %)]))

                      :flex-direction "row"
                      :cards          [{:order     1
                                        :title     @(<t (bp "surface_only"))
                                        :content   @(<t (bp "surface:description"))
                                        :icons     [{:icon-name "surface"}]
                                        :selected? (= @*modules [:surface])
                                        :module    [:surface]}
                                       {:order     2
                                        :title     @(<t (bp "surface_and_crown"))
                                        :content   @(<t (bp "surface+crown:description"))
                                        :icons     [{:icon-name "surface"}
                                                    {:icon-name "crown"}]
                                        :selected? (= @*modules [:surface :crown])
                                        :module    [:surface :crown]}
                                       {:order     3
                                        :title     @(<t (bp "surface_and_mortality"))
                                        :content   @(<t (bp "surface+mortality:description"))
                                        :icons     [{:icon-name "surface"}
                                                    {:icon-name "mortality"}]
                                        :selected? (= @*modules [:surface :mortality])
                                        :module    [:surface :mortality]}
                                       {:order     4
                                        :title     @(<t (bp "surface_and_contain"))
                                        :content   @(<t (bp "surface+contain:description"))
                                        :icons     [{:icon-name "surface"}
                                                    {:icon-name "contain"}]
                                        :selected? (= @*modules [:surface :contain])
                                        :module    [:surface :contain]}
                                       #_{:order     5
                                          :title     @(<t (bp "mortality_only"))
                                          :content   @(<t (bp "mortality:description"))
                                          :icons     [{:icon-name "mortality"}]
                                          :selected? (= @*modules [:mortality])
                                          :module    [:mortality]}
                                       #_{:order     6
                                          :title     @(<t (bp "contain_only"))
                                          :content   "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
                                          :icons     [{:icon-name "contain"}]
                                          :selected? (= @*modules [:contain])
                                          :module    [:contain]}
                                       ]}]]
      [wizard-navigation {:next-label     @(<t (bp "next"))
                          :back-label     @(<t (bp "back"))
                          :next-disabled? (empty? @*modules)
                          :on-back        #(rf/dispatch [:navigate "/worksheets/workflow-selection"])
                          :on-next        #(rf/dispatch [:wizard/new-worksheet @name @*modules @*submodule @*workflow])}]]]))

(defn workflow-selection-page [_params]
  (let [*workflow           (rf/subscribe [:wizard/get-cached-workflow])
        *new-or-import      (rf/subscribe [:wizard/get-cached-new-worksheet-or-import])
        show-tool-selector? @(rf/subscribe [:tool/show-tool-selector?])
        selected-tool-uuid  @(rf/subscribe [:tool/selected-tool-uuid])]
    [:<>
     (when show-tool-selector?
       [tool-selector])
     (when (some? selected-tool-uuid)
       [tool selected-tool-uuid])
     [:div.workflow-select
      [workflow-select-header
       {:icon        "existing-run" ;TODO update when LOGO is available
        :header      @(<t (bp "welcome_message"))
        :description (str
                      @(<t (bp "please_select_a_workflow"))
                      @(<t (bp "note_that_that_you_can_open_using_any_workflow")))}]
      [:div.workflow-select__content
       [c/card-group {:on-select      #(rf/dispatch [:wizard/update-cached-workflow (:workflow %)])
                      :flex-direction "column"
                      :card-size      "large"
                      :cards          [{:title     @(<t (bp "open_using_guided_workflow"))
                                        :content   @(<t (bp "recommended_for_students"))
                                        :icons     [{:icon-name "guided-work"
                                                     :checked?  (= @*workflow :guided)}]
                                        :selected? (= @*workflow :guided)
                                        :order     0
                                        :workflow  :guided}
                                       {:title     @(<t (bp "open_using_standard_workflow"))
                                        :content   @(<t (bp "recommended_for_intermediate_users"))
                                        :icons     [{:icon-name "checklist"
                                                     :checked?  (= @*workflow :standard)}]
                                        :selected? (= @*workflow :standard)
                                        :order     1
                                        :workflow  :standard}]}]]
      [wizard-navigation {:next-label     @(<t (bp "next"))
                          :back-label     @(<t (bp "back"))
                          :next-disabled? (nil? @*workflow)
                          :on-back        #(rf/dispatch [:wizard/navigate-home])
                          :on-next        (if (= @*new-or-import :import)
                                            #(rf/dispatch [:navigate (str "/worksheets/import")])
                                            #(rf/dispatch [:navigate (str "/worksheets/module-selection")]))}]]]))

(defn import-worksheet-page [_params]
  (let [file-name   (r/track #(or @(rf/subscribe [:state [:worksheet :file :name]])
                                  @(<t (bp "select_a_file"))))
        file        (r/track #(or @(rf/subscribe [:state [:worksheet :file :obj]])
                                  nil))
        ws-version  (r/track #(or @(rf/subscribe [:state :ws-version])
                                  nil))
        app-version (r/track #(or @(rf/subscribe [:state :app-version])
                                  nil))
        *workflow           (rf/subscribe [:wizard/get-cached-workflow])]
    [:<>
     [:div.workflow-select
      [workflow-select-header
       {:icon        "modules"
        :header      @(<t (bp "upload_an_existing_file"))
        :description @(<t (bp "only_files_from_behave7_are_supported"))}]
      [:div.workflow-select__content
       [c/browse-input {:button-label @(<t (bp "browse"))
                        :accept       ".bpr,bpw,.bp6,.bp7,.sqlite"
                        :label        @file-name
                        :on-change    #(do (rf/dispatch-sync [:ws/worksheet-selected (.. % -target -files)])
                                           (rf/dispatch-sync [:wizard/open @file]))}]
       (when (and @app-version @ws-version (not= @app-version @ws-version))
         [:div.workflow-select__warning
          (str
           (gstring/format
            @(<t (bp "the-application-version-is-%s-but-your-run-is-%s"))
            @app-version @ws-version)
           " "
           @(<t (bp "review-your-outputs-and-inputs-before-calculating-this-run")))])
       [wizard-navigation {:next-label @(<t (bp "next"))
                           :back-label @(<t (bp "back"))
                           :on-back    #(rf/dispatch [:navigate "/worksheets/workflow-selection"])
                           ;;TODO Get full file path from file
                           :on-next    #(rf/dispatch [:wizard/navigate-to-latest-worksheet @*workflow])}]]]]))
