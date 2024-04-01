(ns behave-cms.components.sidebar
  (:require
   [bidi.bidi         :refer [path-for]]
   [herb.core         :refer [<class]]
   [re-frame.core     :as rf]
   [behave-cms.routes :refer [app-routes]]))

;;; Styles

(def sidebar-width "200px")
(def ^:private $colors {:gray       "rgb(215, 215, 215)"
                        :blue       "rgb(0, 122, 255)"
                        :light-blue "rgba(0, 122, 255, 0.5)"})

(defn- $c [color]
  (color $colors))

(defn- $option []
  ^{:pseudo {:hover {:background-color ($c :light-blue)
                     :color            "white"
                     :cursor           "pointer"}}}
  {:display          "block"
   :border-bottom    (str "1px solid " ($c :gray))
   :padding          "8px"
   :text-decoration  "none"
   :color            "inherit"})

(defn- $sidebar []
  {:position         "fixed"
   :top              "50px"
   :bottom           "0px"
   :width            sidebar-width
   :overflow-x       "hidden"
   :border-right     (str "1px solid " ($c :gray))
   :background-color "white"})

(defn- $sidebar-back []
  ^{:pseudo {:hover {:text-decoration  "underline"
                     :cursor           "pointer"}}}
  {:font-weight      "bold"
   :font-size        "0.8rem"})

;;; Helpers

(defn- navigate [path]
  (rf/dispatch [:navigate path]))

(defn- on-click [f]
  #(do (.preventDefault %) (f)))

(defn- on-enter-space [f]
  #(when (#{13 32} (.-charCode %)) (f)))

;;; Components

(defn- sidebar-header [title parent-title parent-link]
  [:div
   {:style {:display       "flex"
            :flex-direction "column"
            :background    "rgb(245, 245, 245)"
            :padding       "10px"}}
   (when (some? parent-title)
     [:div
      {:class        (<class $sidebar-back)
       :tabindex     0
       :on-key-press (on-enter-space #(navigate parent-link))
       :on-click     (on-click #(navigate parent-link))}
      (str "< " parent-title)])
   [:div
    {:style {:text-align "center" :font-weight "300" :margin "1rem"}}
    title]])

(defn- sidebar-options [options]
  [:div.table-view
   {:display        "flex"
    :flex-direction "column"
    :overflow-y     "scroll"}
   (map (fn [{:keys [label link]}]
          ^{:key label}
          [:a {:class        (<class $option)
               :tabindex     0
               :on-key-press (on-enter-space #(navigate link))
               :on-click     (on-click #(navigate link))}
           label])
        options)])

;;; Public

(defn ->sidebar-links
  "Creates sidebar links for entities. Takes:
   - options [seq<map>]:  Sequence of entity maps. Must have `:db/id` attribute.
   - label-attr [keyword]: Attribute of map that will serve as the label
   - route [keyword]: Route that will be created with `(bidi/path-for app-routes <route> :id (:db/id option))`"
  [options label-attr route]
  (->> options
       (map (fn [option]
              {:label (get option label-attr)
               :link  (path-for app-routes route :id (:db/id option))}))
       (sort-by :label)))

(defn sidebar
  "Sidebar component. Takes:
   - title [string]: Title to display
   - options [seq<map>]: Sidebar links to display. Must include `:label` and `:link` attributes.
   - parent-title [string?]: Title for link to parent
   - parent-link [string?]: Link to parent
   - alt-title [string?]: Alternative title to display
   - alt-options [seq<map>?]: Alternative sidebar links to display. Must include `:label` and `:link` attributes.
  "
  [title
   options
   & [parent-title parent-link alt-title alt-options]]
  [:div.sidebar
   [:div {:class (<class $sidebar)}
    [sidebar-header title parent-title parent-link]
    [sidebar-options options]
    (when (and alt-title alt-options)
      [:<>
       [sidebar-header alt-title]
       [sidebar-options alt-options]])]])
