(ns dita.core
  (:require [clojure.string :as str]
            [clojure.data.xml :as xml]
            [clj-http.client :as client]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [datomic-store.main :as ds]
            [datom-utils.interface :refer [split-datoms]]
            [behave-cms.server :as cms]
            [behave.schema.core :refer [rules]]
            [markdown2hiccup.core :refer [md->hiccup]]
            [clojure.walk :refer [postwalk]]))

(defn sentence->shortcode [sentence]
  (-> sentence
      (clojure.string/replace #"[\p{Punct}]" " ")
      (clojure.string/split #"[\s-]+")
      (->> (map clojure.string/capitalize))
      (str/join)))

(defn ->topic-name [s]
  (str 
   (str/lower-case (subs s 0 1))
   (subs s 1)))

(defn largest-string [strings]
  (reduce (partial max-key count) strings))

(defn ->dita-filename [s]
  (str s ".dita"))

(->topic-name 
 (sentence->shortcode "Hello, World! Does-this-work-too % ! . (maximium) How are you?"))

(defn- help-page-content [db key]
  [key (d/q '[:find ?content .
              :in $ ?k
              :where
              [?e :help-page/key ?k]
              [?e :help-page/content ?content]]
            db key)])

(defn- q-help-content [db key]
  (if (nil? key)
    ""
    (d/q '[:find ?content .
           :in $ ?k
           :where
           [?e :help-page/key ?k]
           [?e :help-page/content ?content]]
         db key)))

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

;;; XML

