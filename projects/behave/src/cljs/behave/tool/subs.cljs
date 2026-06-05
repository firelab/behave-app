(ns behave.tool.subs
  (:require [behave.lib.units       :refer [convert]]
            [behave.translate       :refer [<t]]
            [behave.vms.store             :as s]
            [clojure.set                  :refer [rename-keys]]
            [absurder-sql.datascript.core :as d]
            [goog.string                  :as gstring]
            [number-utils.interface       :refer [parse-float]]
            [re-frame.core                :refer [reg-sub] :as rf]))

;;; Helpers

(defn- in-range?
  "True when v falls within [v-min v-max]. Nil bounds are open."
  [v-min v-max v]
  (cond
    (and (some? v-min) (some? v-max)) (<= v-min v v-max)
    (some? v-min)                     (<= v-min v)
    (some? v-max)                     (<= v v-max)
    :else                             true))

(defn- converted-bounds
  "Converts var-min/var-max from native to selected units (floored)."
  [native-unit-uuid selected-unit-uuid var-min var-max]
  (let [from-sc (when native-unit-uuid
                  (:unit/short-code (d/entity @@s/vms-conn [:bp/uuid native-unit-uuid])))
        to-sc   (when selected-unit-uuid
                  (:unit/short-code (d/entity @@s/vms-conn [:bp/uuid selected-unit-uuid])))]
    (if (and from-sc to-sc (not= from-sc to-sc))
      [(when var-min (convert var-min from-sc to-sc 0))
       (when var-max (convert var-max from-sc to-sc 0))]
      [var-min var-max])))

(defn- outside-range?
  [native-unit-uuid selected-unit-uuid var-min var-max value]
  (when (and (or var-min var-max) value (seq (str value)))
    (when-let [v (parse-float (str value))]
      (let [[adj-min adj-max] (converted-bounds native-unit-uuid selected-unit-uuid var-min var-max)]
        (not (in-range? adj-min adj-max v))))))

(defn- range-placeholder
  "Returns a range string like '1 - 60' with bounds converted to selected units."
  [native-unit-uuid selected-unit-uuid var-min var-max]
  (when (and var-min var-max)
    (let [[adj-min adj-max] (converted-bounds native-unit-uuid selected-unit-uuid var-min var-max)]
      (str (int adj-min) " - " (int adj-max)))))

(defn- outside-range-error-msg
  "Returns an error message when value is outside the converted range."
  [native-unit-uuid selected-unit-uuid var-min var-max value]
  (when (and (or var-min var-max) value (seq (str value)))
    (when-let [v (parse-float (str value))]
      (let [[adj-min adj-max] (converted-bounds native-unit-uuid selected-unit-uuid var-min var-max)]
        (when-not (in-range? adj-min adj-max v)
          (gstring/format "Error: Not within range (min: %2f, max: %2f)" adj-min adj-max))))))

;;; Subscriptions

(reg-sub
 :tool/show-tool-selector?
 (fn [db _]
   (let [state (get-in db [:state :sidebar :*tools-or-settings])]
     (= state :tools))))

