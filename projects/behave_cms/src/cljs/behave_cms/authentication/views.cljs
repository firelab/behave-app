(ns behave-cms.authentication.views
  (:require [reagent.core                 :as r]
            [re-frame.core                :as rf]
            [behave-cms.styles            :as $]
            [behave-cms.components.common :refer [simple-form]]
            [behave-cms.utils             :as u]))

;;; Helpers

(defn- reset-link [forgot?]
  [:a {:style    ($/align :block :left)
       :href     "#"
       :on-click #(reset! forgot? true)}
   "Forgot Password?"])

(defn- verify-message [loading? verify-success? verify-error?]
  (cond
    loading?        "Verification in Progress... Please wait."
    verify-success? "Verification Complete. Redirecting you to the dashboard."
    verify-error?   "Unable to verify your email. Please check your email for the verification link."))

(defn valid-password? [pass re-pass]
  (cond
    (< (count pass) 8)
    "Password must be at least 8 characters."

    (not (re-find #"[\!\@\#\$\%\^\&\*\?]" pass))
    "Password must contain a special character (!?@#$%^&*)."

    (not (re-find #"\d" pass))
    "Password must contain a number (0-9)."

    (not= pass re-pass)
    "Passwords do not match."))

;;; Public Views

(defn reset-password-page
  "The root component for the /set-password page.
  If the user's token is , then redirects them to the home page."
  [{:keys [email reset-key]}]
  (r/with-let [password        (r/atom nil)
               re-password     (r/atom nil)
               error           (r/atom nil)
               reset-password! (fn []
                                 (println "--- Checking passwords" @password @re-password (valid-password? @password @re-password))
                                 (reset! error (valid-password? @password @re-password))
                                 (when (nil? @error)
                                   (rf/dispatch [:auth/reset-password email reset-key @password])))]

    [:div.container
     [:div.row {:style {:display "flex" :justify-content "center" :margin "5rem"}}
      [:div.col-6
       [:form
        {:on-submit #(do (.preventDefault %) (.stopPropagation %) (reset-password!))}
        [:input {:type "hidden" :value email}]
        [:input {:type "hidden" :value reset-key}]
        [:div.mb-3
         [:label.form-label
          "Password"
          [:input.form-control
           {:type      "password"
            :name      "password"
            :required  true
            :on-change #(reset! password (u/input-value %))
            :value     @password}]]]
        [:div.mb-3
         [:label.form-label
          "Confirm Password"
          [:input.form-control
           {:type      "password"
            :name      "re-password"
            :required  true
            :on-change #(reset! re-password (u/input-value %))
            :value     @re-password}]]]
        (when @error
          [:div.invalid-feedback {:style {:display "block"}} @error])
        [:button.btn.btn-primary {:type "submit"} "Reset Password"]]]]]))

(defn verify-email-page
  "The root component for the /verify-email page.
  Displays whether the user has been properly verified, then redirects them to the home page."
  [{:keys [email token]}]
  (rf/dispatch [:auth/verify-email email token])
  (r/with-let [loading?        (rf/subscribe [:state :loading?])
               verify-success? (rf/subscribe [:state :verify-success?])
               verify-error?   (rf/subscribe [:state :verify-error?])]
    [:div.container
     [:div.row {:style {:display "flex" :justify-content "center" :margin "5rem"}}
      [:div.col-6
       [:h3 (verify-message @loading? @verify-success? @verify-error?)]]]]))

(defn invite-user-page
  "The root component for the /invite-user page."
  []
  (let [name            (r/atom nil)
        email           (r/atom nil)
        loading?        (rf/subscribe [:state :loading?])
        invite-success? (rf/subscribe [:state :invite-success?])
        invite-error?   (rf/subscribe [:state :invite-error?])]
    [:div.container
     [:div.row {:style {:display "flex" :justify-content "center" :margin "5rem"}}
      [:div.col-6
       (if (or @loading? @invite-success? @invite-error?)
         [:h3
          (cond
            @loading?        "Invitation in progress..."
            @invite-error?   "Unable to send invitation. Please try again later."
            @invite-success? "Invitation sent!")]
         [simple-form
          "Invite User"
          "Submit"
          [["Name" name "text" "name"]
           ["Email" email "email" "email"]]
          #(rf/dispatch [:auth/invite-user @name @email])])]]]))

(defn login-page
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
          #(rf/dispatch [:auth/login @email @password redirect])
          reset-link])]]]))
