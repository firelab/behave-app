(ns behave.components.settings-form.views
  (:require [behave.components.core :as c]
            [dom-utils.interface    :refer [input-value]]
            [goog.string            :as gstring]
            [goog.string.format]
            [re-frame.core          :refer [dispatch subscribe]]
            [reagent.core           :as r]))

(defn- number-inputs
  [{:keys [saved-entries on-change enabled?]}]
  (map (fn [[gv-uuid saved-value row-enabled?]]
         (let [value-atom (r/atom (str saved-value))
               disabled?  (if (some? enabled?)
                            (not enabled?)
                            (if (some? row-enabled?)
                              (not row-enabled?)
                              false))]
           ;; Hold the raw string while typing so decimals — including an
           ;; in-progress "1." — survive; coerce to a float only on blur, where
           ;; the value is committed. `:step "any"` lets the field accept
           ;; non-integers. (BHP1-1610)
           [c/number-input {:disabled?  disabled?
                            :step       "any"
                            :on-change  #(reset! value-atom (input-value %))
                            :on-blur    #(let [v (js/parseFloat @value-atom)]
                                           (when-not (js/isNaN v)
                                             (on-change gv-uuid v)))
                            :value-atom value-atom}]))
       saved-entries))

(defn settings-form
  [{:keys [ws-uuid title headers rf-event-id rf-sub-id min-attr-id max-attr-id
           default-min-values default-max-values enabled? on-toggle-row]}]
  (let [*gv-uuid+min+max-entries       (subscribe [rf-sub-id ws-uuid])
        *gv-order                      (subscribe [:vms/group-variable-order ws-uuid])
        gv-uuid+min+max-entries-sorted (->> @*gv-uuid+min+max-entries
                                            (sort-by #(.indexOf @*gv-order (first %))))
        units-lookup                   @(subscribe [:worksheet/result-table-units ws-uuid])
        maximums                       (number-inputs {:saved-entries  (map (fn [[gv-uuid _min-val max-val row-enabled?]]
                                                                              [gv-uuid max-val row-enabled?])
                                                                            gv-uuid+min+max-entries-sorted)
                                                       :on-change      #(dispatch [rf-event-id ws-uuid %1 max-attr-id %2])
                                                       :default-values default-max-values
                                                       :enabled?       enabled?})
        minimums                       (number-inputs {:saved-entries  (map (fn [[gv-uuid min-val _max-val row-enabled?]]
                                                                              [gv-uuid min-val row-enabled?])
                                                                            gv-uuid+min+max-entries-sorted)
                                                       :on-change      #(dispatch [rf-event-id ws-uuid %1 min-attr-id %2])
                                                       :default-values default-min-values
                                                       :enabled?       enabled?})
        output-ranges                  (map (fn [[gv-uuid & _rest]]
                                              (let [min-val     (get default-min-values gv-uuid)
                                                    min-val-fmt (if (< min-val 1) "%.1f" "%d")
                                                    max-val     (get default-max-values gv-uuid)
                                                    max-val-fmt (if (< max-val 1) "%.1f" "%d")
                                                    fmt         (gstring/format "%s - %s" min-val-fmt max-val-fmt)]
                                                (gstring/format fmt min-val max-val)))
                                            gv-uuid+min+max-entries-sorted)
        names                          (map (fn [[gv-uuid _min _max]]
                                              (gstring/format "%s (%s)"
                                                              @(subscribe [:wizard/gv-uuid->resolve-result-variable-name gv-uuid])
                                                              (get units-lookup
                                                                   (if-let [directional-children (seq @(subscribe [:vms/directional-children gv-uuid]))]
                                                                     (:bp/uuid (first directional-children))
                                                                     gv-uuid))))
                                            gv-uuid+min+max-entries-sorted)
        checkboxes                     (when on-toggle-row
                                         (map (fn [[gv-uuid _min _max row-enabled?]]
                                                [c/checkbox {:checked?  row-enabled?
                                                             :on-change #(on-toggle-row gv-uuid)}])
                                              gv-uuid+min+max-entries-sorted))
        column-keys                    (mapv (fn [idx] (keyword (str "col" idx))) (range (count headers)))
        row-data                       (apply map
                                              (fn [& args]
                                                (into {}
                                                      (map (fn [x y] [x y])
                                                           column-keys args)))
                                              (if checkboxes
                                                [checkboxes names output-ranges minimums maximums]
                                                [names output-ranges minimums maximums]))]
    [:div.settings-form
     (c/table {:title   title
               :headers headers
               :columns column-keys
               :rows    row-data})]))
