(ns behave.components.toolbar
  (:require [behave.components.core :as c]
            [re-frame.core          :as rf]
            [behave.translate       :refer [<t bp]]))

(def handler->step-number {:ws/all              1
                           :ws/import           2
                           :ws/guided           2
                           :ws/independent      2
                           :ws/review           3
                           :ws/results-settings 4
                           :ws/results          5})

(defn toolbar-tool [{icon-name :icon translation-key :label on-click :on-click :as s}]
  [:div.toolbar__tool
   [:div.toolbar__tool__icon
    [c/button {:variant   "transparent-primary"
               :title     @(<t translation-key)
               :icon-name icon-name
               :on-click  #(on-click s)}]]])

(defn- enrich-step
  [progress selected-step]
  (fn [idx step]
    (let [id (inc idx)]
      (-> step
          (assoc :step-id    id)
          (assoc :completed? (>= progress id))
          (assoc :selected?  (= selected-step id))))))

(defn- get-step-number [route-handler io]
  (cond
    (and (= route-handler :ws/wizard)
         (= io :output))
    1

    (and (= route-handler :ws/wizard)
         (= io :input))
    2

    :else
    (get handler->step-number route-handler 0)))

(defn toolbar [{:keys [id io route-handler] :as _params}]
  (let [on-click      #(js/console.log (str "Selected!" %))
        tools         [{:icon     :home
                        :label    (bp "home")
                        :on-click on-click}
                       {:icon     :save
                        :label    (bp "save")
                        :on-click on-click}
                       {:icon     :print
                        :label    (bp "print")
                        :on-click on-click}
                       {:icon     :share
                        :label    (bp "share")
                        :on-click on-click}
                       {:icon     :zoom-in
                        :label    (bp "zoom-in")
                        :on-click on-click}
                       {:icon     :zoom-out
                        :label    (bp "zoom-out")
                        :on-click on-click}]
        selected-step (get-step-number route-handler io)
        progress      selected-step
        steps         (if id
                        [{:label      @(<t (bp "module_outputs_selections"))
                          :completed? false}
                         {:label      @(<t (bp "module_inputs_selections"))
                          :completed? false}
                         {:label      @(<t (bp "worksheet_review"))
                          :completed? false}
                         {:label      @(<t (bp "result_settings"))
                          :completed? false}
                         {:label      @(<t (bp "run_results"))
                          :completed? false}]
                        [{:label      @(<t (bp "work_style"))
                          :completed? true}
                         {:label      @(<t (bp "modules_selection"))
                          :completed? true}])]
    [:div.toolbar
     [:div.toolbar__tools
      (for [tool tools]
        ^{:key (:label tool)}
        [toolbar-tool tool])
      [c/text-input {:disabled?   false
                     :error?      false
                     :focused?    false
                     :placeholder "Search"}]]
     [c/progress {:on-select              #(do
                                             ;;TODO add navigate dispatch
                                             (rf/dispatch [:state/set [:toolbar :*progress] (:step-id %)])
                                             (rf/dispatch [:state/set [:toolbar :*selected-step] (:step-id %)]))
                  :completed-last-step-id progress
                  :steps                  (map-indexed (enrich-step progress selected-step) steps)}]]))
