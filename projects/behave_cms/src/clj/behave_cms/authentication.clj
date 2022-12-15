(ns behave-cms.authentication
  (:require [datahike.api          :as d]
            [behave-cms.store      :as s :refer [default-conn
                                                 get-entity
                                                 create-entity!
                                                 update-entity!]]
            [behave-cms.utils.mail :refer [get-site-url email? send-mail]]
            [behave-cms.views      :refer [data-response]]
            [datom-utils.interface :refer [safe-deref]])
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
  [db email]
  (and (email? email)
       (some? (ffirst (d/q '[:find ?e ?email
                             :in $ ?email
                             :where [?e :user/email ?email]]
                           (safe-deref db) email)))))

(defn- is-super-admin? [db email]
  (some? (ffirst (d/q '[:find ?admin
                        :in $ ?email
                        :where [?e :user/email ?email]
                               [?e :user/super-admin? ?admin]]
                      (safe-deref db) email))))

(defn- get-user-id [db email]
  (let [user-id (d/q '[:find ?e
                       :in $ ?email
                       :where [?e :user/email ?email]]
                     (safe-deref db) email)]
    (when-not (empty? user-id)
      (ffirst user-id))))

(defn- get-user [db email]
  (when-let [user-id (get-user-id db email)]
    (get-entity db {:db/id user-id})))

(defn- create-user! [db email first-name last-name]
  (create-entity! db {:user/uuid         (str (random-uuid))
                      :user/email        email
                      :user/first-name   first-name
                      :user/last-name    last-name
                      :user/verified?    false
                      :user/super-admin? false
                      :user/reset-key    (rand-string 32)}))

(defn- valid-password? [db email password]
  (let [{pass-hash :user/password} (get-user db email)]
    (match-password? password pass-hash)))

(defn- update-email! [db old-email new-email]
  (when-let [id (get-user-id @db old-email)]
    (update-entity! db {:db/id id :user/email new-email})))

(defn- update-password! [db email new-password reset-key]
  (when-let [user (get-user db email)]
    (when (= reset-key (:user/reset-key user))
      (update-entity! db {:db/id         (:db/id user)
                          :user/password (crypt-password new-password)}))))

(defn- update-reset-key! [db email]
  (when-let [id (get-user-id db email)]
    (update-entity! db {:db/id id
                        :user/reset-key (rand-string 32)})))

(defn- update-verify! [db email reset-key]
  (when-let [user (get-user db email)]
    (when (= reset-key (:user/reset-key user))
      (update-entity! db {:db/id (:db/id user) :user/verified? true}))))

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
  (let [db (default-conn)]
    (if (valid-password? db email password)
      (let [user (get-user db email)]
        (data-response (only-public-cols user) {:session {:user-uuid (str (:user/id user))}}))
      (unathorized "Invalid email/password."))))

(defn logout! [] (data-response "" {:session nil}))

(defn set-email! [{:keys [old-email new-email]}]
  (let [db (default-conn)]
    (update-email! db old-email new-email)
    (data-response (only-public-cols (get-user db new-email)))
    (unathorized "Invalid email/password.")))

(defn reset-password! [{:keys [email password reset-key]}]
  (if (update-password! (default-conn) email password reset-key)
    (data-response {:message "Updated password."})
    (unathorized "Invalid email/password.")))

(defn reset-key! [{:keys [email]}]
  (let [db (default-conn)]
    (if (update-reset-key! db email)
      (let [user (get-user db email)]
        (send-reset-email! (:user/first-name user) email (:user/reset-key user))
        (success (only-public-cols user)))
      (unathorized ""))))

(defn verify-email! [{:keys [email reset-key]}]
  (let [db (default-conn)]
    (if (update-verify! db email reset-key)
      (success (only-public-cols (get-user db email)))
      (unathorized ""))))

(defn invite-user! [{:keys [email first-name last-name]}]
  (let [db (default-conn)]
    (if (email-taken? db email)
      (unathorized (format "Email '%s' has been taken." email))
      (let [_    (create-user! db email first-name last-name)
            user (get-user db email)]
        (send-invite-email! (:user/first-name user)
                            email
                            (:user/created user)
                            (:user/reset-key user))
        (success (only-public-cols user))))))
