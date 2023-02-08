(ns behave.wizard.events
  (:require [behave-routing.main :refer [routes]]
            [behave.solver       :refer [solve-worksheet]]
            [bidi.bidi           :refer [path-for]]
            [clojure.walk        :refer [postwalk]]
            [re-frame.core       :as rf]))

(rf/reg-event-fx
  :wizard/select-tab
  (fn [_ [_ {:keys [id module io submodule]}]]
    (let [path (path-for routes
                         :ws/wizard
                         :id id
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
  (fn [_ [_
          _*module
          _*submodule
          all-submodules
          {:keys [id module io submodule]}]]
    (let [[i-subs o-subs] (partition-by #(:submodule/io %) (sort-by :submodule/io all-submodules))
          submodules      (if (= io :input) i-subs o-subs)
          next-submodules (rest (drop-while #(not= (:slug %) submodule) (sort-by :submodule/order submodules)))
          path            (cond
                            (seq next-submodules)
                            (path-for routes
                                      :ws/wizard
                                      :id id
                                      :module module
                                      :io io
                                      :submodule (:slug (first next-submodules)))

                            (and (= io :output) (empty? next-submodules))
                            (path-for routes
                                      :ws/wizard
                                      :id id
                                      :module module
                                      :io :input
                                      :submodule (:slug (first i-subs)))

                            (and (= io :input) (empty? next-submodules))
                            (path-for routes
                                      :ws/review
                                      :id id))]
      {:fx [[:dispatch [:navigate path]]]})))

(rf/reg-event-fx
 :wizard/solve
 (fn [_ [_ {:keys [id]} ws-uuid]]
   (let [path (path-for routes :ws/results-settings :id id :results-page :settings)]
     {:fx [[:dispatch [:worksheet/solve ws-uuid]]
           [:dispatch [:navigate path]]]})))

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
