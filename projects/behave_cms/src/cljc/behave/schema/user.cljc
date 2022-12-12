(ns behave.schema.user
  (:require [clojure.spec.alpha :as s]))

;;; Validation Fns

(defn email? [s]
  (re-find #"(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,6}$" s))

(defn contains-num? [s]
  (re-find #"[0-9]" s))

(defn contains-special-char? [s]
  (re-find #"[!@#$%^&*?]" s))

;;; Spec

(s/def :user/id uuid?)
(s/def :user/first-name string?)
(s/def :user/last-name string?)
(s/def :user/email (s/and string? email?))
(s/def :user/password (s/and string?
                             #(< 8 (count %))
                             contains-num?
                             contains-special-char?))

(s/def :behave/user (s/keys :req [:user/id :user/first-name :user/last-name :user/email :user/password]))

;;; Schema

(def schema
  [{:db/ident       :user/id
    :db/doc         "User's ID."
    :db/valueType   :db.type/uuid
    :db/unique      :db.unique/identity
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/first-name
    :db/doc         "User's first name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/last-name
    :db/doc         "User's last name."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/first+last-name
    :db/doc         "User's first and last name."
    :db/valueType   :db.type/tuple
    :db/tupleAttrs  [:user/first-name :user/last-name]
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
  (s/valid? :behave/user {:user/id #uuid "5eb87bf7-9501-4d50-9eee-7d0ffadbb1d0"
                          :user/first-name "RJ"
                          :user/last-name "Sheperd"
                          :user/email "rsheperd@sig-gis.com"
                          :user/password "happydays9!"})
)

