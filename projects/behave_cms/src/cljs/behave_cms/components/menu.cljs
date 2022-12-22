(ns behave-cms.components.menu)

(defn- nav-link [label path navigate!]
  [:li.nav-item
   [:a.nav-link
    {:on-click #(navigate! path)
     :style    {:color  "white"
                :cursor "pointer"}}
    label]])

(defn menu
  [pages navigate!]
  [:div.navbar {:style {:height "50px" :padding "0px" :background-color "#3f3f3f"}}
   [:ul.nav
    [nav-link "FireLab VMS" "/" navigate!]
    (for [{:keys [page path]} pages]
      ^{:key path}
      [nav-link page path navigate!])]])
