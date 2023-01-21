(ns behave-cms.routes
  (:require [bidi.bidi :as bidi]
            [clojure.walk :as walk]))

(def admin-routes  #{:users :invite-user})

(def public-routes #{:login
                     :dashboard
                     :verify-email
                     :reset-password
                     :api/login
                     :api/verify-email
                     :api/reset-password})

(def singular       {:applications    :application
                     :classes         :class
                     :enum-members    :enum-member
                     :enums           :enum
                     :functions       :function
                     :groups          :group
                     :group-variables :group-variable
                     :help-pages      :help-page
                     :languages       :language
                     :modules         :module
                     :namespaces      :namespace
                     :parameters      :parameter
                     :permissions     :permission
                     :roles           :role
                     :subgroups       :subgroup
                     :submodules      :submodule
                     :translations    :translation
                     :users           :user
                     :variables       :variable})

;; From https://github.com/WorksHub/client/blob/master/common/src/wh/routes.cljc
;; The collection routes defined here are supposed to have trailing
;; slashes. If a URL without the trailing slash is requested,
;; there will be a server-side redirect to the correct one.
(defn- add-trailing-slashes-to-roots
  [routes]
  (walk/postwalk
    (fn [x]
      (if (and (vector? x)
               (some-> x second map?)
               (some-> x second (get "")))
        (update x 1 #(let [root (get % "")]
                       (assoc % "/" root)))
        x))
    routes))

(defn- ->kw [& s]
  (keyword (apply str s)))

(defn- entity-route [entity]
  (let [entity-str     (-> entity (singular) (name))
        entities-route (name entity)
        get-entity     (->kw "get-" entity-str)]
    [entities-route [["" (->kw entities-route)]
                     [["/" [long :id]] get-entity]]]))

(def api-routes
  (add-trailing-slashes-to-roots
    ["/api"
     [["" :api-index]
      ["/" [;; System API Methods
            ["login"          :api/login]
            ["verify-email"   :api/verify-email]
            ["set-password"   :api/set-password]
            ["reset-password" :api/reset-password]
            ["invite-user"    :api/invite-user]

            ;; Custom API routes
            ["variables/search" :api/search-variables]]]]]))

(def app-routes
  (add-trailing-slashes-to-roots
    ["/"
     [;; App Routes
      ["" :dashboard]

      ;; Users/Auth
      (entity-route :users)
      (entity-route :roles)
      (entity-route :permissions)

      ;; I18n
      (entity-route :languages)
      (entity-route :translations)

      ;; App Entities
      (entity-route :applications)
      (entity-route :modules)
      (entity-route :submodules)
      (entity-route :groups)
      (entity-route :subgroups)
      (entity-route :group-variables)
      (entity-route :variables)
      (entity-route :help-pages)

      ;; CPP
      (entity-route :namespaces)
      (entity-route :classes)
      (entity-route :enums)
      (entity-route :enum-members)
      (entity-route :functions)
      (entity-route :parameters)

      ;; System Routes
      ["login"          :login]
      ["verify-email"   :verify-email]
      ["set-password"   :set-password]
      ["reset-password" :reset-password]
      ["invite-user"    :invite-user]]]))
