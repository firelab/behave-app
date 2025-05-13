(ns behave.wizard.subs
  (:require [behave.schema.core     :refer [rules]]
            [behave.lib.units       :refer [convert]]
            [behave.vms.store       :refer [vms-conn]]
            [behave.translate       :refer [<t]]
            [clojure.set            :refer [rename-keys intersection]]
            [datascript.core        :as d]
            [re-frame.core          :refer [reg-sub subscribe] :as rf]
            [number-utils.interface :refer [is-numeric? parse-float]]
            [string-utils.interface :refer [->kebab]]
            [clojure.string         :as str]
            [bidi.bidi              :refer [path-for]]
            [behave-routing.main    :refer [routes]]
            [goog.string            :as gstring]))

;;; Helpers

(defn- matching-submodule? [io slug submodule]
  (and (= io (:submodule/io submodule))
       (= slug (:slug submodule))))

(defn- in-range?
  "Identifies if a value `v` is within `v-min` and `v-max`."
  [v-min v-max v]
  (and (not (neg? v))
       (cond
         (and (some? v-max) (some? v-min))
         (<= v-min v v-max)

         (some? v-min)
         (<= v-min v)

         (some? v-max)
         (<= 0 v v-max)

         :else true)))

(defn- values-in-range?
  [var-min var-max v]
  {:pre [(or (nil? v) (string? v))]}
  (if (empty? v)
    true
    (let [values (->> (str/split (str v) #"[, ]") (remove empty?))]
      (and (every? is-numeric? values)
           (every? (partial in-range? var-min var-max)
                   (map parse-float values))))))

(defn- outside-range-error-msg
  [v-min v-max]
  (let [msg (cond
              (and v-min v-max)
              ["Error: Value(s) are not within range (min: %2f, max: %2f)" v-min v-max]

              v-min
              ["Error: Value(s) are not within range (min: %2f)" v-min]

              v-max
              ["Error: Value(s) are not within range (min: %2f, min: %2f)" 0 v-max]

              :else
              ["Error: Value(s) are not positive." 0 v-max])]
    (apply gstring/format msg)))

;;; Subscriptions

(reg-sub
 :wizard/*module
 (fn [_]
   (subscribe [:vms/pull-with-attr :module/name]))
 (fn [modules [_ selected-module]]
   (->> modules
        (filter (fn [{m-name :module/name}]
                  (= selected-module (str/lower-case m-name))))
        (first)
        (:db/id)
        (d/entity @@vms-conn)
        (d/touch))))

(reg-sub
 :wizard/submodules
 (fn [[_ module-id]]
   (subscribe [:vms/pull-children
               :module/submodules
               module-id]))
 (fn [submodules _]
   (->> submodules
        (map (fn [submodule]
               (-> submodule
                   (assoc :slug (-> submodule (:submodule/name) (->kebab)))
                   (assoc :submodule/groups @(subscribe [:wizard/groups (:db/id submodule)])))))
        (sort-by :submodule/order))))

(reg-sub
 :wizard/submodules-io-input-only
 (fn [[_ module-id]]
   (subscribe [:wizard/submodules module-id]))

 (fn [submodules _]
   (->> submodules
        (filter (fn [submodule] (= (:submodule/io submodule) :input)))
        (filter #(not (:submodule/research? %))) ;; TODO: Remove when "Research Mode" is enabled
        (sort-by :submodule/order))))

(reg-sub
 :wizard/submodules-io-output-only
 (fn [[_ module-id]]
   (subscribe [:wizard/submodules module-id]))

 (fn [submodules _]
   (->> submodules
        (filter (fn [submodule] (= (:submodule/io submodule) :output)))
        (filter #(not (:submodule/research? %))) ;; TODO: Remove when "Research Mode" is enabled
        (sort-by :submodule/order))))

(reg-sub
 :wizard/submodules-conditionally-filtered
 (fn [[_ _ws-uuid module-id _io]]
   [(subscribe [:wizard/submodules-io-input-only module-id])
    (subscribe [:wizard/submodules-io-output-only module-id])])

 (fn [[input-submodules output-submodules] [_ ws-uuid _module-id io]]
   (let [submodules (if (= io :output) output-submodules input-submodules)]
     (filter (fn [{id :db/id
                   op :submodule/conditionals-operator}]
               @(subscribe [:wizard/show-submodule? ws-uuid id op]))
             submodules))))

(reg-sub
 :wizard/*submodule

 (fn [[_ module-id _ _]]
   (subscribe [:wizard/submodules module-id]))

 (fn [submodules [_ _ slug io]]
   (let [[inputs outputs] (partition-by :submodules/io submodules)]
     (or (first (filter (partial matching-submodule? io slug) submodules))
         (first (if (= :input io) inputs outputs))))))

(defn edit-groups [group]
  (when group
    (cond-> group
      (seq (:group/group-variables group))
      (assoc :group/group-variables
             (mapv #(let [variable-data (rename-keys (first (:variable/_group-variables %))
                                                     {:bp/uuid :variable/uuid})]
                      (-> %
                          (dissoc :variable/_group-variables)
                          (merge variable-data)
                          (dissoc :variable/group-variables)
                          (update :variable/kind keyword)))
                   (remove #(or (:group-variable/research? %) ;; TODO: Remove when "Research Mode" is enabled
                                (:group-variable/conditionally-set? %))
                           (:group/group-variables group))))

      (seq (:group/children group))
      (assoc :group/children
             (map edit-groups (:group/children group))))))

(reg-sub
 :wizard/groups
 (fn [[_ submodule-id]]
   (subscribe [:vms/pull-children
               :submodule/groups
               submodule-id
               '[* {:group/group-variables
                    [* {:variable/_group-variables
                        [* {:variable/list
                            [* {:list/tag-set [*]
                                :list/color-tag-set [*]
                                :list/options
                                [* {:list-option/tag-refs      [*]
                                    :list-option/color-tag-ref [*]}]}]}]}]}
                 {:group/children 6}]])) ;; recursively apply pattern up to 6 levels deep

 (fn [groups]
   (->> (mapv edit-groups groups)
        (sort-by #(:group/order %)))))

(defn- edit-groups-for-result-table [group]
  (when group
    (cond-> group
      (seq (:group/group-variables group))
      (assoc :group/group-variables
             (mapv #(let [variable-data (rename-keys (first (:variable/_group-variables %))
                                                     {:bp/uuid :variable/uuid})]
                      (-> %
                          (dissoc :variable/_group-variables)
                          (merge variable-data)
                          (dissoc :variable/group-variables)
                          (update :variable/kind keyword)))
                   (remove #(:group-variable/research? %)
                           (:group/group-variables group))))

      (seq (:group/children group))
      (assoc :group/children
             (map edit-groups (:group/children group))))))

;; Subgroups

(reg-sub
 :wizard/subgroups
 (fn [[_ group-id]]
   (subscribe [:vms/pull
               '[{:group/children
                  [* {:group/group-variables
                      [* {:variable/_group-variables
                          [* {:variable/list
                              [* {:list/options [*]}]}]}]
                      :group/children [*]}]}]
               group-id]))

 (fn [group]
   (mapv (fn [subgroup]
           (assoc subgroup
                  :group/group-variables
                  (mapv #(let [variable-data (rename-keys (first (:variable/_group-variables %))
                                                          {:bp/uuid :variable/uuid})]
                           (-> %
                               (dissoc :variable/_group-variables)
                               (merge variable-data)
                               (dissoc :variable/group-variables)
                               (update :variable/kind keyword))) (:group/group-variables subgroup))))
         (:group/children group))))


;; Lists

(reg-sub
 :wizard/variable-list
 (fn [[_ group-id]]
   (subscribe [:vms/pull
               '[{:group/children [* {:group/group-variables [* {:variable/_group-variables [*]}] :group/children [*]}]}]
               group-id])))

;; Group Variables

(reg-sub
 :wizard/multi-value-input-count

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet/all-input-values ws-uuid]))

 (fn [all-input-values _query]
   (count
    (filter (fn multiple-values? [value]
              (> (count (str/split value #",|\s"))
                 1))
            all-input-values))))

;; Converts group variable uuid to the translated variable name using the first translation-key
(reg-sub
 :wizard/gv-uuid->default-variable-name
 (fn [_ [_ gv-uuid]]
   (when-let [translation-key (->> (d/entity @@vms-conn [:bp/uuid gv-uuid])
                                   :group-variable/translation-key)]
     @(<t translation-key))))

;; Converts group variable uuid to the translated variable name using the second translation-key
(reg-sub
 :wizard/gv-uuid->result-variable-name
 (fn [_ [_ gv-uuid]]
   (when-let [translation-key (->> (d/entity @@vms-conn [:bp/uuid gv-uuid])
                                   :group-variable/result-translation-key)]
     @(<t translation-key))))

(reg-sub
 :wizard/gv-uuid->resolve-result-variable-name

 (fn [[_ gv-uuid]]
   [(subscribe [:wizard/gv-uuid->result-variable-name gv-uuid])
    (subscribe [:wizard/gv-uuid->default-variable-name gv-uuid])])

 (fn [[result-variable-name default-variable-name] _]
   (or result-variable-name default-variable-name)))

(reg-sub
 :wizard/gv-uuid->variable-units
 (fn [_ [_ gv-uuid]]
   @(subscribe [:vms/query '[:find ?unit-short-code .
                             :in    $ ?gv-uuid
                             :where
                             [?gv :db/id [:bp/uuid ?gv-uuid]]
                             [?v :variable/group-variables ?gv]
                             [?v :variable/native-unit-uuid ?unit-uuid]
                             [?u :db/id [:bp/uuid ?unit-uuid]]
                             [?u :unit/short-code ?unit-short-code]]
                gv-uuid])))

;; Returns a map of group-variable-uuids -> variable native units
;; if and only if the variable is allowed to convert to map-units
(reg-sub
 :wizard/map-unit-convertible-variables
 (fn [_]
   (subscribe [:vms/query '[:find [?gv-uuid ...]
                            :where
                            [?v :variable/group-variables ?gv]
                            [?gv :bp/uuid ?gv-uuid]
                            [?v :variable/map-units-convertible? true]]]))
 (fn [results _]
   (set results)))

(reg-sub
 :wizard/group-variable
 (fn [[_ gv-uuid]]
   (subscribe [:vms/pull '[* {:variable/_group-variables [:variable/name :variable/native-unit-uuid]}] [:bp/uuid gv-uuid]]))

 (fn [group-variable _query]
   (let [variable-data (rename-keys (first (:variable/_group-variables group-variable))
                                    {:db/id :variable/id})]
     (-> group-variable
         (dissoc :variable/_group-variables)
         (merge variable-data)
         (dissoc :variable/group-variables)
         (update :variable/kind keyword)))))

;; TODO Might want to set this in a config file to the application
(def ^:const multi-value-input-limit 3)

(reg-sub
 :wizard/multi-value-input-limit
 (fn [_db _query]
   multi-value-input-limit))

;;; Outside Range

(reg-sub
 :wizard/outside-range?
 (fn [[_ native-unit-uuid unit-uuid _ _ _]]
   [(rf/subscribe [:vms/units-uuid->short-code native-unit-uuid])
    (rf/subscribe [:vms/units-uuid->short-code unit-uuid])])
 (fn [[from-units to-units] [_ _ _ var-min var-max value]]
   (not
    (if (or (nil? to-units) (= from-units to-units))
        (values-in-range? var-min var-max value)
        (values-in-range? (convert var-min from-units to-units 2)
                          (convert var-max from-units to-units 2)
                          value)))))

(reg-sub
 :wizard/outside-range-error-msg
 (fn [[_ native-unit-uuid unit-uuid _ _ _]]
   [(rf/subscribe [:vms/units-uuid->short-code native-unit-uuid])
    (rf/subscribe [:vms/units-uuid->short-code unit-uuid])])

 (fn [[from-units to-units] [_ _ _ var-min var-max]]
   (if (or (nil? to-units) (= from-units to-units))
     (outside-range-error-msg var-min var-max)
     (outside-range-error-msg (convert var-min from-units to-units 2)
                              (convert var-max from-units to-units 2)))))

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

(reg-sub
 :wizard/input-min-max-placeholder
 (fn [_ [_ var-min var-max native-unit-short-code new-unit-short-code]]
   (let [var-min-str (str var-min)
         var-max-str (str var-max)]
     (cond
       (and var-min var-max (= native-unit-short-code new-unit-short-code))
       (str var-min " - " var-max)

       (and var-min var-max)
       (str (convert-values native-unit-short-code new-unit-short-code var-min-str)
            " - "
            (convert-values native-unit-short-code new-unit-short-code var-max-str))

       var-min
       (str var-min " - +INF")

       var-max
       (str "-INF - " var-max)

       :else
       nil))))

(reg-sub
 :wizard/warn-limit?
 (fn [[_id ws-uuid]]
   (subscribe [:wizard/multi-value-input-count ws-uuid]))

 (fn [multi-value-input-count _query]
   (> multi-value-input-count multi-value-input-limit)))

(reg-sub
 :wizard/submodule-name+io
 (fn [_ [_ submodule-uuid]]
   @(subscribe [:vms/query '[:find [?s-name ?io]
                             :in    $ ?uuid
                             :where
                             [?s :db/id [:bp/uuid ?uuid]]
                             [?s :submodule/name ?s-name]
                             [?s :submodule/io ?io]]
                submodule-uuid])))

;; returns a collection of [note-id note-name note-content submodule-name submodule-io]
;; Optionally filter notes using submodule-uuid
(reg-sub
 :wizard/notes

 (fn [[_id ws-uuid]]
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet [_ _ws-uuid submodule-uuid]]
   (let [notes (:worksheet/notes worksheet)]
     (cond->> notes
       submodule-uuid (filter (fn [{s-uuid :note/submodule}]
                                (= s-uuid submodule-uuid)))
       :always        (map (fn resolve-uuid [{id      :db/id
                                              name    :note/name
                                              content :note/content
                                              s-uuid  :note/submodule}]
                             (into   [id name content]
                                     @(subscribe [:wizard/submodule-name+io s-uuid]))))))))

(reg-sub
 :wizard/edit-note?
 (fn [{:keys [state]} [_ note-id]]
   (true? (get-in state [:worksheet :notes note-id :edit?]))))


(reg-sub
 :wizard/show-notes?
 (fn [{:keys [state]} _]
   (true? (get-in state [:worksheet :show-notes?]))))

(reg-sub
 :wizard/show-add-note-form?
 (fn [{:keys [state]} _]
   (true? (get-in state [:worksheet :show-add-note-form?]))))


(reg-sub
 :wizard/results-tab-selected
 (fn [_ _]
   (subscribe [:state [:worksheet :results :tab-selected]]))
 (fn [tab-selected _]
   tab-selected))

(reg-sub
 :wizard/worksheet-date

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet ws-uuid]))

 (fn [worksheet _]
   (let [created-date (:worksheet/created worksheet)
         d            (js/Date.)]
     (.setTime d created-date)
     (.toLocaleDateString d))))

(reg-sub
 :wizard/first-module+submodule
 (fn [[_ ws-uuid _]]
   (subscribe [:wizard/route-order ws-uuid]))

 (fn [route-order [_ _ws-uuid io]]
   (when io
     (when-let [first-path (first (filter
                                   (fn [path] (str/includes? path (name io)))
                                   route-order))]
       (let [module-regex    (gstring/format "(?<=modules/).*(?=/%s)" (name io))
             submodule-regex (gstring/format "(?<=%s/).*" (name io))]
         [(re-find (re-pattern module-regex) first-path)
          (re-find (re-pattern submodule-regex) first-path)])))))

;;; show-group?
(defn- csv? [s] (< 1 (count (str/split s #","))))

(defn- intersect? [s1 s2]
  (pos? (count (intersection s1 s2))))

(defn- resolve-conditionals [worksheet conditionals]
  (let [ws-uuid (:worksheet/uuid worksheet)]
    (map (fn pass?
           [{group-variable-uuid :conditional/group-variable-uuid
             ttype               :conditional/type
             op                  :conditional/operator
             values              :conditional/values
             sub-conditionals    :conditional/sub-conditionals
             sub-conditional-op  :conditional/sub-conditional-operator}]
           (let [{:keys [group-uuid io]} @(subscribe [:wizard/conditional-io+group-uuid
                                                      group-variable-uuid])
                 conditional-values-set  (set values)
                 worksheet-value         (cond
                                           (= ttype :module)
                                           (map name (:worksheet/modules worksheet))

                                           (= io :output)
                                           @(subscribe [:worksheet/output-enabled?
                                                        ws-uuid
                                                        group-variable-uuid])

                                           (= io :input)
                                           @(subscribe [:worksheet/input-value
                                                        ws-uuid
                                                        group-uuid
                                                        0
                                                        group-variable-uuid]))
                 worksheet-value-set       (cond
                                             (= ttype :module)      (set worksheet-value)
                                             (csv? worksheet-value) (set (map str/trim (str/split worksheet-value ",")))
                                             :else                  #{worksheet-value})
                 sub-resolved-conditionals (when sub-conditionals
                                             (if (= sub-conditional-op :or)
                                               (some true? (resolve-conditionals worksheet sub-conditionals))
                                               (every? true? (resolve-conditionals worksheet sub-conditionals))))
                 this-conditional          (case op
                                             :equal     (if (= ttype :module)
                                                          (= conditional-values-set worksheet-value-set)
                                                          (= (first conditional-values-set)
                                                             (if worksheet-value (str worksheet-value) "false")))
                                             :not-equal (not= (first conditional-values-set) (str worksheet-value))
                                             :in        (intersect? conditional-values-set worksheet-value-set))]
             (if sub-conditionals
               (and this-conditional sub-resolved-conditionals)
               this-conditional)))
         conditionals)))

(defn all-conditionals-pass? [worksheet conditionals-operator conditionals]
  (if (seq conditionals)
    (let [resolved-conditionals (resolve-conditionals worksheet conditionals)]
      (if (= conditionals-operator :or)
        (some true? resolved-conditionals)
        (every? true? resolved-conditionals)))
    true))

(defn- find-parent-submodule
  [group]
  (let [submodule (:submodule/_groups group)]
    (cond
      submodule submodule
      group     (find-parent-submodule (:group/_children group))
      :else     nil)))

(reg-sub
 :wizard/conditional-io+group-uuid
 (fn [_ [_ gv-uuid]]
   (let [group (-> (d/entity @@vms-conn [:bp/uuid gv-uuid])
                   (:group/_group-variables))
         io    (-> group
                   (find-parent-submodule)
                   (:submodule/io))]
     {:io         io
      :group-uuid (:bp/uuid group)})))

(reg-sub
 :wizard/_select-actions
 (fn [_ [_ gv-uuid]]
   (->> (d/entity @@vms-conn [:bp/uuid gv-uuid])
        (:group-variable/actions)
        (filter #(= (:action/type %) :select))
        (map d/touch))))

(reg-sub
 :wizard/default-option
 (fn [[_ ws-uuid gv-uuid]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:wizard/_select-actions gv-uuid])])
 (fn [[worksheet actions]]
   (first
    (for [action actions
          :let   [conditionals         (:action/conditionals action)
                  cond-operator        (:action/conditionals-operator action)
                  target-value         (:action/target-value action)
                  conditionals-passed? (or (nil? conditionals)
                                           (all-conditionals-pass?
                                            worksheet cond-operator conditionals))]
          :when  (and target-value conditionals-passed?)]
      (:action/target-value action)))))

(reg-sub
 :wizard/_disabled-actions
 (fn [_ [_ gv-uuid]]
   (->> (d/entity @@vms-conn [:bp/uuid gv-uuid])
        (:group-variable/actions)
        (filter #(= (:action/type %) :disable))
        (map d/touch))))

(reg-sub
 :wizard/disabled-options
 (fn [[_ ws-uuid gv-uuid]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:wizard/_disabled-actions gv-uuid])])
 (fn [[worksheet actions]]
   (->> actions
        (map #(let [conditionals  (:action/conditionals %)
                    cond-operator (:action/conditionals-operator %)
                    target-value  (:action/target-value %)

                    conditionals-passed?
                    (or (nil? conditionals)
                        (all-conditionals-pass? worksheet cond-operator conditionals))]
                (when (and target-value conditionals-passed?)
                  (:action/target-value %))))
        (remove nil?)
        (set))))

(reg-sub
 :wizard/disabled-output-group-variable?
 (fn [[_ ws-uuid gv-uuid]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:wizard/_disabled-actions gv-uuid])])
 (fn [[worksheet actions]]
   (->> actions
        (some #(let [conditionals  (:action/conditionals %)
                     cond-operator (:action/conditionals-operator %)]
                 (or (nil? conditionals)
                     (all-conditionals-pass? worksheet cond-operator conditionals)))))))

(reg-sub
 :wizard/show-group?
 (fn [[_ ws-uuid group-id & _rest]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:vms/pull-children :group/conditionals group-id])
    (subscribe [:vms/entity-from-eid group-id])])

 (fn [[worksheet conditionals group-entity] [_ _ws-uuid _group-id conditionals-operator]]
   (and (all-conditionals-pass? worksheet conditionals-operator conditionals)
        (not (:group/research? group-entity))
        (or (some #(not (:group-variable/conditionally-set? %)) (:group/group-variables group-entity))
            (seq (:group/children group-entity))))))

(reg-sub
 :wizard/show-submodule?
 (fn [[_ ws-uuid submodule-id & _rest]]
   [(subscribe [:worksheet ws-uuid])
    (rf/subscribe [:vms/pull-children :submodule/conditionals submodule-id])])

 (fn [[worksheet conditionals] [_ _ws-uuid _submodule-id conditionals-operator]]
   (all-conditionals-pass? worksheet conditionals-operator conditionals)))

(reg-sub
 :wizard/diagram-input-gv-uuids
 (fn [_ [_ gv-uuid]]
   (d/q '[:find  [?gv-uuid ...]
          :in    $ ?gv
          :where
          [?d :diagram/group-variable ?gv]
          [?d :diagram/input-group-variables ?g]
          [?g  :bp/uuid ?gv-uuid]]
        @@vms-conn [:bp/uuid gv-uuid])))

(reg-sub
 :wizard/diagram-output-gv-uuids
 (fn [_ [_ gv-uuid]]
   (d/q '[:find  [?gv-uuid ...]
          :in    $ ?gv
          :where
          [?d :diagram/group-variable ?gv]
          [?d :diagram/output-group-variables ?g]
          [?g :bp/uuid ?gv-uuid]]
        @@vms-conn [:bp/uuid gv-uuid])))

(reg-sub
 :wizard/show-range-selector?
 (fn [{:keys [state]} [_ gv-uuid repeat-id]]
   (true? (get-in state [:show-range-selector? gv-uuid repeat-id]))))


(defn index-by
  "Indexes collection by key or fn."
  [k-or-fn coll]
  (persistent! (reduce
                (fn [acc cur] (assoc! acc (k-or-fn cur) cur))
                (transient {})
                coll)))

(reg-sub
 :wizard/units-used-short-code

 (fn [[_ _ _ dimension-uuid]]
   (rf/subscribe [:vms/entity-from-uuid dimension-uuid]))

 (fn [{units :dimension/units} [_ v-uuid ws-unit-uuid _dimension-uuid native-unit-uuid english-unit-uuid metric-unit-uuid]]
   (let [units-by-uuid     (index-by :bp/uuid units)
         *cached-unit-uuid (rf/subscribe [:settings/cached-unit v-uuid])
         *cached-unit      (rf/subscribe [:vms/entity-from-uuid @*cached-unit-uuid])
         native-unit       (get units-by-uuid native-unit-uuid)
         english-unit      (get units-by-uuid english-unit-uuid)
         metric-unit       (get units-by-uuid metric-unit-uuid)
         default-unit      (or @*cached-unit native-unit english-unit metric-unit)]
     (:unit/short-code (or (get units-by-uuid ws-unit-uuid) default-unit)))))

(reg-sub
 :wizard/x-axis-limit-min+max-defaults

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet/multi-value-input-uuid+value ws-uuid]))

 (fn [multi-value-inputs [_ _ gv-uuid]]
   (let [[_ values]    (first (filter #(= (first %) gv-uuid) multi-value-inputs))
         parsed-values (map js/parseFloat (str/split values ","))]
     [0 (apply max parsed-values)])))

(reg-sub
 :wizard/conditionally-set-group-variables

 (fn [[_ ws-uuid]]
   (subscribe [:worksheet/modules ws-uuid]))

 (fn [modules [_ _ io]]
   (letfn [(get-conditionally-set-group-variables [module-eid]
             (d/q '[:find [?gv ...]
                    :in $ % ?module-eid ?io
                    :where
                    [?module-eid :module/submodules ?s]
                    [?s :submodule/io ?io]
                    (group ?s ?g)
                    [?g :group/group-variables ?gv]
                    [?gv :group-variable/conditionally-set? true]]
                  @@vms-conn
                  rules
                  module-eid
                  io))]

     (->> (mapcat #(get-conditionally-set-group-variables (:db/id %)) modules)
          (map #(d/touch (d/entity @@vms-conn %)))))))

(reg-sub
 :wizard/conditionally-set-input-data

 (fn [[_ ws-uuid]]
   (subscribe [:wizard/conditionally-set-group-variables ws-uuid :input]))

 (fn [group-variables [_ ws-uuid]]
   (mapv (juxt
          (fn [group-variable]
            (d/q '[:find ?g-uuid .
                   :in $ % ?gv
                   :where
                   (group-variable ?g ?gv ?v)
                   [?g :bp/uuid ?g-uuid]]
                 @@vms-conn
                 rules
                 (:db/id group-variable)))
          :bp/uuid
          #(deref (rf/subscribe [:wizard/default-option ws-uuid (:bp/uuid %)])))
         group-variables)))

(reg-sub
 :wizard/selected-group-variables
 (fn [[_ _ group-eid]]
   (subscribe [:vms/entity-from-eid  group-eid]))

 (fn [group [_ ws-uuid]]
   (let [x-form (comp (map :bp/uuid)
                      (filter #(true? (deref (rf/subscribe [:worksheet/output-enabled? ws-uuid %])))))]
     (into #{} x-form (:group/group-variables (d/touch group))))))

(reg-sub
 :wizard/submodule-parent
 (fn [_ [_ submodule-id]]
   (d/q '[:find  ?module-name .
          :in    $ % ?submodule-id
          :where
          (submodule ?m ?submodule-id)
          [?m :module/name ?module-name]]
        @@vms-conn rules submodule-id)))

(defn- parent-module-name
  [submodule-id]
  (d/q '[:find  ?module-name .
         :in    $ % ?submodule-id
         :where
         (submodule ?m ?submodule-id)
         [?m :module/name ?module-name]]
       @@vms-conn rules submodule-id))

(defn- build-path
  [rroutes ws-uuid submodule]
  (path-for rroutes
            :ws/wizard
            :ws-uuid ws-uuid
            :module (str/lower-case (parent-module-name (:db/id submodule)))
            :io (:submodule/io submodule)
            :submodule (-> submodule (:submodule/name) (->kebab))))

(defn- all-shown-submodules [worksheet modules]
  (->> modules
       (mapcat (fn [module]
                 (->> module
                      :module/submodules
                      (sort-by :submodule/order))))
       (filter (fn [{op           :submodule/conditionals-operator
                     conditionals :submodule/conditionals
                     research?    :submodule/research?}]
                 (and (not research?)
                      (all-conditionals-pass? worksheet op conditionals))))))

(reg-sub
 :wizard/route-order

 (fn [[_ ws-uuid]]
   [(subscribe [:worksheet ws-uuid])
    (subscribe [:worksheet/modules ws-uuid])])

 (fn [[worksheet modules] [_ ws-uuid]]
   (let [submodules        (all-shown-submodules worksheet modules)
         output-submodules (filter (fn [{io :submodule/io}] (= io :output)) submodules)
         input-submodules  (filter (fn [{io :submodule/io}] (= io :input)) submodules)]
     (into []
           (concat
            (map (partial build-path routes ws-uuid) output-submodules)
            (map (partial build-path routes ws-uuid) input-submodules)
            [(path-for routes :ws/review :ws-uuid ws-uuid)
             (path-for routes :ws/results-settings :ws-uuid ws-uuid :results-page :settings)
             (path-for routes :ws/results :ws-uuid ws-uuid)])))))

(reg-sub
 :wizard/working-area-expanded?
 (fn [] [(subscribe [:state [:sidebar :hidden?]])
         (subscribe [:state [:help-area :hidden?]])])

 (fn [[sidebar-hidden? help-area-hidden?]]
   (and sidebar-hidden? help-area-hidden?)))

(reg-sub
 :wizard/output-directional-tables?
 (fn [[_ ws-uuid]] (subscribe [:worksheet/output-directions ws-uuid]))
 (fn [output-directions]
   (> (count output-directions) 1)))

(defn- group-variable-discrete?
  [gv-uuid]
  (= (d/q '[:find  ?kind .
            :in    $ % ?gv-uuid
            :where
            (variable-kind ?gv-uuid ?kind)]
          @@vms-conn rules gv-uuid)
     :discrete))

(reg-sub
 :wizard/discrete-group-variable?
 (fn [_ [_ gv-uuid]]
   (group-variable-discrete? gv-uuid)))

(reg-sub
 :wizard/show-graph-settings?
 (fn [[_ ws-uuid]] (subscribe [:wizard/multi-value-input-count ws-uuid]))
 (fn [count _]
   (pos? count)))

(reg-sub
 :wizard/enable-graph-settings?
 (fn [[_ ws-uuid]]
   [(subscribe [:worksheet/get-graph-settings-attr
                ws-uuid
                :graph-settings/enabled?])
    (subscribe [:worksheet/multi-value-input-uuids ws-uuid])])
 (fn [[saved-enabled?-setting multi-valued-inputs] _]
   (if (not (nil? (first saved-enabled?-setting)))
     (first saved-enabled?-setting)
     (pos? (count multi-valued-inputs)))))
