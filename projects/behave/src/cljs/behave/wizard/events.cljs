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
  (fn [{db :db} [_ {:keys [id]}]]
    (let [{:keys [state]} db
          worksheet       (solve-worksheet (:worksheet state))
          path            (path-for routes :ws/results :id id)]
      {:fx [[:dispatch [:navigate path]]]
       :db (assoc-in db [:state :worksheet] worksheet)})))

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

(def ->check-limit
  (re-frame.core/->interceptor
    :id     :check-limit
    :after  (fn [context]
              (let [{:keys [event db]}              (:coeffects context)
                    continuous-input-limit          (get-in db [:state :worksheet :continuous-input-limit])
                    continuous-input-count          (get-in db [:state :worksheet :continuous-input-count])
                    limit-reached?                  (>= continuous-input-count continuous-input-limit)
                    [_ group-id repeat-id id value] event
                    input-exists?                   (some? (get-in db [:state :worksheet :inputs group-id repeat-id id]))]
                (if (and limit-reached?
                         (seq value)
                         (not input-exists?))
                  (-> context
                      (assoc-in [:effects :dispatch] [:state/set :warn-continuous-input-limit true]))
                  (assoc-in context [:effects :dispatch] [:state/set :warn-continuous-input-limit false]))))))

(defn- count-continusous-variable-inputs
  [k->v depth-to-count]
  (letfn [(dfs-walk [k->v cur-depth]
            (when-let [map-entries (seq k->v)]
              (if (zero? cur-depth)
                (map key map-entries)
                (into (dfs-walk (-> map-entries first val) (dec cur-depth))
                      (dfs-walk (rest map-entries) cur-depth)))))]
    (let [variable-ids (dfs-walk k->v depth-to-count)
          variables    @(rf/subscribe [:vms/pull-many '[{:variable/_group-variables [:variable/kind]}]
                                       variable-ids])]
      (->> variables
           (filter (fn continuous? [variable]
                     (= (-> variable :variable/_group-variables first :variable/kind)
                        "continuous")))
           (count)))))

(rf/reg-event-fx
  :wizard/update-input-count
  (fn [_cfx [_event-id]]
    (let [inputs   (rf/subscribe [:state [:worksheet :inputs]])
          id-count (count-continusous-variable-inputs @inputs 2)]
      {:dispatch [:state/set [:worksheet :continuous-input-count] id-count]})))

(rf/reg-event-fx
  :wizard/update-inputs
  [->check-limit]
  (fn [_cfx [_event-id group-id repeat-id id value]]
    {:fx [(if (empty? value)
            [:dispatch [:state/update [:worksheet :inputs group-id repeat-id] dissoc id]]
            [:dispatch [:state/set [:worksheet :inputs group-id repeat-id id] value]])
          [:dispatch [:wizard/update-input-count]]
          [:dispatch [:wizard/remove-nils [:state :worksheet :inputs]]]]}))
