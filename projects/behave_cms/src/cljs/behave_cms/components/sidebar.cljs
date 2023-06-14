(ns behave-cms.components.sidebar
  (:require [herb.core     :refer [<class]]
            [re-frame.core :as rf]))

(def sidebar-width "200px")
(def $colors {:gray "rgb(215, 215, 215)"
              :blue "rgb(0, 122, 255)"
              :light-blue "rgba(0, 122, 255, 0.5)"})

(defn $c [color]
  (color $colors))

(defn $option []
  ^{:pseudo {:hover {:background-color ($c :light-blue)
                     :color            "white"
                     :cursor           "pointer"}}}
  {:border-bottom    (str "1px solid " ($c :gray))
   :padding          "8px"})

(defn $sidebar []
  {:position "fixed"
   :top "50px"
   :bottom "0px"
   :width sidebar-width
   :border-right (str "1px solid " ($c :gray))
   :background-color "white"})

(defn $sidebar-back []
  ^{:pseudo {:hover {:text-decoration  "underline"
                     :cursor           "pointer"}}}
  {:font-weight      "bold"
   :font-size        "0.8rem"})

(defn sidebar-header [title parent-title parent-link]
  [:div
   {:style {:display       "flex"
            :flex-direction "column"
            :background    "rgb(245, 245, 245)"
            :padding       "10px"}}
   (when (some? parent-title)
     [:div
      {:class    (<class $sidebar-back)
       :on-click #(if parent-link
                     (rf/dispatch [:navigate parent-link])
                     (js/window.history.back))}
      (str "< " parent-title)])
   [:div
    {:style {:text-align "center" :font-weight "300" :margin "1rem"}}
    title]])

(defn sidebar-options [options]
  [:div.table-view {:display "flex" :flex-direction "column"}
   (for [{:keys [label link]} options]
     ^{:key label}
     [:div {:class (<class $option)
            :on-click #(rf/dispatch [:navigate link])}
      label])])

(defn sidebar [title
               options
               & [parent-title parent-link]]
  [:div.sidebar
   [:div {:class (<class $sidebar)}
    [sidebar-header title parent-title parent-link]
    [sidebar-options options]]])

