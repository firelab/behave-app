(ns dita.core
  (:require [clojure.string :as str]
            [clojure.data.xml :as xml]
            [clj-http.client :as client]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [datomic-store.main :as ds]
            [datom-utils.interface :refer [split-datoms]]))

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

(map (partial get-module-help-pages db) modules)

(def DOCTYPES {:map   "<!DOCTYPE map PUBLIC \"-//OASIS//DTD DITA Map//EN\" \"map.dtd\">"
               :topic "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">"})

(defn insert-doctype [doctype xml]
  (let [lines  (str/split-lines xml)
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

(gen-ditamap [{:href   "Content/Modules/Modules.dita"
               :title  "Modules"
               :topics [{:href  "Content/Modules/Surface.dita"
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


(fs/mkdir "dita-test")

(defn gen-structure [modules] 
  (fs/with-cwd (fs/expand-home "~/Code/behave-polylith/dita-test")
    (fs/mkdirs "Content/Modules")
    (fs/mkdirs "Resources/Images")
    (fs/mkdirs "Resources/Snippets")


    (doall
     (for [module modules]
       (fs/mkdirs (str "Content/Modules/" module "/Inputs"))
       (fs/mkdirs (str "Content/Modules/" module "/Outputs"))
       ))
    (fs/mkdirs "Content/Modules/Surface/Outputs")

    (fs/mkdirs "Content/Modules/Surface/Inputs")
    (fs/mkdirs "Content/Modules/Surface/Outputs")

    )) 

(fs/with-cwd (fs/expand-home "~/Code/behave-polylith/dita-test")
  (fs/mkdirs "Content/Modules/Surface/Inputs")
  (fs/mkdirs "Content/Modules/Surface/Outputs")) 

;; Features

;; Modules

)
