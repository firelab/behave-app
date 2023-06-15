(ns behave-cms.styles
  (:require-macros [herb.core :refer [defglobal]])
  (:require herb.runtime))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn combine
  "Combines all provided styles into one map."
  [& styles]
  (apply merge
         (for [style styles]
           (cond
             (map? style)    style
             (fn? style)     (style)
             (vector? style) (apply (first style) (rest style))))))

(defn color-picker
  "Returns the rgba string of a provided color keyword and opacity."
  ([color]
   (color-picker color 1))
  ([color alpha]
   (case color
     :bg-color         (color-picker :dark-gray 0.9)
     :bg-hover-color   (color-picker :white)
     :border-color     (color-picker :white alpha)
     :font-color       "rgb(235, 235, 235)"
     :font-hover-color (color-picker :black)
     :header-color     (str "rgb(24, 30, 36, "     alpha ")")
     :black            (str "rgba(0, 0, 0, "       alpha ")")
     :blue             (str "rgba(0, 0, 175, "     alpha ")")
     :box-tan          (str "rgba(249, 248, 242, " alpha ")")
     :brown            (str "rgba(96, 65, 31, "    alpha ")")
     :dark-gray        (str "rgba(50, 50, 50, "    alpha ")")
     :dark-green       (str "rgba(0, 100, 0, "     alpha ")")
     :light-gray       (str "rgba(230, 230, 230, " alpha ")")
     :orange           (str "rgba(249, 104, 65, "  alpha ")")
     :red              (str "rgba(200, 0, 0, "     alpha ")")
     :transparent      (str "rgba(0, 0, 0, 0)")
     :white            (str "rgba(255, 255, 255, " alpha ")")
     :yellow           (str "rgba(249, 175, 59, "  alpha ")")
     color)))

(defn- to-merge? [modifiers key return]
  (when (contains? (set modifiers) key) return))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global Styles
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defglobal general
  [:* {:box-sizing "border-box"}]

  [:label {:font-size     "1rem"
           :font-weight   "bold"
           :margin-bottom "0"}]

  [:img {:max-width "100%"
         :height    "auto"}])

