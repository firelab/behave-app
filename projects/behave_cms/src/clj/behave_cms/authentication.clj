(ns behave-cms.authentication
  (:require [datomic-store.main    :as s]
            [behave-cms.utils.mail :refer [get-site-url email? send-mail]]
            [behave-cms.views      :refer [data-response]])
  (:import [org.mindrot.jbcrypt BCrypt]
           [java.util Random]
           [java.net URLEncoder]))

;;; Password hashing

(defn generate-salt
  []
  (BCrypt/gensalt))

(defn crypt-password
  [password]
  (BCrypt/hashpw password (generate-salt)))

(defn match-password?
  [plaintext hashed]
  (BCrypt/checkpw plaintext hashed))

;;; Constants

(def users-table :users)

;;; Helpers

(defn default-conn []
  (when @s/datomic-conn
    @s/datomic-conn))

;; https://stackoverflow.com/questions/64034761/fast-random-string-generator-in-clojure
(defn- rand-string
  ^String [^Long len]
  (let [leftLimit 97
        rightLimit 122
        random (Random.)
        stringBuilder (StringBuilder. len)
        diff (- rightLimit leftLimit)]
    (dotimes [_ len]
      (let [ch (char (.intValue ^Double (+ leftLimit (* (.nextFloat ^Random random) (+ diff 1)))))]
        (.append ^StringBuilder stringBuilder ch)))
    (.toString ^StringBuilder stringBuilder)))

(defn- only-public-cols [user]
  (-> user
      (select-keys #{:uuid :email :name})
      (assoc :uuid (-> user (:uuid) str))))

(defn- email-taken?
  [conn email]
  (and (email? email)
       (some? (s/q '[:find ?e .
                     :in $ ?email
                     :where [?e :user/email ?email]]
                   conn email))))

(defn- is-super-admin? [conn email]
  (some? (s/q '[:find ?admin .
                :in $ ?email
                :where [?e :user/email ?email]
                [?e :user/super-admin? ?admin]]
              conn email)))

(defn- get-user-id [conn email]
  (s/q '[:find ?e .
         :in $ ?email
         :where [?e :user/email ?email]]
       conn email))

(defn- get-user [conn email]
  (when-let [user-id (get-user-id conn email)]
    (s/entity conn {:db/id user-id})))

(defn- create-user! [conn email name]
  (s/create! conn {:user/uuid         (str (random-uuid))
                   :user/email        email
                   :user/name         name
                   :user/verified?    false
                   :user/super-admin? false
                   :user/reset-key    (rand-string 32)}))

(defn- valid-password? [conn email password]
  (let [{pass-hash :user/password} (get-user conn email)]
    (match-password? password pass-hash)))

(defn- update-email! [conn old-email new-email]
  (when-let [id (get-user-id conn old-email)]
    (s/update! conn {:db/id id :user/email new-email})))

(defn- update-password! [db email new-password reset-key]
  (when-let [user (get-user db email)]
    (when (= reset-key (:user/reset-key user))
      (s/update! db {:db/id         (:db/id user)
                     :user/password (crypt-password new-password)}))))

(defn- update-reset-key! [db email]
  (when-let [id (get-user-id db email)]
    (s/update! db {:db/id          id
                   :user/reset-key (rand-string 32)})))

(defn- update-verify! [db email reset-key]
  (when-let [user (get-user db email)]
    (when (= reset-key (:user/reset-key user))
      (s/update! db {:db/id (:db/id user) :user/verified? true}))))

;;; Mailers

(defn- send-invite-email! [name email timestamp reset-key]
  (let [email-msg (format (str "Dear %s,\n\n"
                               "You have been invited to collaborate on a FireLab project.\n\n"
                               "Your Account Summary Details:\n\n"
                               "  Email: %s\n"
                               "  Created on: %s\n\n"
                               "  Click the following link to set your password:\n"
                               "  %s/reset-password?email=%s&reset-key=%s\n\n"
                               "Kind Regards,\n"
                               "  The FireLab Team")
                          name email timestamp (get-site-url) (URLEncoder/encode email) reset-key)]
    (send-mail email nil nil "[FireLab] Welcome to the FireLab!" email-msg "text/plain")))

(defn send-reset-email! [name email reset-key]
  (let [email-msg (format (str "Hi %s,\n\n"
                               "  If you did not request a password reset, please ignore this message.\n"
                               "  To reset your password, simply click the following link:\n\n"
                               "  %s/reset-password?email=%s&reset-key=%s")
                          name (get-site-url) email reset-key)]
    (send-mail email nil nil "[FireLab] Password reset request" email-msg "text/plain")))

(defn- success [data & [status]]
  (data-response data {:status (or status 200) :type :edn}))

(defn- unathorized [message]
  (data-response {:message message} {:status 403 :type :edn}))

;;; Public Fns

(defn login! [{:keys [email password]}]
  (let [conn (default-conn)]
    (if (valid-password? conn email password)
      (let [user (get-user conn email)]
        (data-response (only-public-cols user) {:session {:user-uuid (str (:bp/uuid user))}}))
      (unathorized "Invalid email/password."))))

(defn logout! [] (data-response "" {:session nil}))

(defn set-email! [{:keys [old-email new-email]}]
  (let [conn (default-conn)]
    (update-email! conn old-email new-email)
    (data-response (only-public-cols (get-user conn new-email)))
    (unathorized "Invalid email/password.")))

(defn reset-password! [{:keys [email password reset-key]}]
  (if (update-password! (default-conn) email password reset-key)
    (data-response {:message "Updated password."})
    (unathorized "Invalid email/password.")))

(defn reset-key! [{:keys [email]}]
  (let [conn (default-conn)]
    (if (update-reset-key! conn email)
      (let [user (get-user conn email)]
        (send-reset-email! (:user/name user) email (:user/reset-key user))
        (success (only-public-cols user)))
      (unathorized ""))))

(defn verify-email! [{:keys [email reset-key]}]
  (let [conn (default-conn)]
    (if (update-verify! conn email reset-key)
      (success (only-public-cols (get-user conn email)))
      (unathorized ""))))

(defn invite-user! [{:keys [email name]}]
  (let [conn (default-conn)]
    (if (email-taken? conn email)
      (unathorized (format "Email '%s' has been taken." email))
      (let [_    (create-user! conn email name)
            user (get-user conn email)]
        (send-invite-email! (:user/name user)
                            email
                            (:user/created user)
                            (:user/reset-key user))
        (success (only-public-cols user))))))
