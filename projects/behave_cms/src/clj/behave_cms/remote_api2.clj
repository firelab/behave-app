(ns behave-cms.remote-api
  (:require [clojure.data.json          :as json]
            [clojure.repl               :refer [demunge]]
            [clojure.string             :as str]
            [bidi.bidi                  :refer [match-route]]
            [behave-cms.db.export       :refer [export-all
                                                export-table
                                                export-table-triples
                                                export-all-triples]]
            [behave-cms.authentication  :refer [invite-user!
                                                login!
                                                logout!
                                                set-email!
                                                reset-password!
                                                reset-key!
                                                verify-email!]]
            [behave-cms.db.applications :refer [list-applications
                                                get-application
                                                create-application!
                                                update-application!
                                                delete-application!]]
            [behave-cms.db.cpp          :refer [list-classes
                                                get-class
                                                create-class!
                                                update-class!
                                                delete-class!
                                                list-enums
                                                get-enum
                                                create-enum!
                                                update-enum!
                                                delete-enum!
                                                list-enum-members
                                                get-enum-member
                                                create-enum-member!
                                                update-enum-member!
                                                delete-enum-member!
                                                list-functions
                                                get-function
                                                create-function!
                                                update-function!
                                                delete-function!
                                                list-namespaces
                                                get-namespace
                                                create-namespace!
                                                update-namespace!
                                                delete-namespace!
                                                list-parameters
                                                get-parameter
                                                create-parameter!
                                                update-parameter!
                                                delete-parameter!]]
            [behave-cms.db.help         :refer [list-help-pages
                                                get-help-page
                                                create-help-page!
                                                update-help-page!
                                                delete-help-page!]]
            [behave-cms.db.groups       :refer [list-groups
                                                get-group
                                                add-group!
                                                update-group!
                                                delete-group!
                                                list-subgroups
                                                add-subgroup!
                                                list-group-variables
                                                get-group-variable
                                                add-group-variable!
                                                update-group-variable!
                                                delete-group-variable!
                                                ]]
            [behave-cms.db.languages    :refer [list-languages
                                                get-language
                                                create-language!
                                                update-language!
                                                delete-language!]]
            [behave-cms.db.modules      :refer [list-modules
                                                get-module
                                                add-module!
                                                update-module!
                                                delete-module!]]
            [behave-cms.db.permissions  :refer [list-permissions
                                                get-permission
                                                create-permission!
                                                update-permission!
                                                delete-permission!]]
            [behave-cms.db.roles        :refer [list-roles
                                                get-role
                                                create-role!
                                                update-role!
                                                delete-role!
                                                list-role-permissions
                                                add-role-permission!
                                                delete-role-permission!]]
            [behave-cms.db.submodules   :refer [list-submodules
                                                get-submodule
                                                add-submodule!
                                                update-submodule!
                                                delete-submodule!]]
            [behave-cms.db.translations :refer [list-translations
                                                get-translation
                                                create-translation!
                                                update-translation!
                                                delete-translation!]]
            [behave-cms.db.users        :refer [list-users
                                                get-user
                                                create-user!
                                                update-user!
                                                delete-user!
                                                list-user-roles
                                                add-user-role!
                                                delete-user-role!]]
            [behave-cms.db.variables    :refer [list-variables
                                                search-variables
                                                get-variable
                                                create-variable!
                                                update-variable!
                                                delete-variable!]]
            [behave-cms.routes          :refer [api-routes]]
            [behave-cms.views           :refer [data-response]]))

(def log-str println)

(def name->fn {"export-all"           export-all
               "export-all-triples"   export-all-triples
               "export-table"         export-table
               "export-table-triples" export-table-triples})

