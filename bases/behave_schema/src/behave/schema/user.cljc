(ns behave.schema.user
  (:require [clojure.spec.alpha :as s]
            [behave.schema.utils :refer [uuid-string?]]))

;;; Validation Fns

(defn email? [s]
  (re-find #"(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}$" s))

(defn contains-num? [s]
  (re-find #"[0-9]" s))

(defn contains-special-char? [s]
  (re-find #"[!@#$%^&*?]" s))

;;; Spec

(s/def :user/uuid       uuid-string?)
(s/def :user/first-name string?)
(s/def :user/last-name  string?)
(s/def :user/email      (s/and string? email?))
(s/def :user/password   (s/and string?
                               #(< 8 (count %))
                               contains-num?
                               contains-special-char?))

(s/def :behave/user (s/keys :req [:user/uuid
                                  :user/first-name
                                  :user/last-name
                                  :user/email
                                  :user/password]))

;;; Schema

(def schema

  [{:db/ident       :user/uuid
    :db/doc         "User's UUID."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :user/name
    :db/doc         "User's name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :user/email
    :db/doc         "User's email address."
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident       :user/password
    :db/doc         "User's password."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :user/reset-key
    :db/doc         "User's reset-key."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident       :user/verified?
    :db/doc         "User's verified status"
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident       :user/super-admin?
    :db/doc         "User's super admin status"
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one}])

;;; Testing

(comment
  (s/valid? :behave/user {:user/uuid       (str (random-uuid))
                          :user/first-name "Smokey"
                          :user/last-name  "Bear"
                          :user/email      "smokey@sig-gis.com"
                          :user/password   "burnbabyburn9!"}))