(defglobal body-and-content-divs
  [:body {:background-color (color-picker :white)
          :height           "100%"
          :position         "relative"}]
  [:#app {:height "100%"}])

(defglobal over-write-styles
  [:#header {:position "static"}]
  [:#nav-row {:padding "0"}]
  [:.mapboxgl-popup-close-button {:font-size "1.5rem"
                                  :padding   ".25rem"}]
  [:.mapboxgl-popup-close-button:focus {:outline 0}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pseudo / Class Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn p-bordered-input
  "A Herb class for a bordered input with a style when disabled.
   Optionally takes a :flex-style and/or :multi-style as `modifiers`."
  [& modifiers]
  (let [base-style     {:background-color (color-picker :white)
                        :border           "1px solid"
                        :border-color     (color-picker :brown)
                        :border-radius    "2px"
                        :font-family      "inherit"
                        :height           "1.75rem"
                        :padding          ".25rem .5rem"}
        flex-style     {:width            "100%"}
        multi-style    {:height           "inherit"
                        :padding          ".3rem .5rem"
                        :resize           "vertical"}
        disabled-style {:background-color "rgb(236, 236, 236)"
                        :color            "black"}]
    (with-meta
      (merge base-style
             (to-merge? modifiers :flex-style  flex-style)
             (to-merge? modifiers :multi-style multi-style))
      {:pseudo {:disabled disabled-style}})))

(defn p-add-hover
  "A Herb class for a background color on hover.
   Background of button is highlighted when `active?` is true or on hover."
  [& [active?]]
  (let [highlight-color (color-picker :border-color 0.2)]
    (with-meta
      {:background-color (if active? highlight-color (color-picker :transparent))
       :border-radius    "4px"}
      {:pseudo {:hover {:background-color highlight-color}}})))

(defn p-button
  "A Herb class for non-tool buttons.
   Optionally takes :small or :large for the size of the button."
  [bg-color border-color color bg-color-hover color-hover & [btn-size]]
  (let [base-style     {:background-color (color-picker bg-color)
                        :border-color     (color-picker border-color)
                        :border-width     "2px"
                        :border-style     "solid"
                        :border-radius    "20px / 50%"
                        :color            (color-picker color)
                        :cursor           "pointer"
                        :fill             (color-picker color)
                        :font-size        (case btn-size
                                            :large "1rem"
                                            "0.85rem")
                        :outline          "none"
                        :padding          (case btn-size
                                            :small "0.5rem 0.35rem 0.4rem"
                                            :large "0.5rem 1.75rem 0.4rem"
                                            "0.5rem 0.75rem 0.4rem")
                        :text-transform   "uppercase"}
        disabled-style {:opacity          "0.5"
                        :pointer-events   "none"}]
    (with-meta
      base-style
      {:pseudo {:disabled disabled-style
                :focus    {:outline "none"}
                :hover    {:background-color (color-picker bg-color-hover)
                           :color            (color-picker color-hover)
                           :fill             (color-picker color-hover)}}})))

(defn p-form-button
  "A Herb class for form buttons (e.g. the Refresh button on the Match Drop Dashboard)."
  [& [btn-size]]
  (p-button :white :yellow :brown :orange :white btn-size))

(defn p-themed-button
  "A Herb class for themed buttons (e.g. buttons on Match Drop tool)."
  [& [btn-size]]
  (p-button :bg-color :border-color :font-color :bg-hover-color :font-hover-color btn-size))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Style Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn color
  "A shortcut for adding a color style."
  [color]
  {:color (color-picker color)})

(defn bg-color
  "A shortcut for adding a background color style."
  [color]
  {:background (color-picker color)})

(defn margin
  "A shortcut for adding margin styles given the amount and direction keyword."
  [& modifiers]
  (if (= 1 (count modifiers))
    {:margin (first modifiers)}
    (apply merge
           {}
           (for [[size position] (partition 2 modifiers)]
             (case position
               :t {:margin-top    size}
               :b {:margin-bottom size}
               :l {:margin-left   size}
               :r {:margin-right  size}
               :v {:margin-top    size
                   :margin-bottom size}
               :h {:margin-left   size
                   :margin-right  size}
               {})))))

(defn padding
  "A shortcut for adding padding styles given the amount and direction keyword."
  [& modifiers]
  (if (= 1 (count modifiers))
    {:padding (first modifiers)}
    (apply merge
           {}
           (for [[size position] (partition 2 modifiers)]
             (case position
               :t {:padding-top    size}
               :b {:padding-bottom size}
               :l {:padding-left   size}
               :r {:padding-right  size}
               :v {:padding-top    size
                   :padding-bottom size}
               :h {:padding-left   size
                   :padding-right  size}
               {})))))

(defn border
  "A shortcut for adding border styles given the amount and direction keyword."
  [& modifiers]
  (if (= 1 (count modifiers))
    {:border (first modifiers)}
    (apply merge
           {}
           (for [[style position] (partition 2 modifiers)]
             (case position
               :t {:border-top    style}
               :b {:border-bottom style}
               :l {:border-left   style}
               :r {:border-right  style}
               :v {:border-top    style
                   :border-bottom style}
               :h {:border-left   style
                   :border-right  style}
               {})))))

(defn align
  "A shortcut for adding different alignment styles given the type and position."
  [type position]
  (let [style (str (name type) "-" (name position))]
    (case (keyword style)
      :text-center  {:text-align "center"}

      :text-left    {:text-align "left"}

      :text-right   {:text-align "right"}

      :block-center {:display      "block"
                     :margin-left  "auto"
                     :margin-right "auto"}

      :block-left   {:display      "block"
                     :margin-left  "0"
                     :margin-right "auto"}

      :block-right  {:display      "block"
                     :margin-left  "auto"
                     :margin-right "0"}

      :flex-center  {:display         "flex"
                     :justify-content "center"
                     :align-items     "center"}

      :flex-left    {:display         "flex"
                     :justify-content "flex-start"}

      :flex-right   {:display         "flex"
                     :justify-content "flex-end"}

      :flex-top     {:display         "flex"
                     :align-items     "flex-start"}

      :flex-bottom  {:display         "flex"
                     :align-items     "flex-end"}
      {})))

(defn flex-row
  "A shortcut for adding a flex row style."
  []
  {:align-items     "center"
   :display         "flex"
   :flex-direction  "row"
   :flex-wrap       "nowrap"
   :justify-content "space-between"})

(defn flex-col
  "A shortcut for adding a flex column style."
  []
  {:align-items     "center"
   :display         "flex"
   :flex-direction  "column"
   :justify-content "flex-start"})

(defn font
  "A shortcut for adding a font style.
   Takes font attributes their associated values (eg. :size '10px' :family 'Arial')"
  [& modifiers]
  (apply merge
         {}
         (for [[attr val] (partition 2 modifiers)]
           (when (contains? #{:style :variant :weight :size :family} attr)
             {(keyword (str "font-" (name attr))) val}))))

(defn disabled-group
  "A shortcut for adding disabled styling."
  [disabled?]
  (if disabled?
    {:opacity        "0.4"
     :pointer-events "none"}
    {}))

(defn root
  "A shortcut for the root styling of a page."
  []
  {:align-items    "center"
   :display        "flex"
   :flex           "1 1 0"
   :flex-direction "column"
   :overflow       "auto"
   :padding        "0 1rem"})

(defn modal
  "A shortcut for modal styling."
  []
  {:background-color "rgba(0,0,0,0.4)"
   :height           "100%"
   :left             "0"
   :position         "absolute"
   :top              "0"
   :width            "100%"
   :z-index          "5000"})

(defn outline
  "A shortcut for adding a solid, colored border."
  [color]
  (let [rgb (color-picker color)]
    {:background   (color-picker :white)
     :border-color rgb
     :border-style "solid"
     :color        rgb}))

(defn text-ellipsis
  "A shortcut for adding ellipsis on content overflow."
  []
  {:overflow      "hidden"
   :text-overflow "ellipsis"
   :white-space   "nowrap"})

(defn fixed-size
  "A shortcut that makes an element a fixed size."
  [size]
  {:float      "left"
   :height     size
   :min-height size
   :min-width  size
   :width      size})

(defn tool
  "A shortcut for tool styling (eg. the legend or time slider)."
  []
  {:background-color (color-picker :bg-color)
   :border-radius    "5px"
   :box-shadow       "rgba(50, 50, 93, .7) 0px 3px 20px -8px, rgba(0, 0, 0, 0.3) 0px 3px 16px -11px"
   :color            (color-picker :font-color)
   :position         "absolute"
   :z-index          "100"})

(def light-border (str "1.2px solid " (color-picker :brown)))

(defn action-box
  "A shortcut for styling an action box."
  []
  {:background-color (color-picker :white)
   :border           light-border
   :border-radius    "5px"
   :display          "flex"
   :flex-direction   "column"
   :height           "100%"
   :overflow         "hidden"
   :padding          "0"
   :width            "100%"})

(defn action-header
  "A shortcut for styling the header within an action box."
  []
  {:align-items      "center"
   :background-color (color-picker :light-gray)
   :border-bottom    light-border
   :display          "flex"
   :flex-direction   "row"
   :flex-wrap        "nowrap"
   :height           "2.5rem"
   :justify-content  "space-between"
   :padding          ".25rem .7rem"
   :width            "100%"})
