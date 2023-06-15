(ns behave-cms.remote-api
  (:require [clojure.data.json :as json]
            [clojure.repl      :refer [demunge]]
            [clojure.string    :as str]
            [bidi.bidi         :refer [match-route]]
            [config.interface  :refer [get-config]]
            [behave-cms.authentication  :refer [invite-user!
                                                login!
                                                logout!
                                                set-email!
                                                reset-password!
                                                reset-key!
                                                verify-email!]]
            [behave-cms.routes          :refer [api-routes]]
            [behave-cms.views           :refer [data-response]]
            [behave-cms.export          :refer [sync-images]]))

(defn log-str [& args]
  (when (= "dev" (get-config :server :mode))
    (apply println args)))

(def name->fn {"sync-images" sync-images})

(def api-handlers {; Authorization
                   :api/login                  login!
                   :api/logout                 logout!
                   :api/invite-user            invite-user!
                   :api/reset-password         reset-password!
                   :api/set-email              set-email!
                   :api/reset-key              reset-key!
                   :api/verify-email           verify-email!})

(defn- fn->sym [f]
  (-> (str f)
      (demunge)
      (str/split #"@")
      (first)
      (symbol)))

;; Handlers
(defn api-handler [{:keys [uri params content-type]}]
  (log-str "--- URI:" uri " Params: " params " Content Type: " content-type)
  (if-let [{:keys [handler route-params]} (match-route api-routes uri)]
    (let [function   (get api-handlers handler)
          _          (log-str "API Route: " handler ", Route Params: " route-params)
          api-args   (if (= content-type "application/edn")
                       (:api-args params {})
                       (json/read-str (:api-args params "{}")))
          fn-args    (if (and (nil? route-params) (nil? api-args))
                       nil
                       [(merge route-params api-args)])
          _          (log-str "API Call: " (fn->sym function) fn-args)
          api-result (apply function fn-args)
          response   (if (and (map? api-result) (:status api-result))
                       api-result
                       (data-response api-result {:type (if (= content-type "application/edn") :edn :json)}))]
      (log-str response)
      response)
    (data-response "There is no valid handler with this name." {:status 400})))

(defn clj-handler [{:keys [uri params content-type]}]
  (if-let [function (->> (str/split uri #"/")
                         (remove str/blank?)
                         (second)
                         (name->fn))]
    (let [clj-args   (if (= content-type "application/edn")
                       (:clj-args params [])
                       (json/read-str (:clj-args params "[]")))
          clj-result (apply function clj-args)]
      (log-str "CLJ Call: " (cons (fn->sym function) clj-args))
      (if (:status clj-result)
        clj-result
        (data-response clj-result {:type (if (= content-type "application/edn") :edn :json)})))
    (data-response "There is no valid function with this name." {:status 400})))