(def api-handlers {; Authorization
                   :api/login                  login!
                   :api/logout                 logout!
                   :api/invite-user            invite-user!
                   :api/reset-password         reset-password!
                   :api/set-email              set-email!
                   :api/reset-key              reset-key!
                   :api/verify-email           verify-email!

                   ; Applications
                   :api/list-applications      list-applications
                   :api/get-application        get-application
                   :api/create-application     create-application!
                   :api/update-application     update-application!
                   :api/delete-application     delete-application!

                   ; CPP Classes
                   :api/list-classes           list-classes
                   :api/get-class              get-class
                   :api/create-class           create-class!
                   :api/update-class           update-class!
                   :api/delete-class           delete-class!

                   ; CPP Enums
                   :api/list-enums             list-enums
                   :api/get-enum               get-enum
                   :api/create-enum            create-enum!
                   :api/update-enum            update-enum!
                   :api/delete-enum            delete-enum!

                   ; CPP Enum Members
                   :api/list-enum-members      list-enum-members
                   :api/get-enum-member        get-enum-member
                   :api/create-enum-member     create-enum-member!
                   :api/update-enum-member     update-enum-member!
                   :api/delete-enum-member     delete-enum-member!

                   ; CPP Functions
                   :api/list-functions         list-functions
                   :api/get-function           get-function
                   :api/create-function        create-function!
                   :api/update-function        update-function!
                   :api/delete-function        delete-function!

                   ; CPP Namespaces
                   :api/list-namespaces        list-namespaces
                   :api/get-namespace          get-namespace
                   :api/create-namespace       create-namespace!
                   :api/update-namespace       update-namespace!
                   :api/delete-namespace       delete-namespace!

                   ; CPP Parameters
                   :api/list-parameters        list-parameters
                   :api/get-parameter          get-parameter
                   :api/create-parameter       create-parameter!
                   :api/update-parameter       update-parameter!
                   :api/delete-parameter       delete-parameter!

                   ; Help Pages
                   :api/list-help-pages        list-help-pages
                   :api/get-help-page          get-help-page
                   :api/create-help-page       create-help-page!
                   :api/update-help-page       update-help-page!
                   :api/delete-help-page       delete-help-page!

                   ; Groups
                   :api/list-groups            list-groups
                   :api/get-group              get-group
                   :api/create-group           add-group!
                   :api/update-group           update-group!
                   :api/delete-group           delete-group!

                   :api/list-subgroups         list-subgroups
                   :api/create-subgroup        add-subgroup!
                   :api/update-subgroup!       update-group!
                   :api/delete-subgroup!       delete-group!

                   :api/list-group-variables   list-group-variables
                   :api/get-group-variable     get-group-variable
                   :api/create-group-variable  add-group-variable!
                   :api/update-group-variable  update-group-variable!
                   :api/delete-group-variable  delete-group-variable!

                   ; Languages
                   :api/list-languages         list-languages
                   :api/get-language           get-language
                   :api/create-language        create-language!
                   :api/update-language        update-language!
                   :api/delete-language        delete-language!

                   ; Modules
                   :api/list-modules           list-modules
                   :api/get-module             get-module
                   :api/create-module          add-module!
                   :api/update-module          update-module!
                   :api/delete-module          delete-module!

                   ; Permissions
                   :api/get-permission         list-permissions
                   :api/create-permission      create-permission!
                   :api/update-permission      update-permission!
                   :api/delete-permission      delete-permission!

                   ; Roles
                   :api/list-roles             list-roles
                   :api/get-role               get-role
                   :api/create-role            create-role!
                   :api/update-role            update-role!
                   :api/delete-role            delete-role!

                   ; Role Permissions
                   :api/list-role-permissions  list-role-permissions
                   :api/add-role-permission    add-role-permission!
                   :api/delete-role-permission delete-role!

                   ; Submodules
                   :api/list-submodules        list-submodules
                   :api/get-submodule          get-submodule
                   :api/create-submodule       add-submodule!
                   :api/update-submodule       update-submodule!
                   :api/delete-submodule       delete-submodule!

                   ; Translations
                   :api/list-translations      list-translations
                   :api/get-translation        get-translation
                   :api/create-translation     create-translation!
                   :api/update-translation     update-translation!
                   :api/delete-translation     delete-translation!

                   ; Users
                   :api/list-users             list-users
                   :api/get-user               get-user
                   :api/create-user            create-user!
                   :api/update-user            update-user!
                   :api/delete-user            delete-user!

                   ; User Roles
                   :api/list-user-roles        list-user-roles
                   :api/add-user-role          add-user-role!
                   :api/delete-user-role       delete-user-role!

                   ; Variables
                   :api/list-variables         list-variables
                   :api/search-variables       search-variables
                   :api/get-variable           get-variable
                   :api/create-variable        create-variable!
                   :api/update-variable        update-variable!
                   :api/delete-variable        delete-variable!})

(defn- fn->sym [f]
  (-> (str f)
      (demunge)
      (str/split #"@")
      (first)
      (symbol)))

;; Handlers
(defn api-handler [{:keys [uri params content-type]}]
  (println "--- URI" uri)
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
      (println response)
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

(comment
  (match-route api-routes "/api/enum-members/1234")
  (match-route api-routes "/api/classes/1234")
  (def route (match-route api-routes "/api/applications/1234"))
  (get api-handlers (:handler route))

  (merge {} nil)

  (login! {:email "rj@gmail.com" :password "pw"})
  (match-route api-routes "/api/applications/043b7110-5e0e-42bd-af84-2c12e61899ce/delete")
  (get api-handlers :api/login)
  (api-handler {:uri "/api/login" :params {:api-args {:email "rj@gmail.com" :password "pw"}} :content-type "application/edn"})
  (api-handler {:uri "/api/applications" :params {:api-args nil} :content-type "application/edn"})
  (api-handler {:uri "/api/applications/043b7110-5e0e-42bd-af84-2c12e61899ce/delete" :params {:api-args nil} :content-type "application/edn"})

  (api-handler {:uri "/api/modules"  :params {:api-args {:application-uuid "9f553589-7f11-451a-a3db-abd7afe4d411"}} :content-type "application/edn"})

  (api-handler {:uri "/api/languages" :params {:api-args nil} :content-type "application/edn"})

  (clj-handler {:uri "/clj/export-all" :params {:api-args nil} :content-type "application/edn"})
  (clj-handler {:uri "/clj/export-all-triples" :params {:api-args nil} :content-type "application/edn"})
  (clj-handler {:uri "/clj/export-table" :params {:clj-args [{:entity "enum"}]} :content-type "application/edn"})
  (clj-handler {:uri "/clj/export-table-triples" :params {:clj-args [{:entity "enum"}]} :content-type "application/edn"})

  (list-modules {:application-uuid "9f553589-7f11-451a-a3db-abd7afe4d411"})

  )

