(ns behave.components.core
  (:require
    [behave.components.accordion         :as accordion]
    [behave.components.button            :as button]
    [behave.components.card              :as card]
    [behave.components.icon.default-icon :as icon]
    [behave.components.inputs            :as inputs]
    [behave.components.progress          :as progress]
    [behave.components.modal             :as modal]
    [behave.components.tab               :as tab]
    [behave.components.table             :as table]))

(def ^{:argslist
       '([config])

       :doc
       "Accordion component which takes a hash-map of:
       - `:title`     Title to display as a string.
       - `:icon-name` Icon to display. See [[icon]].
       - `:variant`   String of either \"information\", \"research\", \"settings\" or \"tools\".
       - `:opened?`   Whether the accordion is opened.
       - `:on-toggle` Function called when the accordion is toggled.

       Usage:

          (let [opened? (r/atom false)]
            [accordion {:title     \"Settings\"
                        :icon-name \"settings\"
                        :variant   \"settings\"
                        :opened?   @opened?
                        :on-toggle #(reset! opened? (not @opened?))}])"}
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
       - `:options`   Vector of options with the keys `:label` and `:value`.

       Usage:

          [dropdown {:label     \"Dropdown\"
                     :id        \"my-dropdown\"
                     :name      \"my-dropdown\"
                     :on-change #(println \"Changed to value:\" %)
                     :options   [{:label \"First\" :value 1}
                                 {:label \"Second\" :value 2}]"}
  dropdown inputs/dropdown)

(def ^{:argslist
       '([config])

       :doc
       "Number input component which takes a hash-map of:
       - `:label`     Label for the input.
       - `:id`        Form element id for the label and input.
       - `:name`      Form element name for the label and input.
       - `:focus?`    Whether the input is focused.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-click`  Function called with the click event.
       - `:on-load`   Function called with the load event.

       Usage:
          [number-input {:label    \"My Number\"
                         :id       \"my-number\"
                         :name     \"my-number\"
                         :on-click #(println \"Changed!\")
                         :on-load  #(println \"Changed!\")]"}
  browse-input inputs/browse-input)

(def ^{:argslist
       '([config])

       :doc
       "Number input component which takes a hash-map of:
       - `:label`     Label for the input.
       - `:id`        Form element id for the label and input.
       - `:name`      Form element name for the label and input.
       - `:min`       Minimum value.
       - `:max`       Maximum value.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-change` Function called with the change event.

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
       - `:steps` Vector of hash-maps with `:label`, `:selected?`, `:completed?`.

       Usage:

          [progress {:steps [{:label \"First Step\" :completed? true}
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
       '([props])

       :doc
       "Radio group input component which takes a `props` hash-map of:
       - `:label`     Label for the group.
       - `:options`   Vector of options for the group.
       - `:name`      Name of the group.

       Each option is a hash-map of:
       - `:id`        Form element id for the label and input.
       - `:name`      Form element name for the label and input.
       - `:checked?`  Whether the input is checked.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-change` Function called with the change event.

       Usage:

       ```clojure
       [radio-group {:label   \"Radio Group\"
                     :name    \"radio-second\"
                     :options [{:label     \"Radio First\"
                                 :id        \"radio-first\"
                                 :selected  true
                                 :on-change #(println \"Changed!\")}
                                {:label     \"Radio Second\"
                                 :id        \"radio-second\"
                                 :selected  false
                                 :on-change #(println \"Changed!\")}]}]
       ```"}
  radio-group inputs/radio-group)

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
       - `:variant`   String of either \"outline-primary\", \"outline-secondary\", or \"outline-highlight\".
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
       - `:variant`   String of either \"outline-primary\", \"outline-secondary\", or \"outline-highlight\".
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
       - `:disabled?`   Whether the input is disabled.
       - `:error?`      Whether the input is in an error state.
       - `:focused?`    Whether the input is in a focused state.
       - `:id`          Form element id for the label and input.
       - `:label`       Label for the input.
       - `:name`        Form element name for the label and input.
       - `:on-blur`     Function called with the blur event.
       - `:on-change`   Function called with the change event.
       - `:on-focus`    Function called with the blur event.
       - `:placeholder` Specifies a short hint that describes the expected value of the input field
       - `:value`       Default value.

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
       "System message component which takes a hash-map of:
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
       "Table component which takes a hash-map of:
       - `title`   Title to display as a string.
       - `headers` Vector of column header names as strings.
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
                             :column3 9}]}]"}
  table table/table)
