(ns behave-cms.components.sidebar
  (:require [herb.core     :refer [<class]]
            [re-frame.core :as rf]))

;;; Styles

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
  {:display          "block"
   :border-bottom    (str "1px solid " ($c :gray))
   :padding          "8px"
   :text-decoration  "none"
   :color            "inherit"})

(defn $sidebar []
  {:position         "fixed"
   :top              "50px"
   :bottom           "0px"
   :width            sidebar-width
   :overflow-x       "hidden"
   :border-right     (str "1px solid " ($c :gray))
   :background-color "white"})

(defn $sidebar-back []
  ^{:pseudo {:hover {:text-decoration  "underline"
                     :cursor           "pointer"}}}
  {:font-weight      "bold"
   :font-size        "0.8rem"})

;;; Helpers

(defn navigate [path]
  (rf/dispatch [:navigate path]))

(defn on-click [fn]
  #(do (.preventDefault %) (fn)))

(defn on-enter-space [fn]
  #(when (#{13 32} (.-charCode %)) (fn)))

;;; Components

(defn sidebar-header [title parent-title parent-link]
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

(defn sidebar-options [options]
  [:div.table-view
   {:display        "flex"
    :flex-direction "column"
    :overflow-y     "scroll"}
   (for [{:keys [label link]} options]
     ^{:key label}
     [:a {:class        (<class $option)
          :tabindex     0
          :on-key-press (on-enter-space #(navigate link))
          :on-click     (on-click #(navigate link))}
      label])])

(defn sidebar [title
               options
               & [parent-title parent-link subgroups]]
  [:div.sidebar
   [:div {:class (<class $sidebar)}
    [sidebar-header title parent-title parent-link]
    [sidebar-options options]
    (when subgroups
      [:<>
       [sidebar-header "Subgroups"]
       [sidebar-options subgroups]])]])

