(ns user)

(comment

  (require '[cucumber.runner :refer [run-cucumber-tests]])

  ;; Takes 3 hr to complete
  (time
   (run-cucumber-tests
    {:debug?   false
     :features "features"
     :steps    "steps"
     :stop     true
     :browser  :chrome
     :url      "http://localhost:8081/worksheets"}))

  ;; Takes 30 min to complete
  (time
   (run-cucumber-tests
    {:debug?   false
     :features "features"
     :steps    "steps"
     :stop     true
     :query-string '(and "core" (not "extended"))
     :browser  :chrome
     :url      "http://localhost:8081/worksheets"}))
  )

(comment
  (do
    (require '[behave.server :as server]
             '[behave.handlers :refer [vms-sync!]]
             '[config.interface :refer [get-config load-config]])

    (server/init-config!)
    (server/init-db! (get-config :database :config)))

  (vms-sync!)

  (require '[behave-cms.server :as cms])
  (cms/init-db!)

  (require '[clojure.java.shell :refer [sh with-sh-dir]])
  (require '[clojure.string :as str])
  (require '[clojure.data.xml :as xml])
  (require '[clj-http.client :as client])
  (require '[me.raynes.fs :as fs])
  (require '[clojure.java.io :as io])
  (require '[behave.schema.core :refer [all-schemas]])
  (require '[datomic.api :as d])
  (require '[datomic-store.main :as ds])
  (require '[datom-utils.interface :refer [split-datoms]])
  (require '[string-utils.interface :refer [->snake ->capitalize-sentence]])
  (require '[markdown2hiccup.interface :refer [md->hiccup]])

  (defn- help-page-content [db key]
    [key (d/q '[:find ?content .
                :in $ ?k
                :where
                [?e :help-page/key ?k]
                [?e :help-page/content ?content]]
              db key)])

  (defn- flatten-help-keys [acc g-or-gv]
    (let [acc (conj acc (or (:group/help-key g-or-gv)
                            (:group-variable/help-key g-or-gv)))]
      (cond-> acc
        (seq (:group/group-variables g-or-gv))
        (concat (map (partial flatten-help-keys []) (sort-by :group-variables/order (:group/group-variables g-or-gv))))

        (seq (:group/children g-or-gv))
        (concat (map (partial flatten-help-keys []) (sort-by :group/order (:group/children g-or-gv)))))))

  (defn submodule-help-pages [db submodule]
    [(:submodule/name submodule)
     (:submodule/io submodule)
     (-> submodule
         (:db/id)
         (->> (d/pull db '[{:submodule/groups
                            [:group/help-key :group/order {:group/group-variables
                                                           [:group-variable/help-key :group-variable/order]}
                             {:group/children 6}]}]))
         (:submodule/groups)
         (->> (sort-by :group/order))
         (->> (map (partial flatten-help-keys [])))
         (concat)
         (flatten)
         (->> (map (partial help-page-content db)))
         (vec))])

  (defn get-module-help-pages [db module]
    (let [submodules (->> (:module/submodules module)
                          (sort-by :submodule/order))]
      [(:module/name module) (map (partial submodule-help-pages db) submodules)]))

  ;;; Workspace

  (def db (ds/unwrap-db ds/datomic-conn))

  (def app (d/q '[:find ?e .
                  :where [?e :application/name ?name]] db))

  (def modules
    (:application/modules (d/entity db app)))

  (map :module/name modules)

  (def module-help-pages (map (partial get-module-help-pages db) modules))

  (def DOCTYPES {:map "<!DOCTYPE map PUBLIC \"-//OASIS//DTD DITA Map//EN\" \"map.dtd\">"
                 :topic "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">"})

  (defn insert-doctype [doctype xml]
    (let [lines (str/split-lines xml)
          result (concat (take 1 lines) [(get DOCTYPES doctype)] (drop 1 lines))]
      (str/join "\n" result)))

  (def insert-topic-doctype (partial insert-doctype :topic))
  (def insert-map-doctype (partial insert-doctype :map))

  (defn generate-snippet [title help-key body-as-hiccup]
    (let [body (if (empty? body-as-hiccup) nil body-as-hiccup)]
      (insert-topic-doctype
       (xml/indent-str
        (xml/sexp-as-element
         [:topic {:id title}
          [:title title]
          [:body
           [:div {:id "snippet" :data-help-key help-key}
            body]]])))))

  (generate-snippet "MySnippet" "behaveplus:help-key" [:p "Hello World"])

  (defn generate-topic [topic-id title resource-id body]
    (insert-topic-doctype
     (xml/indent-str
      (xml/sexp-as-element
       [:topic {:id topic-id}
        [:title title]
        [:prolog
         [:resourceid {:id resource-id}]]
        [:body body]]))))

  (generate-topic "Getting_Started" "Getting Started" "GT1"
                  [:h1 "Hello World"])

  (defn gen-topic-ref
    [{:keys [href title topics]}]
    [:topicref {:href href :navtitle title}
     (when topics
       (map gen-topic-ref topics))])

  (defn gen-ditamap
    "Takes an array of maps containing properties:
    `:href`, `:title`, and `:topics`."
    [dita-topics]
    (insert-map-doctype
     (xml/indent-str
      (xml/sexp-as-element
       [:map
        (map gen-topic-ref dita-topics)]))))

  (gen-ditamap [{:href "Content/Modules/Modules.dita"
                 :title "Modules"
                 :topics [{:href "Content/Modules/Surface.dita"
                           :title "Surface"}]}])

  ;; Markdown to Hiccup

  db

  ;; Generate DITA Project Layout

  ;; Ditamap.ditamap
  ;; Content
  ;;  - Pages
  ;;    - Installation
  ;;    - About
  ;;  - Modules
  ;;    - Surface
  ;;      - Outputs
  ;;      - Inputs
  ;;    - Contain
  ;;      - Outputs
  ;;      - Inputs
  ;;    - Crown
  ;;      - Outputs
  ;;      - Inputs
  ;;    - Mortality
  ;;      - Outputs
  ;;      - Inputs

  module-help-pages
  (map #(let [submodule %]
          (second submodule)
          #_(ffirst (last submodule)))
       module-help-pages)

  ;; Surface Submodules

  (do
    (fs/delete-dir "dita-test")
    (fs/delete-dir "Content")

    ;; Make dita-test dir
    (fs/mkdir "dita-test")

    (let [cwd (str fs/*cwd* "/dita-test")]
      (fs/mkdirs (str cwd "/Resources/Images"))
      (fs/mkdirs (str cwd "/Resources/Snippets/Variables"))

      (for [[module submodules] module-help-pages]
        (do
          (fs/mkdirs (str cwd "/Content/Modules/" module "/Inputs"))
          (fs/mkdirs (str cwd "/Content/Modules/" module "/Outputs"))

          (for [[submodule io help-pages] submodules]
            (let [submodule-topic-file (str cwd "/dita-test/Content/Modules/" module "/" (if (= io :input) "Inputs" "Outputs") "/" submodule ".dita")
                  snippet-names
                  (for [[help-key help-content] help-pages]
                    (let [snippet-name (->snake (second (reverse (str/split help-key #":"))))
                          snippet-file (str fs/*cwd* "/dita-test/Resources/Snippets/Variables/" snippet-name ".dita")]
                      (spit snippet-file
                            (generate-snippet
                             snippet-name
                             help-key
                             (if help-content
                               (md->hiccup help-content)
                               [:h5 snippet-name])))
                      [snippet-name help-key]))]

              (spit submodule-topic-file
                    (generate-topic
                     (str/replace submodule #" " "_")
                     submodule
                     (->snake submodule)

                     (concat
                      '([:h1 submodule])
                      (map #(let [[snippet-name help-key] %
                                  ref (str "../../../../Resources/Snippets/Variables/" snippet-name ".dita#")]
                              [:p {:conref %}])))
                     []
                     [:h1 "Hello World"])))))))))

;; ===========================================================================================================
;; Test Matrix Generator
;; ===========================================================================================================
;; Generate comprehensive test matrix report for all :group/conditionals in the schema
;; This helps identify which conditionals need Cucumber tests

(comment
  ;; Initialize CMS database first
  (require '[behave-cms.server :as cms])
  (cms/init-db!)

  ;; Load test matrix generator
  (require '[test-matrix-generator :as tmg] :reload)

  ;; Print quick summary
  (tmg/print-summary)

  ;; Generate full test matrix report (Markdown + EDN)
  ;; Creates: development/test_matrix_report.md and development/test_matrix_data.edn
  (tmg/generate-test-matrix!)

  ;; Generate with custom paths
  (tmg/generate-test-matrix! "custom-report.md" "custom-data.edn"))
