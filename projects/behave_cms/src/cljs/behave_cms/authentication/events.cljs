(ns behave-cms.authentication.events
  (:require [bidi.bidi         :refer [path-for]]
            [re-frame.core     :as rf]
            [behave-cms.routes :refer [api-routes]]))

;;; Login

(rf/reg-event-fx
  :auth/login
  (fn [cofx [_ email password redirect-to]]
    {:db (assoc-in (:db cofx) [:state :loading?] true)
     :api/request {:method     :post
                   :uri        (path-for api-routes :api/login)
                   :data       {:email email :password password}
                   :fn-args    [(or redirect-to "/")]
                   :on-success :auth/login-success
                   :on-error   :auth/login-error}}))

(rf/reg-event-fx
  :auth/login-success
  (fn [cofx [_ {body :body} [redirect-to]]]
    {:db (-> (:db cofx)
             (assoc :loading? false)
             (assoc :user body))
     :fx [[:dispatch [:refresh (or redirect-to "/applications")]]]}))

(rf/reg-event-db
  :auth/login-error
  (rf/path :state)
  (fn [db _]
    (-> db
        (assoc :loading? false)
        (assoc :login-error "Error: Unable to authenticate. Please try again."))))

;;; Logout

(rf/reg-event-fx
  :auth/logout
  (fn [cofx _]
    {:db (assoc-in (:db cofx) [:state :user] nil)
     :fx [[:dispatch [:navigate "/login"]]]
     :api/request {:method     :post
                   :uri        (path-for api-routes :api/logout)}}))

;;; Verify Email

(rf/reg-event-fx
  :auth/verify-email
  (fn [{db :db} [_ email reset-key]]
    {:db          (assoc-in db [:state :loading?] true)
     :api/request {:method     :post
                   :uri        (path-for api-routes :api/verify-email)
                   :data       {:email email :reset-key reset-key}
                   :on-success :auth/verify-success
                   :on-error   :auth/verify-error}}))

(rf/reg-event-fx
  :auth/verify-success
  (fn [{db :db} _]
    {:db (-> db
             (assoc-in [:state :loading?] false)
             (assoc-in [:state :verify-success?] true))
     :fx [[:dispatch [:navigate "/login"]]]}))

(rf/reg-event-db
  :auth/verify-error
  (rf/path :state)
  (fn [db _]
     (assoc db :loading? false :verify-error? true)))


;;; Invite User

(rf/reg-event-fx
  :auth/invite-user
  (fn [{db :db} [_ name email]]
    {:db          (assoc-in db [:state :loading?] true)
     :api/request {:method     :post
                   :uri        (path-for api-routes :api/invite-user)
                   :data       {:name name :email email}
                   :on-success :auth/invite-success
                   :on-error   :auth/invite-error}}))

(rf/reg-event-db
  :auth/invite-success
  (rf/path :state)
  (fn [db _]
    (assoc db :loading? false :invite-success? true)))

(rf/reg-event-db
  :auth/invite-error
  (rf/path :state)
  (fn [db _]
     (assoc db :loading? false :invite-error? true)))

;;; Reset Password
(rf/reg-event-fx
  :auth/reset-password
  (fn [{db :db} [_ email reset-key password]]
    {:db          (assoc-in db [:state :loading?] true)
     :api/request {:method     :post
                   :uri        (path-for api-routes :api/reset-password)
                   :data       {:email     email
                                :reset-key reset-key
                                :password  password}
                   :on-success :auth/reset-success
                   :on-error   :auth/reset-error}}))

(rf/reg-event-fx
  :auth/reset-success
  (fn [{db :db} _]
    {:db (-> db
             (assoc-in [:state :loading?] false)
             (assoc-in [:state :reset-success?] true))
     :fx [[:dispatch [:navigate "/login"]]]}))

(rf/reg-event-db
  :auth/reset-error
  (rf/path :state)
  (fn [db _]
     (assoc db :loading? false :reset-error? true)))
