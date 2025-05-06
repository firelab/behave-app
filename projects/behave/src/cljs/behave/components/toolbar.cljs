(ns behave.components.toolbar
  (:require [behave.components.core :as c]
            [behave.translate       :refer [<t bp]]
            [goog.string            :as gstring]
            [re-frame.core          :as rf]))

#_{:clj-kondo/ignore [:missing-docstring]}
(def step-priority
  [[:ws/all nil]
   [:ws/import nil]
   [:ws/guided nil]
   [:ws/independent nil]
   [:ws/wizard :output]
   [:ws/wizard :input]
   [:ws/wizard-standard :output]
   [:ws/wizard-standard :input]
   [:ws/review nil]
   [:ws/results-settings nil]
   [:ws/results nil]])

(defn- toolbar-tool [{icon-name :icon translation-key :label on-click :on-click :as s}]
  [:div.toolbar__tool
   [:div.toolbar__tool__icon
    [c/button {:variant   "transparent-primary"
               :title     @(<t translation-key)
               :icon-name icon-name
               :on-click  #(on-click s)}]]])

(defn- enrich-step
  [progress selected-step furthest-step-completed-id]
  (fn [idx step]
    (cond-> step
      (> idx furthest-step-completed-id) (assoc :on-select #(println "disabled!"))
      :always                            (assoc :step-id    idx)
      :always                            (assoc :completed? (>= progress idx))
      :always                            (assoc :selected?  (= selected-step idx)))))

(defmulti progress-bar
  (fn [{:keys [route-handler workflow] :as params}]
    (cond
      (or (= route-handler :home)
          (= route-handler :settings/all)) :ws/all
      (= workflow :guided)                 :ws/wizard
      (= workflow :standard)               :ws/wizard-standard
      route-handler                        route-handler
      :else                                :none)))

(defmethod progress-bar :none
  [_]
  [c/progress {:completed-last-step-id 0
               :steps                  []}])

(defmethod progress-bar :ws/all
  [_]
  (let [steps [{:label            @(<t (bp "work_style"))
                :completed?       true
                :route-handler+io [:ws/all nil]}]]
    [c/progress {:completed-last-step-id 1
                 :steps                  (map-indexed (enrich-step 1 1 1)
                                                      steps)}]))

(defmethod progress-bar :ws/import
  [{:keys [ws-uuid io route-handler workflow] :as _params}]
  (let [steps         [{:label            @(<t (bp "work_style"))
                        :completed?       true
                        :route-handler+io [:ws/all nil]}
                       {:label            @(<t (bp "upload_a_file"))
                        :completed?       true
                        :route-handler+io [:ws/import nil]}]
        selected-step (.indexOf (mapv :route-handler+io steps) [route-handler io])
        progress      selected-step]
    [c/progress {:on-select              #(rf/dispatch [:wizard/progress-bar-navigate
                                                        ws-uuid
                                                        workflow
                                                        (:route-handler+io %)])
                 :completed-last-step-id progress
                 :steps                  (map-indexed (enrich-step progress
                                                                   selected-step
                                                                   10)
                                                      steps)}]))

(defmethod progress-bar :ws/independent
  [{:keys [ws-uuid io route-handler workflow] :as _params}]
  (let [steps         [{:label            @(<t (bp "work_style"))
                        :completed?       true
                        :route-handler+io [:ws/all nil]}
                       {:label            @(<t (bp "modules_selection"))
                        :completed?       true
                        :route-handler+io [:ws/independent nil]}]
        selected-step (.indexOf (mapv :route-handler+io steps) [route-handler io])
        progress      selected-step]
    [c/progress {:on-select              #(rf/dispatch [:wizard/progress-bar-navigate
                                                        ws-uuid
                                                        workflow
                                                        (:route-handler+io %)])
                 :completed-last-step-id progress
                 :steps                  (map-indexed (enrich-step progress
                                                                   selected-step
                                                                   10)
                                                      steps)}]))

(defmethod progress-bar :ws/wizard
  [{:keys [ws-uuid io route-handler workflow] :as _params}]
  (let [steps                    [{:label            @(<t (bp "module_output_selections"))
                                   :completed?       false
                                   :route-handler+io [:ws/wizard :output]}
                                  {:label            @(<t (bp "module_input_selections"))
                                   :completed?       false
                                   :route-handler+io [:ws/wizard :input]}
                                  {:label            @(<t (bp "worksheet_review"))
                                   :completed?       false
                                   :route-handler+io [:ws/review nil]}
                                  {:label            @(<t (bp "result_settings"))
                                   :completed?       false
                                   :route-handler+io [:ws/results-settings nil]}
                                  {:label            @(<t (bp "results"))
                                   :completed?       false
                                   :route-handler+io [:ws/results nil]}]
        selected-step            (.indexOf (mapv :route-handler+io steps) [route-handler io])
        *worksheet               (rf/subscribe [:worksheet ws-uuid])
        furthest-route-handler   (->> *worksheet
                                      (deref)
                                      (:worksheet/furthest-visited-route-handler))
        furthest-io              (->> *worksheet
                                      (deref)
                                      (:worksheet/furthest-visited-io))
        furthest-visited-step-id (.indexOf (mapv :route-handler+io steps) [furthest-route-handler furthest-io])
        progress                 furthest-visited-step-id]
    [c/progress {:on-select              #(rf/dispatch [:wizard/progress-bar-navigate
                                                        ws-uuid
                                                        workflow
                                                        (:route-handler+io %)])
                 :completed-last-step-id progress
                 :steps                  (map-indexed (enrich-step progress
                                                                   selected-step
                                                                   furthest-visited-step-id)
                                                      steps)}]))

