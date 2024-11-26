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
  [db app-eid]
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
              :group-variable/order
              {:variable/_group-variables [:variable/name]}]}
            {:group/children 6}]}]}]}]
    app-eid)))

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
       [:topic {:id id :help-keys help-keys}
        [:title title]
        [:body {:id "snippet"} body]])))))

(defn generate-topic
  "Generates a DITA topic."
  [topic-id title help-key body]
  (insert-topic-doctype
   (xml/indent-str
    (xml/sexp-as-element
     [:topic {:id topic-id :help-key help-key}
      [:title title]
      [:body body]]))))

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
               (md->hiccup content)))))

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
                 (md->hiccup content))))))))

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
        var-path        (str (str/join (repeat (count (str/split href #"\/")) "../")) "Resources/Snippets/Variables/")

        var-dita-files
        (->> group-variables
             (sort-by :group-variable/order)
             (map #(get-in % [:variable/_group-variables 0 :variable/name]))
             (map (comp (partial format "v%s.dita") sentence->shortcode))
             (mapv (fn [v] [:div {:conref (str var-path v)}])))]

    (spit (io/file (fs/expand-home base-dir) href)
          (generate-topic
           id
           name
           help-key
           (concat 
            (md->hiccup 
             (or (q-help-content db help-key) (str "#### " name)))
            var-dita-files)))))

(defn- generate-topicref [href & [children]]
  [:topicref {:type "topic" :href href}
   (when children children)])

(defn- create-ditamap-entry [db base-dir structure & [parent-path]]
  (map
   (fn [{:keys [id name io elements group?] :as el}]
     (let [path (str parent-path "/" (when io (str (if (= :input io) "Inputs" "Outputs") "/")))
           href (if (empty? elements)
                  (str path id ".dita")
                  (str path id "/index.dita"))]

       (println "Creating" path)
       (fs/with-cwd (fs/expand-home base-dir)
         (fs/mkdirs path))

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
        _               (println structure)
        ditamap-content (generate-ditamap db base-dir structure "Content/Modules")]
    (spit output-file ditamap-content)))

;;; Variable Snippets


(defn- nested-groups [group]
  (if-let [children (:group/children group)]
    children
    []))

(defn- extract-deeply-nested-gvs [modules]
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

(defn- extract-nested-gvs [modules]
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

(defn- extract-vars [modules]
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

(defn- ->vars-w-shortcodes [flattened-vars]
  (map
   #(let [gv     (last %)
          v-name (first gv)]
      (when v-name 
        (assoc % (dec (count %)) (concat [(sentence->shortcode v-name)] gv))))
   flattened-vars))

(defn- filter-single-vars [vs]
  (->> 
   (group-by #(-> % (last) (first)) vs)
   (filter (fn [[_ v]]
             (= 1 (count v))))
   (vals)
   (apply concat)
   (filter some?)))

(defn- filter-duplicate-vars [vs]
  (->> 
   (group-by #(-> % (last) (first)) vs)
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
                            (md->hiccup content)))))

(defn- create-all-variable-snippets! [db base-dir app-eid]
  (let [all-vars          (q-all-vars db app-eid)
        flattened-vars    (concat
                           (extract-vars all-vars)
                           (extract-nested-gvs all-vars)
                           (extract-deeply-nested-gvs all-vars))
        vars-w-shortcodes (->vars-w-shortcodes flattened-vars)
        single-vars       (filter-single-vars vars-w-shortcodes)
        duplicate-vars    (filter-duplicate-vars vars-w-shortcodes)]

    (doall 
     (for [v single-vars
           :let
           [[id v-name help-key] (last v)
            content              (or (q-help-content db help-key) (str "#### " v-name))]]
       (create-variable-snippet! base-dir id v-name help-key content)))

    (doall 
     (for [[_ dups] duplicate-vars
           :let 
           [[id v-name] (-> dups (first) (last))
            help-keys   (map #(-> % (last) (last)) dups)
            content     (or (find-content-for-duplicates db help-keys) (str "#### " v-name))]]

       (create-variable-snippet! base-dir id v-name help-keys content)))))

;;; Workspace

(comment 

  ;; Change this to where the dita project should exist
  (def base-dir "~/Code/sig/behave-polylith/dita-test-2") 

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

  (create-all-variable-snippets! db base-dir app-eid)

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