(def DOCTYPES {:map   "<!DOCTYPE map PUBLIC \"-//OASIS//DTD DITA Map//EN\" \"map.dtd\">"
               :topic "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">"})

(defn insert-doctype [doctype xml]
  (let [lines  (str/split-lines xml)
        result (concat (take 1 lines) [(get DOCTYPES doctype)] (drop 1 lines))]
    (str/join "\n" result)))

(def insert-topic-doctype (partial insert-doctype :topic))
(def insert-map-doctype (partial insert-doctype :map))

(defn generate-snippet [id title help-keys body-as-hiccup]
  (let [body      (if (empty? body-as-hiccup) nil body-as-hiccup)
        help-keys (if (string? help-keys) help-keys (str/join " " help-keys))]
    (insert-topic-doctype 
     (xml/indent-str
      (xml/sexp-as-element
       [:topic {:id id :help-keys help-keys}
        [:title title]
        [:body {:id "snippet"} body]])))))

(defn generate-topic [topic-id title help-key body]
  (insert-topic-doctype
   (xml/indent-str
    (xml/sexp-as-element
     [:topic {:id topic-id :help-key help-key}
      [:title title]
      [:body body]]))))

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

;;; Workspace

(comment 
  (cms/init-db!)
  (def db (ds/unwrap-db ds/datomic-conn))

  (def app-eid
    (d/q '[:find ?e .
           :where [?e :application/name ?name]] db))

  (def modules
    (:application/modules (d/entity db app-eid)))

  (def modules-w-submodules
    (map (fn [m]
           (->> (:module/submodules m)
                (map (juxt :submodule/io :submodule/name :submodule/help-key))
                (map #(concat [(:module/name m)] %))))
         modules))


  (defn gen-structure [base-dir modules modules-w-submodules] 
    (fs/with-cwd (fs/expand-home base-dir)
      #_(fs/mkdirs "Resources/Images")
      #_(fs/mkdirs "Resources/Snippets/Variables")

      #_(for [module modules
            :let   [m-name    (:module/name module)
                    help-key  (:module/help-key module)
                    path      (format "Content/Modules/%s" m-name)
                    dita-file (str (io/file (fs/expand-home base-dir) path "index.dita"))
                    content   (or (q-help-content db help-key) (format "# %s" m-name))]]
        (fs/with-cwd (fs/expand-home base-dir)
          (fs/mkdirs path)
          (spit dita-file
                (generate-topic
                 (sentence->shortcode m-name)
                 m-name
                 help-key
                 (md->hiccup content)))))

      (for [module-w-submodule             modules-w-submodules
            [module io submodule help-key] module-w-submodule]

        (let [io        (if (= :input io) "Inputs" "Outputs")
              path      (format "Content/Modules/%s/%s/%s" module io submodule)
              dita-file (str (io/file (fs/expand-home base-dir) path "index.dita"))
              content   (or (q-help-content db help-key) (format "## %s" submodule))]
          (fs/with-cwd (fs/expand-home base-dir)
            (fs/mkdirs path)
            (spit dita-file
                  (generate-topic
                   (sentence->shortcode submodule)
                   submodule
                   help-key
                   (md->hiccup content))))))))

  (def base-dir "~/Code/sig/behave-polylith/dita-test")
  (gen-structure base-dir modules modules-w-submodules)

  (def all-gvs (d/q '[:find ?s-name ?g-name ?v-name
                      :in $ %
                      :where
                      [?g :group/group-variables ?gv]
                      (submodule-root ?sm ?g)
                      [?sm :submodule/name ?s-name]
                      [?g :group/name ?g-name]
                      [?v :variable/group-variables ?gv]
                      [?v :variable/name ?v-name]]
                    db rules))

  ;; Create a Clojure function that, given the following DataScript query,
  ;; extracts each variable name as a vector with it's parents.

  ;; The result should look like:
  ;; [<module-name> <submodule-name> <group-1-name> ... <group-N-name> <variable-name>]

  (def all-vars
    (:application/modules 
     (d/pull
      db 
      '[{:application/modules 
         [:bp/nid
          :module/name
          :module/help-key
          :module/order
          {:module/submodules
           [:bp/nid
            :submodule/io
            :submodule/order
            :submodule/name
            :submodule/help-key
            {:submodule/groups
             [:bp/nid
              :group/order
              :group/name
              :group/help-key
              {:group/group-variables
               [:bp/nid
                :group-variable/help-key
                :group-variable/orde
                {:variable/_group-variables [:variable/name]}]}
              {:group/children 6}]}]}]}]
      app-eid)))

  (keys (first all-vars))

  (defn nested-groups [group]
    (if-let [children (:group/children group)]
      children
      []))

  (defn extract-deeply-nested-gvs [modules]
    (for [m  (sort-by :module/name modules)
          sm (->> (:module/submodules m) (sort-by :submodule/order))
          g  (->> (:submodule/groups sm) (sort-by :group/order))
          c1 (->> (nested-groups g) (sort-by :group/order))
          c2 (->> (nested-groups c1) (sort-by :group/order))
          gv (->> (:group/group-variables c2) (sort-by :group-variable/order))]
      [(:module/name m)
       (if (= :input (:submodule/io sm)) "Inputs" "Outputs")
       ((juxt :submodule/name :submodule/help-key) sm)
       ((juxt :group/name :group/help-key) g)
       ((juxt :group/name :group/help-key) c1)
       ((juxt :group/name :group/help-key) c2)
       [(get-in gv [:variable/_group-variables 0 :variable/name])
        (:group-variable/help-key gv)]]))

  (defn extract-nested-gvs [modules]
    (for [m  (sort-by :module/name modules)
          sm (->> (:module/submodules m) (sort-by :submodule/order))
          g  (->> (:submodule/groups sm) (sort-by :group/order))
          c  (->> (nested-groups g) (sort-by :group/order))
          gv (->> (:group/group-variables g) (sort-by :group-variable/order))]

      [[(:module/name m) (:module/help-key m)]
       (if (= :input (:submodule/io sm)) "Inputs" "Outputs")
       ((juxt :submodule/name :submodule/help-key) sm)
       ((juxt :group/name :group/help-key) g)
       ((juxt :group/name :group/help-key) c)
       [(get-in gv [:variable/_group-variables 0 :variable/name])
        (:group-variable/help-key gv)]]))

  (defn extract-vars [modules]
    (for [m  (sort-by :module/name modules)
          sm (->> (:module/submodules m) (sort-by :submodule/order))
          g  (->> (:submodule/groups sm) (sort-by :group/order))
          gv (->> (:group/group-variables g) (sort-by :group-variable/order))]
      [[(:module/name m) (:module/help-key m)]
       (if (= :input (:submodule/io sm)) "Inputs" "Outputs")
       ((juxt :submodule/name :submodule/help-key) sm)
       ((juxt :group/name :group/help-key) g)
       [(get-in gv [:variable/_group-variables 0 :variable/name])
        (:group-variable/help-key gv)]]))

  (def flattened-vars
    (concat
     (extract-vars all-vars)
     (extract-nested-gvs all-vars)
     (extract-deeply-nested-gvs all-vars)))

  (def q-help (partial q-help-content db))

  (def vars-w-shortcodes
    (map #(let [gv     (last %)
                v-name (first gv)]
            (when v-name 
              (assoc % (dec (count %)) (concat [(sentence->shortcode v-name)] gv )))) flattened-vars))

  (def simple-vars 
    (->> 
     (group-by #(-> % (last) (first)) vars-w-shortcodes)
     (filter (fn [[k v]]
               (= 1 (count v))))
     (vals)
     (apply concat)
     (filter some?)))

  (count simple-vars)

  (def duplicate-vars 
    (->> 
     (group-by #(-> % (last) (first)) vars-w-shortcodes)
     (filter (fn [[k v]]
               (< 1 (count v))))))

  (count duplicate-vars)

  (def existing-help-pages
    (->> duplicate-vars
     (map (fn [[k v]]
            (let [help-keys (map #(-> % (last) (last)) v)]
              (map q-help help-keys)
              #_(largest-string help-contents))))))

  (def test-var
    (-> duplicate-vars
        (first)
        (second)))

  (count test-var)

  (defn find-content-for-duplicates [dup-vars]
    (largest-string (map (fn [v]
                           (let [help-key (-> v (last) (last))]
                             (q-help help-key))) dup-vars)))

  (for [[k dups] duplicate-vars]
    (let [v-name    (-> dups (first) (last) (second))
          help-keys (map #(-> % (last) (last)) dups)
          id        (str "v" k)
          dita-file (str id ".dita")
          dita-file (str (io/file (fs/expand-home base-dir) "Resources" "Snippets" "Variables" dita-file))
          content   (or (find-content-for-duplicates dups) (str "#### " v-name))]
      (spit dita-file
            (generate-snippet id
                              v-name 
                              help-keys
                              (md->hiccup content)))))

  (for [v simple-vars]
    (let [[id v-name help-key] (last v)
          id                   (str "v" id)
          dita-file            (str id ".dita")
          dita-file            (str (io/file (fs/expand-home base-dir) "Resources" "Snippets" "Variables" dita-file))
          content              (or (q-help help-key) (str "#### " v-name))]
      (spit dita-file
            (generate-snippet id
                              v-name 
                              help-key
                              (md->hiccup content)))))

  (d/q '[:find ])

;; Goals
;; 1. Create a directory structure of Modules/IO/<submodule-name>.dita
;; 2. Create snippts for all Variables linked via Group Variables, linked to their help key 
;; 3. Create Submodule topic files with related Groups, Group Variable snippets

  (map :module/name modules)

  (map (partial get-module-help-pages db) modules)

  (generate-snippet "MySnippet" "behaveplus:help-key" [:p "Hello World"])

  (generate-topic "Getting_Started" "Getting Started" "GT1" [:h1 "Hello World"])

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
         (fs/mkdirs (str "Content/Modules/" module "/Outputs"))))


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
