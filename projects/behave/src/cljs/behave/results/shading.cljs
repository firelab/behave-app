(ns behave.results.shading
  "Pure helpers for deciding which result cells fall outside an enabled
  table-filter range (the \"shaded\" cells).

  No subscriptions or reactive state lives here: callers supply already-fetched
  matrix data so the range math can be unit-tested in isolation and shared by
  every view that shades results (matrices, flat table, CSV export)."
  (:require [goog.string :as gstring]))

(defn- round-to
  "Round `n` to `sig` decimal places, matching the result-table formatter.
  Returns `n` unchanged when `sig` or `n` is nil."
  [sig n]
  (if (and sig (some? n))
    (js/parseFloat (gstring/format (str "%." sig "f") n))
    n))

(defn out-of-range?
  "True when `value` for `output-gv-uuid` falls outside its enabled filter range.

  - `filters-by-uuid` : map of output gv-uuid -> `[gv-uuid min max enabled?]`
  - `sig-digits`      : map of output gv-uuid -> significant decimal places
  - `value`           : the cell value (number or numeric string)"
  [filters-by-uuid sig-digits output-gv-uuid value]
  (let [[_ mmin mmax enabled?] (get filters-by-uuid output-gv-uuid)
        rnd                    (partial round-to (get sig-digits output-gv-uuid))
        rounded                (rnd (js/parseFloat value))]
    (boolean (and enabled? mmin mmax
                  (not (<= (rnd mmin) rounded (rnd mmax)))))))

(defn shade-set
  "Return the set of positions whose value is out of range.

  `cells` is a seq of `[position output-gv-uuid value]` tuples. `position` is
  opaque to this fn — a row key when there is one multi-valued input, or a
  `[row col]` pair when there are two — so the same reducer serves both layouts."
  [filters-by-uuid sig-digits cells]
  (reduce (fn [acc [position output-gv-uuid value]]
            (cond-> acc
              (out-of-range? filters-by-uuid sig-digits output-gv-uuid value)
              (conj position)))
          #{}
          cells))

(defn filters-by-uuid
  "Index a `:worksheet/table-settings-filters` seq (`[gv-uuid min max enabled?]`
  tuples) by group-variable uuid for O(1) lookup in [[out-of-range?]]."
  [table-setting-filters]
  (into {} (map (fn [[gv-uuid :as filter-tuple]] [gv-uuid filter-tuple]))
        table-setting-filters))

(defn applicable-filters
  "Keep only the filter tuples whose output can actually shade.

  - `visible?` : set of output gv-uuids currently shown on Results
  - `hidden?`  : fn of gv-uuid, true when it opts out of table filters
  - `children` : fn of gv-uuid -> its direction-children gv-uuids

  A directional parent is never a visible output itself (it carries
  `hide-result?`); its direction children are. The parent is what gets the
  filter row, so it counts as shown whenever any child is."
  [{:keys [visible? hidden? children]} table-setting-filters]
  (let [shown? (fn [gv-uuid]
                 (or (visible? gv-uuid)
                     (some visible? (children gv-uuid))))]
    (remove (fn [[gv-uuid]]
              (or (hidden? gv-uuid)
                  (not (shown? gv-uuid))))
            table-setting-filters)))
