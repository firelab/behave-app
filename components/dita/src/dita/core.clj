(ns dita.core
  (:require [clojure.string :as str]
            [clojure.data.xml :as xml]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [datomic-store.main :as ds]
            [markdown2hiccup.core :refer [md->hiccup]]
            [clojure.walk :refer [postwalk]]))

;;; Utils

(defn- sentence->shortcode [sentence]
  (-> sentence
      (clojure.string/replace #"[\p{Punct}]" " ")
      (clojure.string/split #"[\s-]+")
      (->> (map clojure.string/capitalize))
      (str/join)))

(defn- largest-string [strings]
  (reduce (partial max-key count) strings))

(def llast (comp last last))

;;; Fix Markdown

(def ^:private dita-mappings
  {:h1     :title
   :h2     :title
   :h3     :title
   :h4     :title
   :h5     :title
   :h6     :title
   :a      :xref
   :img    :image
   :strong :b})

(defn- md->dita [md]
  (->> (md->hiccup md)
       (postwalk (fn [e]
                   (cond
                     (dita-mappings e)
                     (dita-mappings e)

                     (:key e)
                     (dissoc e :key)

                     :else
                     e)))))

;;; Queries

(defn- q-help-page-content [db k]
  [k (d/q '[:find ?content .
              :in $ ?k
              :where
              [?e :help-page/key ?k]
              [?e :help-page/content ?content]]
            db k)])

(defn- q-help-content [db k]
  (if (nil? k)
    ""
    (d/q '[:find ?content .
           :in $ ?k
           :where
           [?e :help-page/key ?k]
           [?e :help-page/content ?content]]
         db k)))

(defn- q-all-vars
  ;; Query for application structure variables
  [db]
  (d/q '[:find ?nid ?v-name ?help-key
         :keys :nid :v-name :help-key
         :where
         [?e :group/group-variables ?gv]
         [?gv :bp/nid ?nid]
         [?gv :group-variable/help-key ?help-key]
         [?v :variable/group-variables ?gv]
         [?v :variable/name ?v-name]] db))

(defn- flatten-help-keys [acc g-or-gv]
  (let [acc (conj acc (or (:group/help-key g-or-gv)
                          (:group-variable/help-key g-or-gv)))]
    (cond-> acc
      (seq (:group/group-variables g-or-gv))
      (concat (map (partial flatten-help-keys []) (sort-by :group-variables/order (:group/group-variables g-or-gv))))

      (seq (:group/children g-or-gv))
      (concat (map (partial flatten-help-keys []) (sort-by :group/order (:group/children g-or-gv)))))))

(defn- submodule-help-pages [db submodule]
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
       (->> (map (partial q-help-page-content db)))
       (vec))])

(defn- get-module-help-pages [db module]
  (let [submodules (->> (:module/submodules module)
                        (sort-by :submodule/order))]
    [(:module/name module) (map (partial submodule-help-pages db) submodules)]))

;;; XML

(def ^:private DOCTYPES {:map   "<!DOCTYPE map PUBLIC \"-//OASIS//DTD DITA Map//EN\" \"map.dtd\">"
                         :topic "<!DOCTYPE topic PUBLIC \"-//OASIS//DTD DITA Topic//EN\" \"topic.dtd\">"})

(defn- insert-doctype [doctype xml]
  (let [lines  (str/split-lines xml)
        result (concat (take 1 lines) [(get DOCTYPES doctype)] (drop 1 lines))]
    (str/join "\n" result)))

(def ^:private insert-topic-doctype (partial insert-doctype :topic))
(def ^:private insert-map-doctype (partial insert-doctype :map))

(defn- generate-snippet [id title help-keys body-as-hiccup]
  (let [body      (if (empty? body-as-hiccup) nil body-as-hiccup)
        help-keys (if (string? help-keys) help-keys (str/join " " help-keys))]
    (insert-topic-doctype 
     (xml/indent-str
      (xml/sexp-as-element
       [:topic {:id id}
        [:title title]
        [:body {:id "snippet"}
         [:prolog
          [:metadata help-keys]]
         [:p
          body]]])))))

