(ns cucumber-test
  (:require [cucumber-test-generator.core :as core]
            [cucumber.runner :refer [run-cucumber-tests]]
            [datomic.api :as d]
            [behave-cms.store :refer [default-conn]]))

(comment

  (require '[cucumber-test-generator.core :as core] :reload)

  (require '[cucumber-test-generator.generate-scenarios :as gs] :reload)

  (core/generate-test-matrix! (d/db (default-conn)))

  (gs/generate-feature-files!)

  (time
   (run-cucumber-tests
    {:debug?    false
     :headless? false
     :features  "features"
     ;; :features  "/home/kcheung/work/code/behave-polylith/features/crown-surface-input_fuel-moisture_dead-live-herb-and-live-woody-categories_dead-fuel-moisture.feature"
     ;; :features "/home/kcheung/work/code/behave-polylith/features/surface-input_fuel-moisture_by-size-class_live-woody-fuel-moisture.feature"
     :steps     "steps"
     :stop      true
     :browser   :chrome
     :url       "http://localhost:8081/worksheets"}))

  (def test-data
    {["Surface"
      "Wind and Slope"
      "Wind Adjustment Factor"
      "Wind Adjustment Factor - User Input"]
     {:path
      ["Surface"
       "Wind and Slope"
       "Wind Adjustment Factor"
       "Wind Adjustment Factor - User Input"],
      :group/research?       nil,
      :parent-submodule/io   :input,
      :group/translated-name "Wind Adjustment Factor - User Input",
      :group/hidden?         nil,
      :group/order           nil,
      :submodule/order       3,
      :conditionals
      {:conditionals
       [{:type     :group-variable,
         :operator :equal,
         :values   ["User Input"],
         :group-variable
         {:group-variable/translated-name
          "Wind Adjustment Factor Calculation Method",
          :group-variable/research? nil,
          :io                       :input,
          :path                     ["Surface" "Wind and Slope" "Wind Adjustment Factor"],
          :submodule/order          3,
          :submodule/research?      nil,
          :group/order              3}}],
       :conditionals-operator nil}}

     ["Surface" "Wind and Slope"]
     {:path                ["Surface" "Wind and Slope"],
      :submodule/name      "Wind and Slope",
      :submodule/io        :input,
      :submodule/research? nil,
      :conditionals
      {:conditionals
       [{:type     :group-variable,
         :operator :equal,
         :values   ["true"],
         :group-variable
         {:group-variable/translated-name "Heading",
          :group-variable/research?       nil,
          :io                             :output,
          :path                           ["Surface" "Fire Behavior" "Direction Mode"],
          :submodule/order                0,
          :submodule/research?            nil,
          :group/order                    0}}
        {:type     :group-variable,
         :operator :equal,
         :values   ["true"],
         :group-variable
         {:group-variable/translated-name "Direction of Interest",
          :group-variable/research?       nil,
          :io                             :output,
          :path                           ["Surface" "Fire Behavior" "Direction Mode"],
          :submodule/order                0,
          :submodule/research?            nil,
          :group/order                    0}}],
       :conditionals-operator :or}}

     ["Surface" "Wind and Slope" "Wind Adjustment Factor"]
     {:path                  ["Surface" "Wind and Slope" "Wind Adjustment Factor"],
      :group/translated-name "Wind Adjustment Factor",
      :group/research?       nil,
      :group/hidden?         nil,
      :parent-submodule/io   :input,
      :group/order           3,
      :submodule/order       3,
      :conditionals
      {:conditionals
       [{:type                     :group-variable,
         :operator                 :in,
         :values                   ["20-Foot" "10-Meter"],
         :group-variable
         {:group-variable/translated-name "Wind Measured at:",
          :group-variable/research?       nil,
          :io                             :input,
          :path                           ["Surface" "Wind and Slope" "Wind Measured at:"],
          :submodule/order                3,
          :submodule/research?            nil,
          :group/order                    nil},
         :sub-conditionals
         [{:type     :group-variable,
           :operator :equal,
           :values   ["true"],
           :group-variable
           {:group-variable/translated-name "Heading",
            :group-variable/research?       nil,
            :io                             :output,
            :path                           ["Surface" "Fire Behavior" "Direction Mode"],
            :submodule/order                0,
            :submodule/research?            nil,
            :group/order                    0}}
          {:type     :group-variable,
           :operator :equal,
           :values   ["true"],
           :group-variable
           {:group-variable/translated-name "Flame Length",
            :group-variable/research?       nil,
            :io                             :output,
            :path                           ["Crown" "Fire Behavior" "Fire Behavior"],
            :submodule/order                0,
            :submodule/research?            nil,
            :group/order                    0}}],
         :sub-conditional-operator :or}],
       :conditionals-operator :and}}

     ["Surface" "Fire Behavior"] {:path                ["Surface" "Fire Behavior"],
                                  :submodule/name      "Fire Behavior",
                                  :submodule/io        :output,
                                  :submodule/research? nil,
                                  :conditionals
                                  {:conditionals
                                   ({:type :module, :operator :equal, :values ["surface"]}
                                    {:type :module, :operator :equal, :values ["contain" "surface"]}
                                    {:type     :module,
                                     :operator :equal,
                                     :values   ["mortality" "surface"]}),
                                   :conditionals-operator :or}}})

  (defn collect-parent-groups
    "Given a path like [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"],
   return all parent group paths in order from module/submodule to leaf.

   Skips module (first element) and submodule (second element) in intermediate paths,
   but includes them in final paths for lookup.

   Example input:  [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"]
   Example output: [[\"Surface\" \"Fuel Moisture\"]
                    [\"Surface\" \"Fuel Moisture\" \"By Size Class\"]
                    [\"Surface\" \"Fuel Moisture\" \"By Size Class\" \"Live Woody Fuel Moisture\"]]

   Arguments:
   - path: Vector of path elements [Module Submodule Groups...]

   Returns:
   Sequence of parent paths, or nil if path has 2 or fewer elements

   Implementation pattern from test_matrix_generator.clj lines 358-375"
    [path]
    (when (> (count path) 2)
      (let [module     (take 1 path)
            rest-path  (drop 1 path)
            num-groups (count rest-path)]
        (for [i (range 1 num-groups)]
          (vec (concat module (take i rest-path)))))))

  (def group {:path
               ["Surface"
                "Wind and Slope"
                "Wind Adjustment Factor"
                "Wind Adjustment Factor - User Input"],
               :group/research?       nil,
               :parent-submodule/io   :input,
               :group/translated-name "Wind Adjustment Factor - User Input",
               :group/hidden?         nil,
               :group/order           nil,
               :submodule/order       3,
               :conditionals
               {:conditionals
                [{:type     :group-variable,
                  :operator :equal,
                  :values   ["User Input"],
                  :group-variable
                  {:group-variable/translated-name
                   "Wind Adjustment Factor Calculation Method",
                   :group-variable/research? nil,
                   :io                       :input,
                   :path                     ["Surface" "Wind and Slope" "Wind Adjustment Factor"],
                   :submodule/order          3,
                   :submodule/research?      nil,
                   :group/order              3}}],
                :conditionals-operator nil}})

  (def ancestor-paths (collect-parent-groups (:path group)))


  (def conditional-with-sub-conditionals
    {:type                     :group-variable,
     :operator                 :in,
     :values                   ["20-Foot" "10-Meter"],
     :group-variable
     {:group-variable/translated-name "Wind Measured at:",
      :group-variable/research?       nil,
      :io                             :input,
      :path                           ["Surface" "Wind and Slope" "Wind Measured at:"],
      :submodule/order                3,
      :submodule/research?            nil,
      :group/order                    nil},
     :sub-conditionals
     [{:type     :group-variable,
       :operator :equal,
       :values   ["true"],
       :group-variable
       {:group-variable/translated-name "Heading",
        :group-variable/research?       nil,
        :io                             :output,
        :path                           ["Surface" "Fire Behavior" "Direction Mode"],
        :submodule/order                0,
        :submodule/research?            nil,
        :group/order                    0}}
      {:type     :group-variable,
       :operator :equal,
       :values   ["true"],
       :group-variable
       {:group-variable/translated-name "Flame Length",
        :group-variable/research?       nil,
        :io                             :output,
        :path                           ["Crown" "Fire Behavior" "Fire Behavior"],
        :submodule/order                0,
        :submodule/research?            nil,
        :group/order                    0}}],
     :sub-conditional-operator :or})


  (defn process-sub-conditionals [conditional-info]
    (let [sub-conds (:sub-conditionals conditional-info)
          op    (:sub-conditional-operator conditional-info)]
      (if (= op :or)
        (mapv (fn [sub-cond]
               [(dissoc conditional-info :sub-conditionals conditional-info) (process-sub-conditionals sub-cond)]) sub-conds)
       ;; else (= op :and)
        conditional-info
       )))

  (process-sub-conditionals conditional-with-sub-conditionals)


  (def required-setup-conditionals
    (mapv (fn [ancestor-path]
            (let [ancestor-cond-info (:conditionals (get test-data ancestor-path))
                  conds              (:conditionals ancestor-cond-info)
                  op                 (:conditionals-operator ancestor-cond-info)]
              (if (= op :or)
                (mapv process-sub-conditionals conds)
                ;;else (= op :and)
                [(mapv process-sub-conditionals conds)]
                )))
          ancestor-paths))

  )
