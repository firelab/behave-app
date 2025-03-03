(ns behave-cms.utils.mail
  (:require [postal.core :refer [send-message]]
            [config.interface   :refer [get-config]]
            [logging.interface :refer [log-str]]))

(defn get-site-url []
  (:site-url (get-config :mail)))

(defn email? [string]
  (let [pattern #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"]
    (and (string? string) (re-matches pattern string))))

(defn- send-postal [to-addresses cc-addresses bcc-addresses subject body content-type]
  (send-message
   (select-keys (get-config :mail) [:host :user :pass :tls :port])
   {:from    (get-config :mail :user)
    :to      to-addresses
    :cc      cc-addresses
    :bcc     bcc-addresses
    :subject subject
    :body    [{:type    (or content-type "text/plain")
               :content body}]}))

(defn send-mail [to-addresses cc-addresses bcc-addresses subject body content-type]
  (let [{:keys [message error]} (send-postal to-addresses
                                             cc-addresses
                                             bcc-addresses
                                             subject
                                             body
                                             content-type)]
    (when-not (= :SUCCESS error) (log-str message))))
