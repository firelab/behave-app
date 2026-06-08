(ns behave.components.core
  (:require
   [behave.components.accordion    :as accordion]
   [behave.components.button       :as button]
   [behave.components.card         :as card]
   [behave.components.compute      :as compute]
   [behave.components.icon.core    :as icon]
   [behave.components.inputs       :as inputs]
   [behave.components.progress     :as progress]
   [behave.components.matrix-table :as matrix-table]
   [behave.components.modal        :as modal]
   [behave.components.note         :as note]
   [behave.components.tab          :as tab]
   [behave.components.table        :as table]))

(def ^{:argslist
       '([config])

       :doc
       "Accordion component which takes a hash-map of:
       - `:accordion-items` A vector of accordion items
       - `:default-open?`   Boolean which opens the accordion by default.

       accordion item is a hash-map of
       - `:label` Accordion item's label.
       - `:content` Item label's content.

       Usage:

          [accordion {:accordion-items [{:label   \"Accordion Item 1\"
                                         :content \"Lorem ipsum dolor accordion content.\"}
                                        {:label   \"Accordion Item 2\"
                                         :content \"Lorem ipsum dolor accordion content.\"}
                                        {:label   \"Accordion Item 3\"
                                         :content \"Lorem ipsum dolor accordion content.\"}]}]"}
  accordion accordion/accordion)

(def ^{:argslist
       '([config])

       :doc
       "Button component which takes a hash-map of:
       - `:label`    Button's label as a string.
       - `:variant`  String of either \"primary\", \"secondary\", \"highlight\" or \"back\".
       - `:size`     String of either \"small\", or \"normal\".
       - `:on-click` Function called on click.

       Usage:

          [button {:label   \"Hello World\"
                   :size    \"small\"
                   :variant \"primary\"
                   :on-click #(println \"Clicked!\")}]"}
  button button/button)

(def ^{:argslist
       '([config])

       :doc
       "Browse which takes a hash-map of:
       - `:button-label` Button label.
       - `:label`        Label.
       - `:accept`       Extensions that will be accepted.
       - `:focused?`     Whether input is focused.
       - `:disabled?`    Whether input is disabled.
       - `:on-change`    Function which is called when card is selected.

       Usage:

          [card {:title     \"Fire\"}]
                 :icons     [{:icon-name \"Fire\"}]
                 :content   \"Fire is a necessary part of a healthy ecosystem.\"
                 :on-select #(reset! selected-card %)}]"}
  browse-input inputs/browse-input)

(def ^{:argslist
       '([config])

       :doc
       "Card component which takes a hash-map of:
       - `:title`         Card title.
       - `:content`       Card's content.
       - `:size`          String of either \"normal\" or \"large\". Defaults to \"normal\".
       - `:icons`         Vector of icons to display. See [[icon]].
       - `:icon-position` String of either \"left\" or \"top\". Defaults to \"left\".
       - `:disabled?`     Whether card is disabled.
       - `:selected?`     Whether card is selected.
       - `:on-select`     Function which is called when card is selected.

       Usage:

          [card {:title     \"Fire\"}]
                 :icons     [{:icon-name \"Fire\"}]
                 :content   \"Fire is a necessary part of a healthy ecosystem.\"
                 :on-select #(reset! selected-card %)}]"}
  card card/card)

(def ^{:argslist
       '([config])

       :doc
       "Compute component which takes a hash-map of:
       - `:compute-btn-label` Label for the compute button.
       - `:compute-fn`        Function this component should compute.
       - `:compute-args`      Vector of hash-maps describing the args
                              to be passed into :compute-fn. See example. Note that the order
                              must match the order of the args in :compute-fn.
                              `units` and `range` are optional.
       - `:on-compute`        Callback Function to be used compute button is clicked.

       Usage:

          [compute {:compute-btn-label \"Select Range\"
                    :compute-fn        (fn [from to step]
                                        (range from to step))
                    :compute-args      [{:name  \"From\"
                                         :units \"ac\"
                                         :range [0 100]}
                                        {:name  \"To\"
                                         :units \"ac\"
                                         :range [0 100]}
                                        {:name  \"Steps\"
                                         :units \"ac\"
                                         :range [0 100]}]
                    :on-compute        #(js/console.log \"computed:\" %)}]"}
  compute compute/compute)

(def ^{:argslist
       '([config])

       :doc
       "Card group component which takes a hash-map of:
       - `:cards`          Vector of maps (e.g. `:icons`, `:title`, `:content`). See [[card]].
       - `:card-size`      String of either \"normal\" or \"large\". Defaults to \"normal\".
       - `:flex-direction` String of either \"row\" or \"column\". Defaults to \"row\".
       - `:icon-position`  String of either \"left\" or \"top\". Defaults to \"left\".
       - `:on-select`      Function called with the selected card's map.

       Usage:

          [card-group {:cards [{title: \"Fire-1\" :icons [:icon-name \"Fire\"]
                               {title: \"Fire-2\" :icons [:icon-name \"Fire\"]}}]
                       :on-select #(reset! selected-card %)}]"}
  card-group card/card-group)

(def ^{:argslist
       '([icon-name])

       :doc
       "Icon component which takes either a keyword, string, or hash-map with the key `:icon-name`.

       Usage:

          ;; Keyword
          [icon :fire]

          ;; String
          [icon \"fire\"]

          ;; Hash-map w/ :icon-name
          [icon {:icon-name \"fire\"}]"}
  icon icon/icon)

(def ^{:argslist
       '([config])

       :doc
       "Checkbox component which takes a hash-map of:
       - `:label`     Label for the input.
       - `:id`        Form element id for the label and input.
       - `:name`      Form element name for the label and input.
       - `:checked?`  Whether the input is checked.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-change` Function called with the change event.

       Usage:

          [checkbox {:label     \"Check me\"
                     :id        \"check-me\"
                     :name      \"check-me\"
                     :on-change #(println \"Changed!\")]"}
  checkbox inputs/checkbox)

(def ^{:argslist
       '([config])

       :doc
       "Dropdown component which takes a hash-map of:
       - `:label`     Label for the input.
       - `:id`        Form element id for the label and select.
       - `:name`      Form element name for the label and select.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-change` Function called with the change event.
       - `:options`   Vector of `option` maps with the keys: `:label`, `:value`, `selected?` (optional).

       Usage:

          [dropdown {:label     \"Dropdown\"
                     :id        \"my-dropdown\"
                     :name      \"my-dropdown\"
                     :on-change #(println \"Changed to value:\" %)
                     :options   [{:label \"First\" :value 1 :selected? true}
                                 {:label \"Second\" :value 2}]"}
  dropdown inputs/dropdown)

(def ^{:argslist
       '([config])

       :doc
       "Number input component which takes a hash-map of:
       - `:label`      Label for the input.
       - `:id`         Form element id for the label and input.
       - `:name`       Form element name for the label and input.
       - `:min`        Minimum value.
       - `:max`        Maximum value.
       - `:disabled?`  Whether the input is disabled.
       - `:error?`     Whether the input is in an error state.
       - `:on-change`  Function called with the change event.
       - `:value`      Value.
       - `:value-atom` A value atom. Supercedes :value if both is passed in.

       Usage:

          [number-input {:label     \"My Number\"
                        :id        \"my-number\"
                        :name      \"my-number\"
                        :min       0
                        :max       1000
                        :on-change #(println \"Changed!\")]"}
  number-input inputs/number-input)

(def ^{:argslist
       '([config])

       :doc
       "Progress component which takes a hash-map of:
       - `:steps` Vector of hash-maps with `:label`, `:selected?`, `:completed?`,
         `:on-select` (optional).

       When `:on-select` exists in the steps hashmap it will overwrite the on-select at the top level
       of the args to the progress component.

       Usage:

          [progress {:on-select #(println \"Clicked!\")
                     :steps     [{:label \"First Step\" :completed? true}
                                 {:label \"Second Step\" :completed? false :selected? true}]}]]"}
  progress progress/progress)

(def ^{:argslist
       '([config])

       :doc
       "Radio input component which takes a hash-map of:
       - `:label`     Label for the input.
       - `:id`        Form element id for the label and input.
       - `:name`      Form element name for the label and input.
       - `:checked?`  Whether the input is checked.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-change` Function called with the change event.

       Usage:

          [radio-input {:label     \"Radio me\"
                        :id        \"radio-me\"
                        :name      \"radio-me\"
                        :on-change #(println \"Changed!\")]"}
  radio-input inputs/radio-input)

(def ^{:argslist
       '([config])

       :doc
       "Radio group component which takes a hash-map of:
       - `:label`     Label for the radio group.
       - `:options`   A collection of hash-maps for the radio options.


       Options are can each take:
       - `:label`     Label for the input.
       - `:id`        Form element id for the label and input.
       - `:name`      Form element name for the label and input.
       - `:checked?`  Whether the input is checked.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-change` Function called with the change event.

       Usage:

          [radio-group {:label   \"New Radio Group\"
                        :options [{:label     \"Radio one\"
                                   :id        \"radio-one\"
                                   :name      \"radio-one\"
                                   :on-change #(println \"Changed!\")}
                                  {:label     \"Radio two\"
                                   :id        \"radio-two\"
                                   :name      \"radio-two\"
                                   :on-change #(println \"Changed!\")}]"}
  radio-group inputs/radio-group)

(def ^{:argslist
       '([config])

       :doc
       "Toggle switch component which takes a hash-map of:
       - `:label`       Optional label for the toggle group.
       - `:left-label`  Label displayed to the left of the toggle.
       - `:right-label` Label displayed to the right of the toggle.
       - `:checked?`    Whether the toggle is in the on/checked state.
       - `:disabled?`   Whether the toggle is disabled.
       - `:on-change`   Function called with the change event when toggled.

       Usage:

          [toggle {:left-label  \"English\"
                   :right-label \"Metric\"
                   :checked?    true
                   :on-change   #(println \"Toggled!\")}]"}
  toggle inputs/toggle)

(def ^{:argslist
       '([config])

       :doc
       "Range input component which takes a hash-map of:
       - `:label`     Label for the input.
       - `:id`        Form element id for the label and input.
       - `:name`      Form element name for the label and input.
       - `:min`       Minimum value.
       - `:max`       Maximum value.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-change` Function called with the change event.

       Usage:

          [range-input {:label     \"My Range\"
                        :id        \"my-range\"
                        :name      \"my-range\"
                        :min       0
                        :max       1000
                        :on-change #(println \"Changed!\")]"}
  range-input inputs/range-input)

(def ^{:argslist
       '([config])

       :doc
       "Tab component which takes a hash-map of:
       - `:label`     Button's label as a string.
       - `:variant`   String of either \"primary\", \"secondary\", or \"highlight\".
       - `:selected?` Whether the tab is selected.
       - `:on-click`  Function called when tab is clicked.
       - `:icon-name` Icon to display. See [[icon]].
       - `:disabled?` Whether the tab is disabled.
       - `flat-edge`  String of either \"top\" or \"bottom\".
       - `size`       String of either \"small\" , \"normal\", or \"large\".
       - `order-id`   Integer representing the order in a [[tab-group]] of this tab.

       Usage:

          [tab {:label     \"Hello World\"
                :selected? true
                :on-click #(println \"Selected!\")}]"}
  tab tab/tab)

(def ^{:argslist
       '([config])

       :doc
       "Tab Group component which takes a hash-map of:
       - `:tabs`      Vector of tab hash-maps. See [[tab]].
       - `:variant`   String of either \"primary\", \"secondary\", or \"highlight\".
       - `:flat-edge` String of either \"top\" or \"bottom\".
       - `:size`      String of either \"small\" , \"normal\", or \"large\".
       - `:on-click`  Function called when tab is clicked.
       - `:align`     String of either \"left\" or \"right\".

       Usage:

          [tab-group {:tabs      [{:label \"First Tab\" :selected? true :order-id 0}
                                  {:label \"Second Tab\" :selected? false :order-id 1}]
                      :on-click  #(println \"Selected!\")}]"}
  tab-group tab/tab-group)

(def ^{:argslist
       '([config])

       :doc
       "Text input component which takes a hash-map of:
       - `:disabled?`     Whether the input is disabled.
       - `:error?`        Whether the input is in an error state.
       - `:focused?`      Whether the input is in a focused state.
       - `:id`            Form element id for the label and input.
       - `:label`         Label for the input.
       - `:name`          Form element name for the label and input.
       - `:on-blur`       Function called with the blur event.
       - `:on-change`     Function called with the change event.
       - `:on-focus`      Function called with the blur event.
       - `:placeholder`   Specifies a short hint that describes the expected value of the input field
       - `:value`         Value.
       - `:value-atom`    A value atom. Supercedes :value if both is passed in.
       - `:default-value` Default value.

       Usage:

          [text-input {:id          \"identifier\"
                       :label       \"Text Input Label\"
                       :name        \"Text Input Name\"
                       :on-blur     #(println \"Blurred!\")
                       :on-focus    #(println \"Focused!\")
                       :on-change   #(println \"Changed!\")
                       :placeholder \"Enter hint\"]"}
  text-input inputs/text-input)

(def ^{:argslist
       '([config])

       :doc
       "Modal component which takes a hash-map of:
       - `title`           Title to display as a string.
       - `:close-on-click` Function called on click of the close icon.
       - `:content`        Card's content.
       - `buttons`         Optional Vector of buttons. See [[button]].
       - `:icon`           Optional Icon to display. See [[icon]].

       Usage:

          [modal {:title        \"Title\"
                  :icon         {:icon-name \"help2\"}
                  :close-button {:on-click #(js/console.log \"Clicked!\")}
                  :buttons      [{:variant  \"primary\"
                                  :label    \"button-1\"
                                  :on-click #(js/console.log \"Clicked!\")}
                                 {:variant  \"secondary\"
                                  :label    \"button-2\"
                                  :on-click #(js/console.log \"Clicked!\")}]
                  :content      \"content\"}]"}
  modal modal/modal)

(def ^{:argslist
       '([config])

       :doc
       "Note component which takes a hash-map of:
       - `:title-label`       Label of the note's title input component.
       - `:title-placeholder` Placehodler value in the note's title input component.
       - `:body-placeholder`  Placeholder for the note's body.
       - `on-save`            Function to be called when ths save button is clicked.
       - `limit`              Number of characters allowed for the body. Defaults

       Usage:

          [note {:title-label       \"Note's Name / Category\"
                 :title-placeholder \"Enter note's name or category\"
                 :body-placeholder  \"Add notes\"
                 :on-save           #(js/console.log %)
                 :limit             10}]"}
  note note/note)

(def ^{:argslist
       '([config])

       :doc
       "Table component which takes a hash-map of:
       - `title`   Title to display as a string.
       - `headers` Vector of column header names as strings or a map representing the structure of
                   grouped headers.
       - `columns` Vector of keywords.
       - `rows`    Vector of row data hash-maps. Row data maps may include an optional key `:shaded?` with a boolean value.

       Be sure to match column keywords to the keys in the row data hash-maps.

       Usage:

          [table {:title   \"Table Title\"
                  :headers [\"Column 1\" \"Column 2\" \"Column 3\"]
                  :columns [:column1 :column2 :column3]
                  :rows    [{:column1 1
                             :column2 2
                             :column3 3
                             :shaded? true}
                            {:column1 4
                             :column2 5
                             :column3 6}
                            {:column1 7
                             :column2 8
                             :column3 9}]}]

        Grouped Header Example:

        {\"SH 1\" {\"SH 1-1\" [\"Column 1\" \"Column 2\"]
                   \"SH 1-2\" [\"Column 3\" \"Column 4\"]}
         \"SH 2\" {\"SH 2-1\" [\"Column 5\" \"Column 6\"]
                   \"SH 2-2\" [\"Column 7\" \"Column 8\"]}}"}
  table table/table)

(def ^{:argslist
       '([config])

       :doc
       "Matrix-table component which takes a hash-map of:
       - `title`          Title to display as a string.
       - `column-headers` Vector of column-header maps
       - `row-headers`    Vector of row-header spec
       - `data`           Map of [row-key column-key] -> value
       - `rows-label`     (Optional) String to represent a label for all columns. In the case the column names                            are just numbers.
       - `cols-label`     (Optional) String to represent a label for all columns. In the case the column names                            are just numbers.


       column-header and row-header
       - `name` String name of the column header
       - `key`  Keyword or number for the column header that matches the respective parts of the lookup key
                in the `data`

       The entry in the column-headers header should be the name for the rows

       Usage:

          [table {:title          \"Matrix Title\"
                  :rows-label      \"Rows\"
                  :cols-label      \"Columns\"
                  :column-headers [{:name \"Row Name\"}
                                   {:name \"Column 1\" :key :column1}
                                   {:name \"Column 2\" :key :column2}
                                   {:name \"Column 3\" :key :column3}]
                  :row-headers    [{:name \"Row 1\" :key :row1}
                                   {:name \"Row 2\" :key :row2}
                                   {:name \"Row 3\" :key :row3}]
                  :data           {[:row1 :column1] 1
                                   [:row1 :column2] 2
                                   [:row1 :column3] 3

                                   [:row2 :column1] 4
                                   [:row2 :column2] 5
                                   [:row2 :column3] 6

                                   [:row3 :column1] 7
                                   [:row3 :column2] 8
                                   [:row3 :column3] 9}}]"}
  matrix-table matrix-table/matrix-table)

(def ^{:argslist
       '([config])

       :doc
       "Multi Select component which takes a hash-map of:
       - `:input-label` Label for the input.
       - `:on-select`   Function called when an option is selected. `value` from the option is
                        passed to the function.
       - `:on-deselect` Function called when an option is de-selected. `value` from the option is
                        passed to the function.
       - `:options`     Vector of `option` maps with the

           keys: - `:label`       Label for the option
                 - `:value`,      Value for the option
                 - `:on-select`   Function called when option is selected (optional). `value` is
                                  passed to the function.
                 - `:on-deselect` Same as `on-select`.
                 - `:selected?`   (optional).

       Usage:

         [multi-select-input {:input-label \"Input Label\"
                              :options     [{:label     \"option1\"
                                             :value     1
                                             :on-select #(prn %)
                                             :on-deselect #(prn %)}

                                            {:label     \"option2\"
                                             :value     2
                                             :on-select #(prn %)
                                             :on-deselect #(prn %)}

                                            {:label     \"option3\"
                                             :value     3
                                             :on-select #(prn %)
                                             :on-deselect #(prn %)}"}
  multi-select-input inputs/multi-select-input)
