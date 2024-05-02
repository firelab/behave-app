(ns behave.wizard.events
  (:require [behave-routing.main           :refer [routes]]
            [behave.solver.core            :refer [solve-worksheet]]
            [behave.store                  :as s]
            [bidi.bidi                     :refer [path-for]]
            [clojure.string                :as str]
            [clojure.walk                  :refer [postwalk]]
            [datascript.core               :as d]
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
 :wizard/back

 [(rf/inject-cofx
   ::inject/sub
   (fn [[_ ws-uuid]]
     [:wizard/route-order ws-uuid]))]

 (fn [{path-order :wizard/route-order} _]
   (let [current-path       (str/replace
                             (. (. js/document -location) -href)
                             #"(^.*(?=(/worksheets)))"
                             "")
         current-path-index (.indexOf path-order current-path)
         next-path          (cond
                              (and (zero? current-path-index) @s/worksheet-from-file?)
                              "/worksheets/import"

                              (and (zero? current-path-index))
                              "/worksheets/independent"

                              :else
                              (get path-order (dec current-path-index)))]
     {:fx [[:dispatch [:navigate next-path]]]})))

(rf/reg-event-fx
 :wizard/next

 [(rf/inject-cofx
   ::inject/sub
   (fn [[_ ws-uuid]]
     [:wizard/route-order ws-uuid]))]

 (fn [{path-order :wizard/route-order} _]
   (let [current-path       (str/replace
                             (. (. js/document -location) -href)
                             #"(^.*(?=(/worksheets)))"
                             "")
         current-path-index (.indexOf path-order current-path)
         next-path          (get path-order (inc current-path-index))]
     {:fx [[:dispatch [:navigate next-path]]]})))

(rf/reg-event-fx
 :wizard/before-solve
 (fn [_ [_ {:keys [ws-uuid]}]]
   {:fx [[:dispatch [:worksheet/proccess-conditonally-set-output-group-variables ws-uuid]]
         [:dispatch [:worksheet/proccess-conditonally-set-input-group-variables ws-uuid]]
         [:dispatch [:worksheet/delete-existing-diagrams ws-uuid]]
         [:dispatch [:worksheet/delete-existing-result-table ws-uuid]]
         [:dispatch [:state/set :worksheet-computing? true]]]}))

(rf/reg-event-fx
 :wizard/solve
 (fn [{db :db} [_ {:keys [ws-uuid]}]]
   (let [worksheet (solve-worksheet ws-uuid)]
     {:db (assoc-in db [:state :worksheet] worksheet)})))

(rf/reg-event-fx
 :wizard/after-solve
 (fn [_ [_ {:keys [ws-uuid]}]]
   (let [path (path-for routes :ws/results-settings :ws-uuid ws-uuid :results-page :settings)]
     {:fx [[:dispatch [:navigate path]]
           [:dispatch [:worksheet/update-all-table-filters-from-results ws-uuid]]
           [:dispatch [:worksheet/update-all-y-axis-limits-from-results ws-uuid]]
           [:dispatch [:worksheet/set-default-graph-settings ws-uuid]]
           [:dispatch [:state/set :worksheet-computing? false]]]})))

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
                  (fn [[_ ws-uuid [_ io]]]
                    [:wizard/first-module+submodule ws-uuid io]))]
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
       {:fx [[:dispatch [:navigate path]]]}))))

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

;; Upserts input variable with the given value.
;; If value provided is different from the stored value, set the progress bar's furthest step back to inputs.
(rf/reg-event-fx
 :wizard/upsert-input-variable

 (rf/inject-cofx ::inject/sub
                 (fn [[_ ws-uuid group-uuid repeat-id group-variable-uuid _]]
                   [:worksheet/input-value ws-uuid group-uuid repeat-id group-variable-uuid]))

 (fn [{ws-input-value :worksheet/input-value} [_ ws-uuid group-uuid repeat-id group-variable-uuid value]]
   (let [effects (cond-> [[:dispatch [:worksheet/upsert-input-variable
                                      ws-uuid group-uuid repeat-id group-variable-uuid value]]]

                   (not= ws-input-value value)
                   (conj [:dispatch [:worksheet/set-furthest-vistited-step ws-uuid :ws/wizard :input]]))]
     {:fx effects})))


;; Update input variable with units
;; If units provided is different from the stored units, set the progress bar's furthest step back to inputs.
;; Also clear the input value from the worksheet.
(rf/reg-event-fx
 :wizard/update-input-units
 [(rf/inject-cofx ::inject/sub
                  (fn [[_ ws-uuid group-uuid repeat-id group-variable-uuid _]]
                    [:worksheet/input ws-uuid group-uuid repeat-id group-variable-uuid]))
  (rf/inject-cofx ::inject/sub
                  (fn [[_ _ _ _ group-variable-uuid]]
                    [:vms/native-units group-variable-uuid]))]
 (fn [{input            :worksheet/input
       vms-native-units :vms/native-units} [_ ws-uuid group-uuid repeat-id group-variable-uuid units]]
   (let [ws-input-units         (:input/units input)
         different-unit-chosen? (or (and (nil? ws-input-units)
                                         (not= vms-native-units units))
                                    (and (some? ws-input-units) (not= ws-input-units units)))
         effects                (cond-> [[:dispatch [:worksheet/update-input-units
                                                     ws-uuid group-uuid repeat-id group-variable-uuid units]]]

                                  different-unit-chosen?
                                  (conj [:dispatch [:worksheet/set-furthest-vistited-step ws-uuid :ws/wizard :input]])

                                  different-unit-chosen?
                                  (conj [:dispatch [:worksheet/clear-input-value (:db/id input)]]))]
     {:fx effects})))

(rf/reg-event-fx
 :wizard/save
 (fn [_ [_ ws-uuid file-name]]
   (s/save-worksheet! {:ws-uuid   ws-uuid
                       :file-name file-name})))

(rf/reg-event-fx
 :wizard/navigate-to-latest-worksheet
 (fn [_]
   (let [ws-uuid (d/q '[:find ?uuid .
                        :in $
                        :where [?e :worksheet/uuid ?uuid]]
                      @@s/conn)]
     {:fx [[:dispatch [:wizard/next ws-uuid]]]})))

(rf/reg-event-fx
 :wizard/open
 (fn [_ [_ file]]
   (s/open-worksheet! {:file file})))


(rf/reg-event-fx
 :wizard/new-worksheet
 (fn [_ [_ nname modules submodule]]
   (s/new-worksheet! nname modules submodule)))
