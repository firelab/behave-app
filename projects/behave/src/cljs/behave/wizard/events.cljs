(ns behave.wizard.events
  (:require [behave-routing.main           :refer [routes current-route-order]]
            [behave.lib.units              :refer [convert]]
            [browser-utils.core :refer [scroll-top!]]
            [behave.solver.core            :refer [solve-worksheet]]
            [behave.vms.store              :as vms]
            [behave.store                  :as s]
            [bidi.bidi                     :refer [path-for]]
            [goog.string                   :as gstring]
            [clojure.string                :as str]
            [clojure.walk                  :refer [postwalk]]
            [datascript.core               :as d]
            [re-frame.core                 :as rf]
            [number-utils.interface        :refer [is-numeric? parse-float]]
            [string-utils.interface        :refer [->str]]
            [vimsical.re-frame.cofx.inject :as inject]))

;;; Helpers

(defn- convert-values
  [from to v & [precision]]
  {:pre [(string? from) (string? to) (or (nil? v) (string? v))]}
  (if (empty? v)
    nil
    (let [convert-fn #(convert % from to)
          values     (->> (str/split (str v) #"[, ]")
                          (remove empty?))]
      (when (every? is-numeric? values)
        (->> (map (comp convert-fn parse-float) values)
             (map #(.toFixed % (or precision 2)))
             (str/join ","))))))

;;; Subscriptions

(rf/reg-event-fx
 :wizard/select-tab
 (fn [_ [_ {:keys [ws-uuid module io submodule workflow]}]]
   (let [path (path-for routes
                        :ws/wizard-guided
                        :ws-uuid ws-uuid
                        :workflow workflow
                        :module module
                        :io io
                        :submodule submodule)]
     {:fx              [[:dispatch [:navigate path]]]
      :help/scroll-top nil})))

(rf/reg-event-fx
 :wizard/back
 (rf/inject-cofx ::inject/sub (fn [_] [:wizard/get-cached-new-worksheet-or-import]))
 (fn [{new-or-import :wizard/get-cached-new-worksheet-or-import} _]
   (let [current-path       (str/replace
                             (. (. js/document -location) -href)
                             #"(^.*(?=(/worksheets)))"
                             "")
         current-path-index (.indexOf @current-route-order current-path)
         next-path          (cond
                              (and (zero? current-path-index) (= new-or-import :import))
                              "/worksheets/import"

                              (and (zero? current-path-index) (= new-or-import :new-worksheet))
                              "/worksheets/module-selection"

                              :else
                              (get @current-route-order (dec current-path-index)))]
     {:fx [[:dispatch [:navigate next-path]]]})))

(rf/reg-event-fx
 :wizard/next
 (fn [_]
   (let [current-path       (str/replace
                             (. (. js/document -location) -href)
                             #"(^.*(?=(/worksheets)))"
                             "")
         current-path-index (.indexOf @current-route-order current-path)
         next-path          (get @current-route-order (inc current-path-index))]
     {:fx [[:dispatch [:navigate next-path]]]})))

(rf/reg-event-fx
 :wizard/before-solve
 (fn [_ [_ {:keys [ws-uuid]}]]
   {:fx [[:dispatch [:worksheet/proccess-conditonally-set-output-group-variables ws-uuid]]
         [:dispatch [:worksheet/process-search-table-output-group-variables ws-uuid]]
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
 (fn [_ [_ {:keys [ws-uuid workflow]}]]
   (let [path (path-for routes :ws/results-settings
                        :ws-uuid ws-uuid
                        :workflow workflow
                        :results-page :settings)]
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
                           :dispatch [:wizard/scroll-into-view
                                      "wizard-page__body"
                                      (str repeat-id  "-" var-uuid)]}]]}))

(rf/reg-event-fx
 :wizard/scroll-into-view
 (fn [_cfx [_event-id class id]]
   (let [content (first (.getElementsByClassName js/document class))
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
         [:dispatch [:wizard/scroll-into-view "review-wizard-page__body" (name tab)]]]}))

(rf/reg-event-fx
 :wizard/progress-bar-navigate
 [(rf/inject-cofx ::inject/sub
                  (fn [[_ ws-uuid _ [_ io]]]
                    (when ws-uuid
                      [:wizard/first-module+submodule ws-uuid io])))]
 (fn [{first-module+submodule :wizard/first-module+submodule} [_ ws-uuid workflow route-handler+io]]
   (let [[handler io]          route-handler+io
         [ws-module submodule] first-module+submodule]
     (when-let [path (cond
                       (= handler :ws/home)
                       (str "/worksheets/")

                       (= handler :ws/module-selection)
                       (str "/worksheets/module-selection")

                       (and (= handler :ws/wizard-standard) io)
                       (path-for routes :ws/wizard-standard
                                 {:ws-uuid  ws-uuid
                                  :workflow :standard
                                  :io       io})

                       (and (= handler :ws/wizard-guided) io)
                       (path-for routes :ws/wizard-guided
                                 {:ws-uuid   ws-uuid
                                  :workflow  :guided
                                  :module    ws-module
                                  :io        io
                                  :submodule submodule})

                       (= handler :ws/results-settings)
                       (path-for routes :ws/results-settings
                                 {:ws-uuid      ws-uuid
                                  :workflow     workflow
                                  :results-page :settings})

                       :else
                       (path-for routes handler
                                 {:ws-uuid  ws-uuid
                                  :workflow workflow}))]
       {:fx (cond-> [[:dispatch [:navigate path]]]
              (= handler :ws/home)
              (into [[:dispatch [:state/set [:sidebar :*modules] nil]]
                     [:dispatch [:state/set [:worksheet :*modules] nil]]]))}))))

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
                   (conj [:dispatch [:worksheet/set-furthest-vistited-step ws-uuid :ws/wizard-guided :input]]))]
     {:fx effects})))


