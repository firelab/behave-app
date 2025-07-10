(ns behave.print.views
  (:require [re-frame.core                          :refer [subscribe dispatch]]
            [behave.print.subs]
            [behave.components.results.graphs       :refer [result-graphs]]
            [behave.components.results.diagrams     :refer [result-diagrams]]
            [behave.components.results.matrices     :refer [result-matrices]]
            [behave.components.results.inputs.views :refer [inputs-table]]
            [behave.components.results.table        :refer [directional-result-tables pivot-tables search-tables]]))

(defn- wizard-notes [notes]
  (when (seq notes)
    [:div.wizard-notes
     [:div.wizard-print__header "Run Notes"]
     (doall (for [[id & _rest :as note] notes]
              ^{:key id}
              (let [[_note-id note-name note-content] note]
                [:div.wizard-note
                 [:div.wizard-note__name note-name]
                 [:div.wizard-note__content note-content]])))]))

(defn- epoch->date-string [epoch]
  (.toDateString (js/Date. epoch)))

(defn print-page [{:keys [ws-uuid]}]
  (dispatch [:dev/close-after-print])
  (js/setTimeout #(dispatch [:dev/print]) 1000)
  (let [worksheet           @(subscribe [:worksheet ws-uuid])
        ws-date-created     (:worksheet/created worksheet)
        ws-version          (:worksheet/version worksheet)
        notes               @(subscribe [:wizard/notes ws-uuid])
        graph-data          @(subscribe [:worksheet/result-table-cell-data ws-uuid])
        directional-tables? @(subscribe [:wizard/output-directional-tables? ws-uuid])]
    [:div.print
     [:div.print__header
      [:img {:src "/images/logo.svg"}]
      [:div.print__header__info
       (when ws-version [:div (str "Version: " ws-version)])
       [:div (str "Created: " (epoch->date-string ws-date-created))]]]
     [:div.wizard-print__header "Inputs"]
     [inputs-table ws-uuid]
     [wizard-notes notes]
     [:div.wizard-print__header "Results"]
     [search-tables ws-uuid]
     [pivot-tables ws-uuid]
     (if directional-tables?
       [directional-result-tables ws-uuid]
       [result-matrices ws-uuid])
     [result-graphs ws-uuid graph-data]
     [result-diagrams ws-uuid]]))
