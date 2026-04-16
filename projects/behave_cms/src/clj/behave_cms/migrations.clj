(ns behave-cms.migrations
  (:require [behave-cms.store         :refer [default-conn]]
            [hiccup.page              :refer [html5]]
            [hiccup2.core             :as h]
            [schema-migrate.interface :as sm]
            [starfederation.datastar.clojure.api                  :as d*]
            [starfederation.datastar.clojure.adapter.ring
             :refer [->sse-response on-open]]))

(def ^:private migrations-dir "development/migrations")

(defn- page-data
  "Query current migration state from Datomic and filesystem."
  []
  (let [conn        (default-conn)
        all         (sm/discover-all-migrations migrations-dir)
        applied     (sm/applied-migrations conn)
        applied-set (set (map :id applied))
        pending     (remove #(or (:ignored? %) (applied-set (:id %))) all)
        ignored     (filter :ignored? all)]
    {:applied applied :pending pending :ignored ignored}))

(defn- applied-section [applied]
  [:div#applied-migrations
   [:h2 "Applied Migrations"]
   (if (seq applied)
     [:table.table.table-sm.table-striped
      [:thead [:tr [:th "Migration ID"] [:th "Applied At"]]]
      [:tbody
       (for [{:keys [id applied-at]} applied]
         [:tr [:td [:code id]] [:td (str applied-at)]])]]
     [:p.text-muted "No migrations applied yet."])])

(defn- pending-section [pending]
  [:div#pending-migrations
   [:h2.mt-4 "Pending Migrations"]
   (if (seq pending)
     [:table.table.table-sm
      [:thead [:tr [:th "Migration ID"] [:th ""]]]
      [:tbody
       (for [{:keys [id]} pending]
         [:tr
          [:td [:code id]]
          [:td [:button.btn.btn-sm.btn-primary
                {:data-on-click (str "$post('/migrations/stream?migration-id=" id "'")}
                "Run"]]])]]
     [:p.text-success "All migrations are up to date."])])

(defn- ignored-section [ignored]
  [:div#ignored-migrations
   [:h2.mt-4 "Ignored Migrations"]
   (if (seq ignored)
     [:table.table.table-sm.text-muted
      [:thead [:tr [:th "Migration ID"]]]
      [:tbody
       (for [{:keys [id]} ignored]
         [:tr [:td [:code id]]])]]
     [:p.text-muted "No ignored migrations."])])

(defn migrations-page
  "GET /migrations — renders the full migrations management page."
  [_request]
  (let [{:keys [applied pending ignored]} (page-data)]
    {:status  200
     :headers {"Content-Type" "text/html"}
     :body    (html5
               [:head
                [:title "Migrations - Behave CMS"]
                [:meta {:charset "utf-8"}]
                [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                [:link {:rel "stylesheet" :href "/css/bootstrap.min.css"}]
                [:script {:type "module" :src d*/CDN-url}]]
               [:body
                [:div.container.mt-4
                 [:h1 "Migrations"]
                 [:div#migration-error]
                 (applied-section applied)
                 (pending-section pending)
                 (ignored-section ignored)]])}))

(defn run-migration!
  "POST /migrations/stream?migration-id=... — runs a migration and patches updated fragments via SSE."
  [request]
  (let [migration-id (get-in request [:params :migration-id])
        conn         (default-conn)]
    (->sse-response request
      {on-open
       (fn [sse]
         (d*/with-open-sse sse
           (try
             (sm/run-migration-by-id! conn migrations-dir migration-id)
             (let [{:keys [applied pending]} (page-data)]
               (d*/patch-elements! sse (h/html (applied-section applied)))
               (d*/patch-elements! sse (h/html (pending-section pending))))
             (catch Exception e
               (d*/patch-elements! sse
                 (h/html [:div#migration-error.alert.alert-danger.mt-2
                           "Error: " (.getMessage e)]))))))})))
