(ns steps.outputs
  (:require [steps.helpers :as h]))

;;; =============================================================================
;;; Private Helper Functions
;;; =============================================================================

(defn- select-single-output
  [driver [submodule & groups]]
  (h/select-submodule-tab driver submodule)
  (h/wait-for-groups driver (butlast groups))
  (h/wait-for-element-by-selector driver {:text (last groups)} 10000)
  (h/select-output driver (last groups)))

;;; =============================================================================
;;; Public API
;;; =============================================================================

(defn select-output
  "Expects the bindings {submodule} : {group} : {subgroup} : {value}
  subgroup is optional

  The driver will follow the path to select output:
  submodule -> group -> subgroup -> value"
  [{:keys [driver]} & path]
  (h/wait-for-wizard driver)
  (let [[submodule & groups] path]
    (h/select-submodule-tab driver submodule)
    (h/wait-for-groups driver (butlast groups))
    (h/wait-for-element-by-selector driver {:text (last groups)} 10000)
    (h/select-output driver (last groups)))
  {:driver driver})

(defn select-outputs
  "Expects a data table (as described in the gherkin syntax) to be provided and verifies if
  Data table expects the headers:
  - submodule
  - group
  - subgroup (optional)
  - value
  For each row in the data table the driver will follow the path to select output:
  submodule -> group -> subgroup -> value"
  [{:keys [driver] :as context}]
  (h/wait-for-wizard driver)
  (let [step-data (get-in context [:tegere.parser/step :tegere.parser/step-data])
        paths     (h/parse-step-data step-data)]
    (doseq [path paths]
      (select-single-output driver path))
    {:driver driver}))

(defn verify-outputs-in-results
  "Navigates to the Results page and verifies each named output appears in the
  results output table.

  Expects a single-column data table whose header is `output` and whose rows
  are the fully-qualified result names as they render in the table, e.g.

    | output                  |
    | Heading Rate of Spread  |
    | Flanking Rate of Spread |

  Throws ex-info naming the first missing output."
  [{:keys [driver] :as context}]
  (h/wait-for-wizard driver)
  (h/navigate-to-results driver)
  (let [step-data    (get-in context [:tegere.parser/step :tegere.parser/step-data])
        output-names (map (comp first vals) step-data)]
    (doseq [output-name output-names]
      (when-not (h/output-in-results? driver output-name)
        (throw (ex-info (str "Expected output was NOT displayed in results: " output-name)
                        {:output-name output-name}))))
    {:driver driver}))

(defn verify-outputs-not-selected
  "Expects a data table (as described in the gherkin syntax) to be provided and verifies if
  Data table expects the headers:
  - submodule
  - group
  - subgroup (optional)
  - value
  For each row in the data table the driver will follow the path to verify it is NOT
  displayed: submodule -> group -> subgroup -> value"
  [{:keys [driver] :as context}]
  (h/wait-for-wizard driver)
  (let [step-data (get-in context [:tegere.parser/step :tegere.parser/step-data])
        paths     (h/parse-step-data step-data)]
    (doseq [path paths]
      (let [[submodule & groups] path
            output-name          (last path)
            last-group           (h/navigate-to-group driver path)
            is-checked?          (h/output-checked? last-group)]
        (when is-checked?
          (throw (ex-info (str "Output should NOT be selected but was: " output-name)
                          {:output    output-name
                           :submodule submodule
                           :groups    groups})))))
    {:driver driver}))
