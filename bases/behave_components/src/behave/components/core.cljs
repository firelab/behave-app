(ns behave.components.core
  (:require
    [behave.components.accordion       :as accordion]
    [behave.components.button          :as button]
    [behave.components.card            :as card]
    [behave.components.header          :as header]
    [behave.components.icon            :as icon]
    [behave.components.inputs.browse   :as browse]
    [behave.components.inputs.checkbox :as checkbox]
    [behave.components.inputs.number   :as number]
    [behave.components.inputs.radio    :as radio]
    [behave.components.inputs.range    :as num-range]
    [behave.components.inputs.select   :as select]
    [behave.components.inputs.text     :as text]
    [behave.components.progress    :as progress]
    [behave.components.tab         :as tab]))

(def ^{:argslist
       '([props])

       :doc
       "Accordion component which takes a `props` hash-map of:
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
       '([props])

       :doc
       "Button component which takes a `props` hash-map of:
       - `:label`    Button's label as a string.
       - `:variant`  String of either \"primary\", \"secondary\", \"highlight\" or \"back\".
       - `:size`     String of either \"small\", or \"normal\".
       - `:on-click` Function called on click.

       Usage:

          [button {:label   \"Hello World\"
                   :size    \"small\"
                   :variant \"primary\"
                   :on-click #(println \"Clicked!\")}]"}
  browse-input browse/browse-input)

(def ^{:argslist
       '([props])

       :doc
       "Button component which takes a `props` hash-map of:
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
       '([props])

       :doc
       "Card component which takes a `props` hash-map of:
       - `:icon-name` Icon to display. See [[icon]].
       - `:title`     Card title.
       - `:content`   Card's content.
       - `:disabled?` Whether card is disabled.
       - `:selected?` Whether card is selected.
       - `:on-select` Function which is called when card is selected.

       Usage:

          [card {:icon-name \"fire\"
                  :title     \"Fire\"}]
                  :content   \"Fire is a necessary part of a healthy ecosystem.\"
                  :on-select #(reset! selected-card %)}]"}
  card card/card)

(def ^{:argslist
       '([props])

       :doc
       "Card group component which takes a `props` hash-map of:
       - `:cards`     Vector of maps (e.g. `:icon-name`, `:title`, `:content`). See [[card]].
       - `:on-select` Function called with the selected card's map.

       Usage:

          [card-group {:cards [{:icon-name \"Fire\" title: \"Fire\"}]
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
       '([props])

       :doc
       "Checkbox component which takes a `props` hash-map of:
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
  checkbox checkbox/checkbox)

(def ^{:argslist
       '([props])

       :doc
       "Dropdown component which takes a `props` hash-map of:
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
  dropdown select/select-input)

(def ^{:argslist '([label])
       :doc      "H1 Component with `label`."}
  h1 header/h1)

(def ^{:argslist '([label])
       :doc      "H2 Component with `label`."}
  h2 header/h2)

(def ^{:argslist '([label])
       :doc      "H3 Component with `label`."}
  h3 header/h3)

(def ^{:argslist '([label])
       :doc      "H4 Component with `label`."}
  h4 header/h4)

(def ^{:argslist '([label])
       :doc      "H5 Component with `label`."}
  h5 header/h5)

(def ^{:argslist '([label])
       :doc      "H6 Component with `label`."}
  h6 header/h6)

(def ^{:argslist
       '([props])

       :doc
       "Number input component which takes a `props` hash-map of:
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
  number-input number/number-input)

(def ^{:argslist
       '([props])

       :doc
       "Progress component which takes a `props` hash-map of:
       - `:steps` Vector of hash-maps with `:label`, `:selected?`, `:completed?`.

       Usage:

          [progress {:steps [{:label \"First Step\" :completed? true}
                             {:label \"Second Step\" :completed? false :selected? true}]}]]"}
  progress progress/progress)

(def ^{:argslist
       '([props])

       :doc
       "Radio input component which takes a `props` hash-map of:
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
  radio-input radio/radio-input)


(def ^{:argslist
       '([props])

       :doc
       "Range input component which takes a `props` hash-map of:
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
  range-input num-range/range-input)

(def ^{:argslist
       '([props])

       :doc
       "Tab component which takes a `props` hash-map of:
       - `:label`     Button's label as a string.
       - `:selected?` Whether the tab is selected.
       - `:on-select` Function called when tab is selected.

       Usage:

          [tab {:label     \"Hello World\"
                :selected? true
                :on-select #(println \"Selected!\")}]"}
  tab tab/tab)

(def ^{:argslist
       '([props])

       :doc
       "Tab Group component which takes a `props` hash-map of:
       - `:tabs`      Vector of tab hash-maps.
       - `:on-select` Function called when tab is selected.

       Usage:

          [tab-group {:tabs      [{:label \"First Tab\" :selected? true}
                                  {:label \"Second Tab\" :selected? false}]
                      :on-select #(println \"Selected!\")}]"}
  tab-group tab/tab-group)

(def ^{:argslist
       '([props])

       :doc
       "Text input component which takes a `props` hash-map of:
       - `:label`     Label for the input.
       - `:id`        Form element id for the label and input.
       - `:name`      Form element name for the label and input.
       - `:disabled?` Whether the input is disabled.
       - `:error?`    Whether the input is in an error state.
       - `:on-change` Function called with the change event.

       Usage:

          [text-input {:label     \"My Text\"
                        :id        \"my-text\"
                        :name      \"my-text\"
                        :on-change #(println \"Changed!\")]"}
  text-input text/text-input)
