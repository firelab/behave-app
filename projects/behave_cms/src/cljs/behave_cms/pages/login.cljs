(ns behave-cms.pages.login
  (:require [reagent.core                 :as r]
            [re-frame.core                :as rf]
            [behave-cms.styles            :as $]
            [behave-cms.components.common :refer [simple-form]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; UI Components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- reset-link [forgot?]
  [:a {:style    ($/align :block :left)
       :href     "#"
       :on-click #(reset! forgot? true)}
   "Forgot Password?"])

(defn root-component
  "The root component for the /login page.
  Displays either the login form or request new password form and a link to the register page."
  [{:keys [redirect]}]
  (let [forgot?  (r/atom false)
        email    (r/atom "")
        password (r/atom "")]
    [:div.container
     [:div.row {:style {:display "flex" :justify-content "center" :margin "5rem"}}
      [:div.col-6
       (if @forgot?
         [simple-form
          "Request New Password"
          "Submit"
          [["Email" email "email" "email"]]
          #(rf/dispatch [:auth/reset-password @email])]
         [simple-form
          "Log in"
          "Log in"
          [["Email"    email    "email"    "email"]
           ["Password" password "password" "current-password"]]
          #(rf/dispatch [:auth/login @email @password])
          reset-link])]]]))