;; Update input variable with units
;; If units provided is different from the stored units, set the progress bar's furthest step back to inputs.
;; Also clear the input value from the worksheet.
(rf/reg-event-fx
 :wizard/update-input-units
 (fn [_ [_ {:keys [ws-uuid group-uuid repeat-id gv-uuid new-units-uuid old-units-uuid value]}]]
   (let [new-unit-short-code    (:unit/short-code (vms/entity-from-uuid new-units-uuid))
         old-unit-short-code    (:unit/short-code (vms/entity-from-uuid old-units-uuid))
         different-unit-chosen? (not= new-unit-short-code old-unit-short-code)
         new-value              (convert-values old-unit-short-code new-unit-short-code value)
         effects                (cond-> [[:dispatch [:worksheet/update-input-units
                                                     ws-uuid group-uuid repeat-id gv-uuid new-units-uuid]]]

                                  different-unit-chosen?
                                  (conj [:dispatch [:worksheet/set-furthest-vistited-step ws-uuid :ws/wizard-guided :input]])

                                  (and (some? new-value) different-unit-chosen?)
                                  (conj [:dispatch [:wizard/upsert-input-variable
                                                    ws-uuid group-uuid repeat-id gv-uuid new-value]]))]
     {:fx effects})))

(rf/reg-event-fx
 :wizard/save
 (fn [_ [_ ws-uuid file-name]]
   (s/save-worksheet! {:ws-uuid   ws-uuid
                       :file-name file-name})
   {}))

(rf/reg-event-fx
 :wizard/navigate-to-latest-worksheet
 (fn [_ [_ workflow]]
   (let [ws-uuid (d/q '[:find ?uuid .
                        :in $
                        :where [?e :worksheet/uuid ?uuid]]
                      @@s/conn)]
     (reset! current-route-order @(rf/subscribe [:wizard/route-order ws-uuid workflow]))
     {:fx [[:dispatch [:wizard/next]]]})))

(rf/reg-event-fx
 :wizard/open
 (fn [_ [_ file]]
   (s/open-worksheet! {:file file})))


(rf/reg-event-fx
 :wizard/new-worksheet
 (fn [_ [_ nname modules submodule workflow]]
   (s/new-worksheet! nname modules submodule workflow)))

(rf/reg-event-fx
 :wizard/toggle-expand

 (fn [{:keys [db]}]
   (let [sidebar-hidden?   (get-in db [:state :sidebar :hidden?])
         help-area-hidden? (get-in db [:state :help-area :hidden?])
         all-hidden?       (and sidebar-hidden? help-area-hidden?)]
     {:fx [[:dispatch [:state/set [:sidebar :hidden?] (not all-hidden?)]]
           [:dispatch [:state/set [:help-area :hidden?] (not all-hidden?)]]]})))

(rf/reg-event-fx
 :wizard/navigate-home
 (fn []
   {:fx [[:dispatch [:navigate "/"]]
         [:dispatch [:state/set [:sidebar :*modules] nil]]
         [:dispatch [:state/set [:worksheet :*modules] nil]]]}))

(rf/reg-event-fx
 :wizard/toggle-disclaimer
 [(rf/inject-cofx ::inject/sub (fn [_] [:local-storage/get]))
  (rf/inject-cofx ::inject/sub (fn [_] [:state :show-disclaimer?]))]
 (fn [{local-storage :local-storage/get
       state         :state}]
   {:fx [[:dispatch [:local-storage/update-in
                     [:show-disclaimer?]
                     (not (:show-disclaimer? local-storage))]]
         [:dispatch [:state/set :show-disclaimer? (not state)]]]}))

(rf/reg-event-fx
 :wizard/update-cached-new-worksheet-or-import
 (fn [_ [_ new-worksheet-or-import]]
   {:fx [[:dispatch [:local-storage/update-in [:state :*new-or-import] new-worksheet-or-import]]
         [:dispatch [:state/set :*new-or-import new-worksheet-or-import]]]}))

(rf/reg-event-fx
 :wizard/update-cached-workflow
 (fn [_ [_ workflow]]
   {:fx [[:dispatch [:local-storage/update-in [:state :*workflow] workflow]]
         [:dispatch [:state/set :*new-or-import workflow]]]}))

(rf/reg-event-fx
 :wizard/standard-navigate-io-tab
 (fn [_ [_ ws-uuid io]]
   {:fx [[:dispatch [:navigate (path-for routes :ws/wizard-standard
                                         {:ws-uuid  ws-uuid
                                          :workflow :standard
                                          :io       io})]]]
    :browser/scroll-top {}}))