(defn generate-topic
  "Generates a DITA topic."
  [topic-id title help-key body]
  (insert-topic-doctype
   (xml/indent-str
    (xml/sexp-as-element
     [:topic {:id topic-id :help-key help-key}
      [:title title]
      [:body
       [:prolog
        [:metadata help-key]]
       body]]))))

(defn- gen-topic-ref
  [{:keys [href subtopics]}]
  [:topicref {:href href}
   (when subtopics
     (map gen-topic-ref subtopics))])

(defn gen-ditamap
  "Takes an array of maps containing properties:
    `:href`, `:title`, and `:subtopics`."
  [dita-topics]
  (insert-map-doctype
   (xml/indent-str
    (xml/sexp-as-element
     [:map
      (map gen-topic-ref dita-topics)]))))

;;; File Structure

(defn- gen-structure
  "Generate the structure of the project given modules & submodules"
  [db base-dir modules modules-w-submodules] 
  (fs/with-cwd (fs/expand-home base-dir)
    (fs/mkdirs "Resources/Images")
    (fs/mkdirs "Resources/Snippets/Variables")

    (doall
     (for [module modules
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
                (md->dita content))))))

    (doall
     (for [module-w-submodule             modules-w-submodules
           [module io submodule help-key] module-w-submodule]

       (let [io        (if (= :input io) "Inputs" "Outputs")
             path      (format "Content/Modules/%s/%s/%s" module io (sentence->shortcode submodule))
             dita-file (str (io/file (fs/expand-home base-dir) path "index.dita"))
             content   (or (q-help-content db help-key) (format "## %s" submodule))]
         (fs/with-cwd (fs/expand-home base-dir)
           (fs/mkdirs path)
           (spit dita-file
                 (generate-topic
                  (sentence->shortcode submodule)
                  submodule
                  help-key
                  (md->dita content)))))))))

(defn- transform-structure [data]
  (postwalk
   (fn [x]
     (cond
       (:module/name x)
       {:nid      (:bp/nid x)
        :name     (:module/name x)
        :id       (sentence->shortcode (:module/name x))
        :help-key (:module/help-key x)
        :elements (sort-by :submodule/order (:module/submodules x))}

       (:submodule/name x)
       {:nid      (:bp/nid x)
        :name     (:submodule/name x)
        :io       (:submodule/io x)
        :id       (sentence->shortcode (:submodule/name x))
        :help-key (:submodule/help-key x)
        :elements (sort-by :group/order (:submodule/groups x))}

       (:group/name x)
       {:nid      (:bp/nid x)
        :name     (:group/name x)
        :id       (sentence->shortcode (:group/name x))
        :help-key (:group/help-key x)
        :group?   true
        :elements (sort-by :group/order (:group/children x))}

       :else x))
   data))

