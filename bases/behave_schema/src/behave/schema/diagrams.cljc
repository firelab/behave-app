(ns behave.schema.diagrams)

(def schema
  "Datomic schema for diagram entities."
  [{:db/ident       :diagram/type
    :db/doc         "Keyword #{:contain :fire-shape :wind-slope-spread-direction :optimized-contain}"
    :db/valueType   :db.type/keyword
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/group-variable
    :db/doc         "Diagram's reference to the output group variable."
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/output-group-variables
    :db/doc         "Diagram output group-variables to show in summary table"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :diagram/units-uuid
    :db/doc         "DEPRECATED — use :diagram/x-units-uuid and :diagram/y-units-uuid instead"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/input-group-variables
    :db/doc         "Diagram input group-variables to show in summary table"
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident       :diagram/title
    :db/doc         "Diagram display title (e.g. 'Containment'). Per-run params are appended at render time."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/title-translation-key
    :db/doc         "Translation key for diagram title; resolved via <t in the app."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/x-axis-title
    :db/doc         "Diagram x-axis label"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/x-axis-title-translation-key
    :db/doc         "Translation key for x-axis label; resolved via <t in the app."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/y-axis-title
    :db/doc         "Diagram y-axis label"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/y-axis-title-translation-key
    :db/doc         "Translation key for y-axis label; resolved via <t in the app."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/y-axis-positive-label
    :db/doc         "Label drawn at the positive (top) end of the y-axis (e.g. 'Upslope')."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/y-axis-positive-label-translation-key
    :db/doc         "Translation key for the positive y-axis end label; resolved via <t in the app."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/y-axis-negative-label
    :db/doc         "Label drawn at the negative (bottom) end of the y-axis (e.g. 'Downslope')."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/y-axis-negative-label-translation-key
    :db/doc         "Translation key for the negative y-axis end label; resolved via <t in the app."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/hide-axis-numbers?
    :db/doc         "When true, hides the numeric tick labels on both axes (axis titles are kept)."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/x-units-uuid
    :db/doc         "Diagram x-axis units UUID"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/y-units-uuid
    :db/doc         "Diagram y-axis units UUID"
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/show-quadrant-1?
    :db/doc         "Show upper-right quadrant (x >= 0, y >= 0). Defaults to true."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/show-quadrant-2?
    :db/doc         "Show upper-left quadrant (x <= 0, y >= 0). Defaults to true."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/show-quadrant-3?
    :db/doc         "Show lower-left quadrant (x <= 0, y <= 0). Defaults to true."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/show-quadrant-4?
    :db/doc         "Show lower-right quadrant (x >= 0, y <= 0). Defaults to true."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :diagram/connect-points?
    :db/doc         "When true, scatter-plot points are connected as a line. Defaults to false."
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}])