(defmethod progress-bar :ws/wizard-standard
  [{:keys [ws-uuid io route-handler workflow] :as _params}]
  (let [steps                    [{:label            @(<t (bp "module_output_selections"))
                                   :completed?       false
                                   :route-handler+io [:ws/wizard-standard :output]}
                                  {:label            @(<t (bp "module_input_selections"))
                                   :completed?       false
                                   :route-handler+io [:ws/wizard-standard :input]}
                                  {:label            @(<t (bp "result_settings"))
                                   :completed?       false
                                   :route-handler+io [:ws/results-settings nil]}
                                  {:label            @(<t (bp "results"))
                                   :completed?       false
                                   :route-handler+io [:ws/results nil]}]
        ;; NOTE (kcheung 2025) For some reason when navigating using the progress bar from either inputs or outputs page the io for these route handlers is set to either "input" or "output" when it should be nil. This is a crude way of forcing it to be nil for now.
        io                       (if (or (= route-handler :ws/results-settings)
                                         (= route-handler :ws/results))
                                   nil
                                   io)
        selected-step            (.indexOf (mapv :route-handler+io steps) [route-handler io])
        *worksheet               (rf/subscribe [:worksheet ws-uuid])
        furthest-route-handler   (->> *worksheet
                                      (deref)
                                      (:worksheet/furthest-visited-route-handler))
        furthest-io              (->> *worksheet
                                      (deref)
                                      (:worksheet/furthest-visited-io))
        furthest-visited-step-id (.indexOf (mapv :route-handler+io steps) [furthest-route-handler furthest-io])
        progress                 furthest-visited-step-id]
    [c/progress {:on-select              #(rf/dispatch [:wizard/progress-bar-navigate
                                                        ws-uuid
                                                        workflow
                                                        (:route-handler+io %)])
                 :completed-last-step-id progress
                 :steps                  (map-indexed (enrich-step progress
                                                                   selected-step
                                                                   furthest-visited-step-id)
                                                      steps)}]))

#_{:clj-kondo/ignore [:missing-docstring]}
(defn toolbar [{:keys [ws-uuid] :as params}]
  (prn "toolbar params:" params)
  (let [*loaded? (rf/subscribe [:app/loaded?])
        on-click #(js/console.log (str "Selected!" %))
        tools    [{:icon     :home
                   :label    (bp "home")
                   :on-click #(rf/dispatch [:wizard/navigate-home])}
                  {:icon     :save
                   :label    (bp "save")
                   :on-click #(when ws-uuid
                                (let [worksheet-name @(rf/subscribe [:worksheet/name ws-uuid])]
                                  (rf/dispatch [:wizard/save
                                                ws-uuid
                                                (gstring/format "behave7-%s.bp7"
                                                                (or worksheet-name ws-uuid))])))}
                  {:icon     :print
                   :label    (bp "print")
                   :on-click (when ws-uuid
                               #(rf/dispatch [:toolbar/print ws-uuid]))}
                  (when-not (:jar-local? params)
                    {:icon     :share
                     :label    (bp "share")
                     :on-click #(rf/dispatch [:dev/export-from-vms])})
                  #_{:icon     :zoom-in
                     :label    (bp "zoom-in")
                     :on-click on-click}
                  #_{:icon     :zoom-out
                     :label    (bp "zoom-out")
                     :on-click on-click}]]
    [:div.toolbar
     [:div.toolbar__tools
      (for [tool tools]
        ^{:key (:label tool)}
        [toolbar-tool tool])
      #_[c/text-input {:disabled?   false
                       :error?      false
                       :focused?    false
                       :placeholder "Search"}]]
     (when @*loaded?
       [progress-bar params])]))
