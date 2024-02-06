(ns behave.components.toolbar
  (:require [behave.components.core :as c]
            [behave.translate       :refer [<t bp]]
            [goog.string            :as gstring]
            [re-frame.core          :as rf]))

(def step-kw->number
  {:work-style      1
   :module          2
   :module-outputs  1
   :module-inputs   2
   :review          3
   :result-settings 4
   :results         5})

(def route-handler->step-number
  {:ws/all              1
   :ws/import           2
   :ws/guided           2
   :ws/independent      2
   :ws/review           3
   :ws/results-settings 4
   :ws/results          5})

(defn get-step-kw [route-handler io]
  (case [route-handler io]
    [:ws/wizard :input]        :module-inputs
    [:ws/wizard :output]       :module-outputs
    [:ws/review nil]           :review
    [:ws/results-settings nil] :result-settings
    [:ws/results nil]          :results
    [:ws/all nil]              :work-style
    [:ws/import nil]           :module
    [:ws/guided nil]           :module
    [:ws/independent nil]      :module))

(defn toolbar-tool [{icon-name :icon translation-key :label on-click :on-click :as s}]
  [:div.toolbar__tool
   [:div.toolbar__tool__icon
    [c/button {:variant   "transparent-primary"
               :title     @(<t translation-key)
               :icon-name icon-name
               :on-click  #(on-click s)}]]])

(defn- enrich-step
  [progress selected-step furthest-step-completed-id]
  (fn [idx step]
    (let [id (inc idx)]
      (cond-> step
        (> id furthest-step-completed-id) (assoc :on-select #(println "disabled!"))
        :always                           (assoc :step-id    id)
        :always                           (assoc :completed? (>= progress id))
        :always                           (assoc :selected?  (= selected-step id))))))

(defn get-step-number [route-handler io]
  (cond
    (and (= route-handler :ws/wizard)
         (= io :output))
    1

    (and (= route-handler :ws/wizard)
         (= io :input))
    2

    :else
    (get route-handler->step-number route-handler 0)))

(defmulti progress-bar
  (fn [{:keys [ws-uuid]}]
    (if ws-uuid
      :post-worksheet-creation
      :pre-worksheet-creation)))

(defmethod progress-bar :pre-worksheet-creation
  [{:keys [ws-uuid io route-handler] :as _params}]
  (let [selected-step (get-step-number route-handler io)
        progress      selected-step
        steps         [{:label            @(<t (bp "work_style"))
                        :completed?       true
                        :route-handler+io [:ws/all nil]}
                       {:label            @(<t (bp "modules_selection"))
                        :completed?       true
                        :route-handler+io [:ws/independent nil]}]]
    [c/progress {:on-select              #(rf/dispatch [:wizard/progress-bar-navigate ws-uuid (:route-handler+io %)])
                 :completed-last-step-id progress
                 :steps                  (map-indexed (enrich-step progress
                                                                   selected-step
                                                                   10)
                                                      steps)}]))

(defmethod progress-bar :post-worksheet-creation
  [{:keys [ws-uuid io route-handler] :as _params}]
  (let [selected-step            (get-step-number route-handler io)
        steps                    [{:label            @(<t (bp "module_outputs_selections"))
                                   :completed?       false
                                   :route-handler+io [:ws/wizard :output]}
                                  {:label            @(<t (bp "module_inputs_selections"))
                                   :completed?       false
                                   :route-handler+io [:ws/wizard :input]}
                                  {:label            @(<t (bp "worksheet_review"))
                                   :completed?       false
                                   :route-handler+io [:ws/review nil]}
                                  {:label            @(<t (bp "result_settings"))
                                   :completed?       false
                                   :route-handler+io [:ws/result-settings nil]}
                                  {:label            @(<t (bp "run_results"))
                                   :completed?       false
                                   :route-handler+io [:ws/results nil]}]
        *worksheet               (rf/subscribe [:worksheet ws-uuid])
        furthest-visited-step-id (->> *worksheet
                                      (deref)
                                      (:worksheet/furthest-visited-step)
                                      (get step-kw->number))
        progress                 furthest-visited-step-id]
    [c/progress {:on-select              #(rf/dispatch [:wizard/progress-bar-navigate ws-uuid (:route-handler+io %)])
                 :completed-last-step-id progress
                 :steps                  (map-indexed (enrich-step progress
                                                                   selected-step
                                                                   furthest-visited-step-id)
                                                      steps)}]))

(defn toolbar [{:keys [ws-uuid] :as params}]
  (prn "ws-uuid" ws-uuid)
  (let [*loaded? (rf/subscribe [:app/loaded?])
        on-click #(js/console.log (str "Selected!" %))
        tools    [{:icon     :home
                   :label    (bp "home")
                   :on-click on-click}
                  {:icon     :save
                   :label    (bp "save")
                   :on-click #(when ws-uuid
                                (let [worksheet-name @(rf/subscribe [:worksheet/name ws-uuid])]
                                  (rf/dispatch [:wizard/save
                                                ws-uuid
                                                (gstring/format "behave7-%s.sqlite"
                                                                (or worksheet-name ws-uuid))])))}
                  {:icon     :print
                   :label    (bp "print")
                   :on-click #(rf/dispatch [:toolbar/print ws-uuid])}
                  {:icon     :share
                   :label    (bp "share")
                   :on-click #(rf/dispatch [:dev/export-from-vms])}
                  {:icon     :zoom-in
                   :label    (bp "zoom-in")
                   :on-click on-click}
                  {:icon     :zoom-out
                   :label    (bp "zoom-out")
                   :on-click on-click}]]
    [:div.toolbar
     [:div.toolbar__tools
      (for [tool tools]
        ^{:key (:label tool)}
        [toolbar-tool tool])
      [c/text-input {:disabled?   false
                     :error?      false
                     :focused?    false
                     :placeholder "Search"}]]
     (when @*loaded?
       [progress-bar params])]))
