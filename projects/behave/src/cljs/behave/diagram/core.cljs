(ns behave.diagram.core
  "Which series of a results Diagram are currently shown in its legend.

  Transient UI state only -- it is NEVER written to the worksheet database. The
  app-db is the source of truth for the inline page and the pop-out modal, and
  [[behave.diagram.events]] mirrors it into the `behave-settings` local storage so
  the print view -- a separate window with its own app-db -- can hydrate from it.")

(defn diagram-key
  "Per-diagram identity for legend state. The same triple
  [[behave.components.results.diagrams/modal-content]] uses to find a diagram, so
  each diagram on a results page keeps its own set of shown series."
  [ws-uuid row-id group-variable-uuid]
  [ws-uuid row-id group-variable-uuid])

(def db-path
  "Where the per-diagram shown-series sets live in the app-db."
  [:state :diagrams :visible-series])

(def ls-path
  "Where they are mirrored inside the `behave-settings` local storage."
  [:diagrams :visible-series])

(defn resolve-visible
  "The series to show, in legend order.

  `visible` is the app-tracked set of shown series, or nil when this diagram has
  never been touched -- only then does `hidden-by-default` apply. An empty set is
  an explicit \"hide everything\" and is honored as such. Filtering against
  `domain` also drops labels left over in local storage from a renamed series."
  [domain visible hidden-by-default]
  (if (some? visible)
    (filterv visible domain)
    (vec (remove (or hidden-by-default #{}) domain))))

(defn clear-worksheet
  "`m` without the entries belonging to `ws-uuid`. Each key is a [[diagram-key]]
  triple whose first element is the worksheet, so re-running one worksheet drops
  only its own rows and leaves every other worksheet alone.

  Re-running rebuilds the diagrams with row-ids starting at 0 again, so a kept
  entry would re-attach to a different input combination's row."
  [m ws-uuid]
  ;; These keys come back out of local storage, where a malformed one would make
  ;; `first` throw -- a bad entry must not take down a solve.
  (into {} (remove (fn [[k _]] (and (sequential? k) (= ws-uuid (first k))))) m))