(reg-sub
 :tool/all-tools
 (fn [_ _]
   (let [eids (d/q '[:find [?e ...]
                     :in $
                     :where
                     [?e :tool/name ?name]]
                   @@s/vms-conn)]
     (map #(d/entity @@s/vms-conn %) eids))))

(reg-sub
 :tool/selected-tool-uuid
 (fn [db _]
   (get-in db [:state :tool :selected-tool])))

(reg-sub
 :tool/selected-subtool-uuid
 (fn [db _]
   (get-in db [:state :tool :selected-subtool])))

(reg-sub
 :tool/entity
 (fn [_ [_ tool-uuid]]
   (d/entity @@s/vms-conn [:bp/uuid tool-uuid])))

(defn- enrich-subtool-variable [subtool-variable]
  (let [variable-data (rename-keys (first (:variable/_subtool-variables subtool-variable))
                                   {:bp/uuid :variable/uuid})]
    (-> subtool-variable
        (dissoc :variable/_subtool-variables)
        (merge variable-data)
        (dissoc :variable/subtool-variables)
        (update :variable/kind keyword))))

(reg-sub
 :subtool/encriched-subtool-variables
 (fn [_ [_ subtool-uuid]]
   (let [subtool (d/pull @@s/vms-conn '[* {:subtool/variables
                                           [* {:variable/_subtool-variables
                                               [* {:variable/list
                                                   [* {:list/options
                                                       [* {:list-option/color-tag-ref [*]}]}]}]}]}]
                         [:bp/uuid subtool-uuid])]
     (->> (:subtool/variables subtool)
          (mapv enrich-subtool-variable)
          (sort-by :subtool-variable/order)))))

(reg-sub
 :subtool/input-variables
 (fn [[_ subtool-eid]]
   (rf/subscribe [:subtool/encriched-subtool-variables subtool-eid]))
 (fn [variables _]
   (filter #(= (:subtool-variable/io %) :input) variables)))

(reg-sub
 :subtool/output-variables
 (fn [[_ subtool-eid]]
   (rf/subscribe [:subtool/encriched-subtool-variables subtool-eid]))
 (fn [variables _]
   (filter #(= (:subtool-variable/io %) :output) variables)))

(reg-sub
 :tool/input-value
 (fn [db [_ tool-uuid subtool-uuid subtool-variable-uuid]]
   (get-in db [:state
               :tool
               :data
               tool-uuid
               subtool-uuid
               :tool/inputs
               subtool-variable-uuid
               :input/value])))

(reg-sub
 :tool/input-units
 (fn [db [_ tool-uuid subtool-uuid subtool-variable-uuid]]
   (get-in db [:state
               :tool
               :data
               tool-uuid
               subtool-uuid
               :tool/inputs
               subtool-variable-uuid
               :input/units-uuid])))

(reg-sub
 :tool/output-value
 (fn [db [_ tool-uuid subtool-uuid subtool-variable-uuid]]
   (get-in db [:state
               :tool
               :data
               tool-uuid
               subtool-uuid
               :tool/outputs
               subtool-variable-uuid
               :output/value])))

(reg-sub
 :tool/all-inputs
 (fn [db [_ tool-uuid subtool-uuid]]
   (get-in db [:state
               :tool
               :data
               tool-uuid
               subtool-uuid
               :tool/inputs])))

(reg-sub
 :tool/all-outputs
 (fn [db [_ tool-uuid subtool-uuid]]
   (->> (d/q '[:find [?output-uuids ...]
               :in $ ?uuid
               :where
               [?s  :bp/uuid ?uuid]
               [?s  :subtool/variables ?sv]
               [?sv :subtool-variable/io :output]
               [?sv :bp/uuid ?output-uuids]]
             @@s/vms-conn subtool-uuid)
        (map (fn [output-uuid]
               [output-uuid (get-in db [:state
                                        :tool
                                        :data
                                        tool-uuid
                                        subtool-uuid
                                        :tool/outputs
                                        output-uuid
                                        :output/units-uuid-uuid])])))))

(reg-sub
 :tool/sv->translated-name
 (fn [_ [_ subtool-variable-uuid]]
   (when-let [translation-key (->> (d/entity @@s/vms-conn [:bp/uuid subtool-variable-uuid])
                                   :subtool-variable/translation-key)]
     @(<t translation-key))))

(reg-sub
 :tool/input-range-placeholder
 (fn [_ [_ native-unit-uuid var-min var-max effective-unit-uuid]]
   (range-placeholder native-unit-uuid effective-unit-uuid var-min var-max)))

(reg-sub
 :tool/input-error-msg
 (fn [_ [_ _ _ _ native-unit-uuid var-min var-max effective-unit-uuid value]]
   (outside-range-error-msg native-unit-uuid effective-unit-uuid var-min var-max value)))

(reg-sub
 :tool/all-inputs-filled?
 (fn [[_ tool-uuid subtool-uuid]]
   [(rf/subscribe [:subtool/input-variables subtool-uuid])
    (rf/subscribe [:tool/all-inputs tool-uuid subtool-uuid])])
 (fn [[input-variables all-inputs] _]
   (every? (fn [{sv-uuid :bp/uuid}]
             (some? (:input/value (get all-inputs sv-uuid))))
           input-variables)))

(reg-sub
 :tool/any-input-outside-range?
 (fn [[_ tool-uuid subtool-uuid]]
   [(rf/subscribe [:subtool/input-variables subtool-uuid])
    (rf/subscribe [:tool/all-inputs tool-uuid subtool-uuid])
    (rf/subscribe [:settings/tool-units-system])])
 (fn [[input-variables all-inputs units-system] _]
   (boolean
    (some (fn [{var-min      :variable/minimum
                var-max      :variable/maximum
                sv-uuid      :bp/uuid
                kind         :variable/kind
                nat-uuid     :variable/native-unit-uuid
                eng-uuid     :variable/english-unit-uuid
                met-uuid     :variable/metric-unit-uuid
                domain-uuid  :variable/domain-uuid}]
            (when (= kind :continuous)
              (let [domain         (when domain-uuid (d/entity @@s/vms-conn [:bp/uuid domain-uuid]))
                    native-uuid    (or (:domain/native-unit-uuid domain) nat-uuid)
                    english-uuid   (or (:domain/english-unit-uuid domain) eng-uuid)
                    metric-uuid    (or (:domain/metric-unit-uuid domain) met-uuid)
                    input          (get all-inputs sv-uuid)
                    value          (:input/value input)
                    per-input-uuid (:input/units-uuid input)
                    effective-uuid (or per-input-uuid
                                       (case units-system
                                         :english english-uuid
                                         :metric  metric-uuid
                                         native-uuid))]
                (outside-range? native-uuid effective-uuid var-min var-max value))))
          input-variables))))