(defn- create-group-topic! [db base-dir href {:keys [nid id name help-key]}]
  (let [group-variables (:group/group-variables
                         (d/pull db
                                 '[{:group/group-variables
                                    [:bp/nid
                                     :group-variable/help-key
                                     :group-variable/order
                                     {:variable/_group-variables [:variable/name]}]}]
                                 [:bp/nid nid]))
        id-fn           (comp #(str "v" %) sentence->shortcode #(or % ""))
        var-path        (str (str/join (repeat (- (count (str/split href #"\/")) 1) "../")) "Resources/Snippets/Variables/")

        var-dita-files
        (->> group-variables
             (sort-by :group-variable/order)
             (map #(assoc % :id (id-fn (get-in % [:variable/_group-variables 0 :variable/name]))))
             (mapv (fn [{:keys [id] :as v}]
                     (let [help-key (:group-variable/help-key v)]
                       [:div {:id help-key}
                        [:p  {:conref (str var-path id ".dita#" id "/snippet")}]]))))]

    (spit (io/file (fs/expand-home base-dir) href)
          (generate-topic
           id
           name
           help-key
           (concat 
            (md->dita
             (or (q-help-content db help-key) (str "#### " name)))
            var-dita-files)))))

(defn- generate-topicref [href & [children]]
  [:topicref {:type "topic" :href href}
   (when children children)])

(defn- create-ditamap-entry [db base-dir structure & [parent-path]]
  (map
   (fn [{:keys [id name io elements group?] :as el}]
     (let [path (str parent-path "/" (when io (str (if (= :input io) "Inputs" "Outputs") "/")) id)
           href (if (empty? elements)
                  (str path ".dita")
                  (str path "/index.dita"))]

       (when-not (empty? elements)
         (fs/with-cwd (fs/expand-home base-dir)
           (fs/mkdirs path)))

       (when group?
         (create-group-topic! db base-dir href el))


       (generate-topicref href
                          (when elements
                            (create-ditamap-entry db base-dir elements path)))))
   structure))

(defn- generate-ditamap [db base-dir structure & [parent-path]]
  (insert-map-doctype
   (xml/indent-str
    (xml/sexp-as-element
     [:map 
      (create-ditamap-entry db base-dir structure parent-path)]))))

(defn- convert-to-ditamap [db base-dir all-vars output-file]
  (let [structure       (transform-structure all-vars)
        ditamap-content (generate-ditamap db base-dir structure "Content/Modules")]
    (spit output-file ditamap-content)))

;;; Variable Snippets

(defn- add-varable-shortcodes [vs]
  (map
   (fn [{:keys [v-name] :as v}]
     (assoc v :id (sentence->shortcode v-name)))
   vs))

(defn- filter-single-vars [vs]
  (->> 
   (group-by :id vs)
   (filter (fn [[_ v]]
             (= 1 (count v))))
   (vals)
   (apply concat)
   (filter some?)))

(defn- filter-duplicate-vars [vs]
  (->> 
   (group-by :id vs)
   (filter (fn [[_ v]]
             (< 1 (count v))))))

(defn- find-content-for-duplicates [db help-keys]
  (largest-string (map #(q-help-content db %) help-keys)))

(defn- create-variable-snippet! [base-dir id v-name help-keys content]
  (let [id        (str "v" id)
        dita-file (str id ".dita")
        dita-file (str (io/file (fs/expand-home base-dir) "Resources" "Snippets" "Variables" dita-file))]
    (spit dita-file
          (generate-snippet id
                            v-name 
                            help-keys
                            (md->dita content)))))

(defn- create-all-variable-snippets! [db base-dir]
  (let [all-vars          (q-all-vars db)
        vars-w-shortcodes (add-varable-shortcodes all-vars)
        single-vars       (filter-single-vars vars-w-shortcodes)
        duplicate-vars    (filter-duplicate-vars vars-w-shortcodes)]

    (doall 
     (for [{:keys [id v-name help-key]} single-vars
           :let
           [content (or (q-help-content db help-key) (str "#### " v-name))]]
       (create-variable-snippet! base-dir id v-name help-key content)))

    (doall 
     (for [[_ dups] duplicate-vars
           :let 
           [{:keys [id v-name]} (first dups)
            help-keys           (map :help-key dups)
            content             (or (find-content-for-duplicates db help-keys) (str "#### " v-name))]]
       (create-variable-snippet! base-dir id v-name help-keys content)))))

;;; Workspace

(comment 

  ;; Change this to where the dita project should exist
  (def base-dir "~/Code/sig/behave-polylith/dita-test-12")

  ;; Init DB
  (require '[behave-cms.server :as cms])
  (cms/init-db!)
  (def db (ds/unwrap-db ds/datomic-conn))

  ;; Generate DITA structure

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

  (gen-structure db base-dir modules modules-w-submodules)

  ;; Create Variable snippets

  (create-all-variable-snippets! db base-dir)

  ;; Generate Group Topics

  (def all-groups
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
              {:group/children 6}]}]}]}]
      app-eid)))

  (convert-to-ditamap db base-dir all-groups (io/file (fs/expand-home base-dir) "behave.ditamap"))
                      

  )
