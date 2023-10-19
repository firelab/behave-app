(ns behave.wizard.events
  (:require [behave.solver.core            :refer [solve-worksheet]]
            [behave-routing.main           :refer [routes]]
            [bidi.bidi                     :refer [path-for]]
            [clojure.string                :as str]
            [clojure.walk                  :refer [postwalk]]
            [re-frame.core                 :as rf]
            [string-utils.interface        :refer [->str]]
            [vimsical.re-frame.cofx.inject :as inject]))

(rf/reg-event-fx
  :wizard/select-tab
  (fn [_ [_ {:keys [ws-uuid module io submodule]}]]
    (let [path (path-for routes
                         :ws/wizard
                         :ws-uuid ws-uuid
                         :module module
                         :io io
                         :submodule submodule)]
      {:fx              [[:dispatch [:navigate path]]]
       :help/scroll-top nil})))

(rf/reg-event-fx
  :wizard/prev-tab
  (fn [_ _]
    (.back js/history)))

(rf/reg-event-fx
  :wizard/next-tab

  [(rf/inject-cofx ::inject/sub
                   (fn [[_ _ _ _ {:keys [ws-uuid]}]]
                     [:worksheet/modules ws-uuid]))]
  (fn [{modules :worksheet/modules} [_
                                     _*module
                                     _*submodule
                                     all-submodules
                                     {:keys [ws-uuid module io submodule]}]]
    (let [all-submodules  (filter (fn [{op        :submodule/conditionals-operator
                                        research? :submodule/research?
                                        id        :db/id}]
                                    (and (not research?)
                                         @(rf/subscribe [:wizard/show-submodule? ws-uuid id op])))
                                  all-submodules)
          o-subs          (filter #(= :output (:submodule/io %)) (sort-by :submodule/order all-submodules))
          i-subs          (filter #(= :input (:submodule/io %)) (sort-by :submodule/order all-submodules))
          submodules      (if (= io :input) i-subs o-subs)
          next-submodules (rest (drop-while #(not= (:slug %) submodule) (sort-by :submodule/order submodules)))
          next-modules    (rest (drop-while #(not= (str/lower-case (:module/name %)) module) (sort-by :module/order modules)))
          path            (cond
                            (seq next-submodules)
                            (path-for routes
                                      :ws/wizard
                                      :ws-uuid ws-uuid
                                      :module module
                                      :io io
                                      :submodule (:slug (first next-submodules)))

                            (and (= io :output) (empty? next-submodules))
                            (path-for routes
                                      :ws/wizard
                                      :ws-uuid ws-uuid
                                      :module module
                                      :io :input
                                      :submodule (:slug (first i-subs)))

                            (and (= io :input) (empty? next-submodules) (seq next-modules))
                            (let [next-module    (str/lower-case (:module/name (first next-modules)))
                                  next-submodule @(rf/subscribe [:worksheet/first-output-submodule-slug next-module])]
                              (path-for routes
                                        :ws/wizard
                                        :ws-uuid ws-uuid
                                        :module next-module
                                        :io :output
                                        :submodule next-submodule))

                            (and (= io :input) (empty? next-submodules) (empty? next-modules))
                            (path-for routes
                                      :ws/review
                                      :ws-uuid ws-uuid))]
      {:fx [[:dispatch [:navigate path]]]})))

(rf/reg-event-fx
 :wizard/solve
 (fn [{db :db} [_ {:keys [ws-uuid]}]]
   (let [worksheet (solve-worksheet ws-uuid)
         path      (path-for routes :ws/results-settings :ws-uuid ws-uuid :results-page :settings)]
     {:fx [[:dispatch [:navigate path]]
           [:dispatch [:worksheet/update-all-table-filters-from-results ws-uuid]]
           [:dispatch [:worksheet/update-all-y-axis-limits-from-results ws-uuid]]]
      :db (assoc-in db [:state :worksheet] worksheet)})))

(rf/reg-event-fx
 :wizard/before-solve
 (fn [_ [_ {:keys [ws-uuid]}]]
   {:fx [[:dispatch [:worksheet/delete-existing-diagrams ws-uuid]]]}))

(rf/reg-event-fx
 :wizard/during-solve
 (fn [{db :db} [_ {:keys [ws-uuid]}]]
   (let [worksheet (solve-worksheet ws-uuid)]
     {:db (assoc-in db [:state :worksheet] worksheet)})))

(rf/reg-event-fx
 :wizard/after-solve
 (fn [_ [_ {:keys [ws-uuid]}]]
   (let [path (path-for routes :ws/results-settings :ws-uuid ws-uuid :results-page :settings)]
     {:fx [[:dispatch [:navigate path]]
           [:dispatch [:worksheet/update-all-table-filters-from-results ws-uuid]]
           [:dispatch [:worksheet/update-all-y-axis-limits-from-results ws-uuid]]]})))

(defn- remove-nils
  "remove pairs of key-value that has nil value from a (possibly nested) map. also transform map to
  nil if all of its value are nil"
  [nm]
  (postwalk
    (fn [el]
      (if (map? el)
        (not-empty (into {} (remove (comp nil? second)) el))
        el))
    nm))

(rf/reg-event-fx
  :wizard/remove-nils
  (fn [{:keys [db] :as _cfx} [_event-id path]]
    {:db (update-in db path remove-nils)}))

(rf/reg-event-fx
  :wizard/update-inputs
  (fn [_cfx [_event-id group-id repeat-id id value]]
    {:fx [(if (empty? value)
            [:dispatch [:state/update [:worksheet :inputs group-id repeat-id] dissoc id]]
            [:dispatch [:state/set [:worksheet :inputs group-id repeat-id id] value]])
          [:dispatch [:wizard/remove-nils [:state :worksheet :inputs]]]]}))

(rf/reg-event-fx
  :wizard/edit-input
  (fn [_cfx [_event-id route repeat-id var-uuid]]
    {:fx [[:dispatch [:navigate route]]
          [:dispatch-later {:ms       200
                            :dispatch [:wizard/scroll-into-view (str repeat-id  "-" var-uuid)]}]]}))

(rf/reg-event-fx
  :wizard/scroll-into-view
  (fn [_cfx [_event-id id]]
    (let [content (first (.getElementsByClassName js/document "wizard-page__body"))
          section (.getElementById js/document id)
          buffer  (* 0.01 (.-offsetHeight content))
          top     (- (.-offsetTop section) (.-offsetTop content) buffer)]
      (.scroll content #js {:top top :behavior "smooth"}))))

(rf/reg-event-fx
  :wizard/delete
  (fn [_cfx [_event-id group-id repeat-id]]
    {:fx [[:dispatch [:state/update [:worksheet :inputs group-id] dissoc repeat-id]]
          [:dispatch [:state/update [:worksheet :repeat-groups group-id] disj repeat-id]]]}))

(rf/reg-event-fx
 :wizard/edit-note
 (fn [_cfx [_id note-id]]
   {:fx [[:dispatch [:state/set [:worksheet :notes note-id :edit?] true]]]}))

(rf/reg-event-fx
 :wizard/create-note
 (fn [_cfx [_id ws-uuid submodule-uuid submodule-name submodule-io payload]]
   {:fx [[:dispatch [:worksheet/create-note ws-uuid submodule-uuid submodule-name submodule-io payload]]
         [:dispatch [:wizard/toggle-show-add-note-form]]]}))

(rf/reg-event-fx
 :wizard/update-note
 (fn [_cfx [_id note-id payload]]
   {:fx [[:dispatch [:worksheet/update-note note-id payload]]
         [:dispatch [:state/set [:worksheet :notes note-id :edit?] false]]]}))

(rf/reg-event-fx
 :wizard/toggle-show-notes
 (fn [_ _]
   {:fx [[:dispatch [:state/update [:worksheet :show-notes?] not]]]}))

(rf/reg-event-fx
 :wizard/toggle-show-add-note-form
 (fn [_cfx _query]
   {:fx [[:dispatch [:state/update [:worksheet :show-add-note-form?] not]]]}))


(rf/reg-event-fx
 :wizard/results-select-tab
 (fn [_cfx [_ {:keys [tab]}]]
   {:fx [[:dispatch [:state/set [:worksheet :results :tab-selected] tab]]
         [:dispatch [:wizard/scroll-into-view (name tab)]]]}))
(rf/reg-event-fx
 :wizard/progress-bar-navigate
 [(rf/inject-cofx ::inject/sub
                  (fn [_]
                    [:state [:worksheet :*workflow]]))
  (rf/inject-cofx ::inject/sub
                  (fn [[_ io]]
                    [:wizard/first-module+submodule io]))]
 (fn [{module                 :state
       first-module+submodule :wizard/first-module+submodule} [_ ws-uuid route-handler+io]]
   (let [[handler io]          route-handler+io
         [ws-module submodule] first-module+submodule]
     (when-let [path (cond
                       (= handler :ws/independent)
                       (str "/worksheets/" (->str module))

                       io
                       (path-for routes
                                 :ws/wizard
                                 :ws-uuid  ws-uuid
                                 :module ws-module
                                 :io io
                                 :submodule submodule)

                       (= handler :ws/result-settings)
                       (path-for routes :ws/results-settings :ws-uuid ws-uuid :results-page :settings)

                       :else
                       (path-for routes handler :ws-uuid ws-uuid))]
       {:fx (cond-> [[:dispatch [:navigate path]]]

              io
              (conj [:dispatch [:worksheet/set-furthest-vistited-step ws-uuid handler io]]))}))))

(rf/reg-event-fx
 :worksheet/map-units-enabled?
 (fn [_ _]
   {:fx [[:dispatch [:state/update [:worksheet :show-map-units?] not]]]}))

(rf/reg-event-fx
 :wizard/insert-range-input
 (fn [_ [_ ws-uuid group-uuid repeat-id gv-uuid value]]
   {:fx [[:dispatch [:worksheet/upsert-input-variable ws-uuid group-uuid repeat-id gv-uuid value]]
         [:dispatch [:wizard/toggle-show-range-selector gv-uuid repeat-id]]]}))

(rf/reg-event-fx
 :wizard/toggle-show-range-selector
 (fn [_ [_ gv-uuid repeat-id]]
   {:fx [[:dispatch [:state/update [:show-range-selector? gv-uuid repeat-id] not]]]}))
