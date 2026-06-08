(ns behave-cms.components.sidebar.views
  (:require [behave-cms.routes :refer [app-routes]]
            [behave-cms.styles :refer [color-picker]]
            [bidi.bidi         :refer [path-for]]
            [herb.core         :refer [<class]]
            [re-frame.core     :as rf]))

;;; Styles

(def sidebar-width "300px")

(defn- $sidebar []
  {:position         "fixed"
   :top              "50px"
   :bottom           "0px"
   :width            sidebar-width
   :overflow-x       "hidden"
   :overflow-y       "auto"
   :border-right     (str "1px solid " (color-picker :light-gray))
   :background-color (color-picker :light-gray 0.4)
   :user-select      "none"})

(defn- $sidebar__row [active?]
  (cond-> ^{:pseudo {:hover {:background-color (color-picker :light-gray)
                             :color            (color-picker :dark-gray)
                             :cursor           "pointer"}}}
   {:display     "flex"
    :align-items "center"
    :gap         "2px"
    :color       (color-picker :dark-gray)
    :overflow    "hidden"}
    active? (merge {:background-color (color-picker :dark-gray)
                    :color            "white"})))

(defn- $sidebar__chevron [expanded? active?]
  {:display     "inline-block"
   :width       "24px"
   :flex-shrink "0"
   :font-size   "10px"
   :color       (if active? "white" (color-picker :dark-gray))
   :cursor      "pointer"
   :text-align  "center"
   :transform   (if expanded? "rotate(90deg)" "rotate(0deg)")
   :transition  "transform 0.1s ease"})

(defn- $sidebar__label []
  {:flex          "1"
   :overflow      "hidden"
   :text-overflow "ellipsis"
   :white-space   "nowrap"
   :font-size     "12px"
   :cursor        "pointer"
   :padding       "7px 4px 7px 0"})

;;; Helpers

(defn ->sidebar-links
  "Creates sidebar links for entities. Takes:
   - options [seq<map>]:  Sequence of entity maps. Must have `:bp/nid` attribute.
   - label-attr [keyword]: Attribute of map that will serve as the label
   - route [keyword]: Route that will be created with `(bidi/path-for app-routes <route> :nid (:bp/nid option))`"
  [options label-attr route]
  (->> options
       (map (fn [option]
              {:label (get option label-attr)
               :link  (path-for app-routes route :nid (:bp/nid option))}))
       (sort-by :label)))

;;; Tree component

(declare tree-node)

(defn- tree-node [node depth ancestor-ids active-id toggled]
  (let [id        (:id node)
        has-kids? (seq (:children node))
        ancestor? (contains? ancestor-ids id)
        active?   (= active-id id)
        manual?   (contains? toggled id)
        expanded? (not= ancestor? manual?)]
    [:div
     [:div {:class (<class $sidebar__row active?)
            :style {:padding-left (str (+ 8 (* 14 depth)) "px")}}
      (if has-kids?
        [:span {:class    (<class $sidebar__chevron expanded? active?)
                :on-click (fn [e]
                            (.stopPropagation e)
                            (rf/dispatch [:sidebar/toggle id]))}
         "▶"]
        [:span {:style {:display     "inline-block"
                        :width       "24px"
                        :flex-shrink "0"}}])
      [:span {:class    (<class $sidebar__label)
              :title    (:label node)
              :on-click (fn [e]
                          (.preventDefault e)
                          (if (:link node)
                            (rf/dispatch [:navigate (:link node)])
                            (rf/dispatch [:sidebar/toggle id])))}
       (:label node)]]
     (when (and has-kids? expanded?)
       [:div
        (for [child (:children node)]
          ^{:key (:id child)}
          [tree-node child (inc depth) ancestor-ids active-id toggled])])]))

;;; Public

(defn sidebar []
  (let [tree         @(rf/subscribe [:sidebar/tree])
        active-path  @(rf/subscribe [:sidebar/active-path])
        toggled      @(rf/subscribe [:sidebar/toggled])
        ancestor-ids (set (butlast active-path))
        active-id    (last active-path)]
    [:div {:class (<class $sidebar)}
     (for [n tree]
       ^{:key (:id n)}
       [tree-node n 0 ancestor-ids active-id toggled])]))
