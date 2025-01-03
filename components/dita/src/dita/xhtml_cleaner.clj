(ns dita.xhtml-cleaner
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hickory.core :as hickory]
            [hickory.select :as select]
            [hickory.render :as hr]
            [hiccup.core :as hiccup]
            [me.raynes.fs :as fs]
            [clojure.walk :refer [postwalk]]))

(def ^:private remove-nodes #{:meta :link})

(defn- remove-stylesheet-and-class [node]
  (cond
    ;; Remove <link>, <meta> elements
    (and (vector? node)
         (remove-nodes (first node)))
    nil

    ;; Remove elements with id
    (and (vector? node)
         (= :div (first node))
         ((-> node (second) (keys) (set)) :id))
    nil

    ;; Remove prolog elements
    (and (vector? node)
         (= :div (first node))
         (= "prolog" (-> node (second) :class)))
    nil

    (and (string? node)
         (re-matches #"\n\s+" node))
    nil

    (and (vector? node)
         (= :img (first node)))
    (if (-> node (second) (:src))
      (let [new-src (-> node
                        (second)
                        (:src)
                        (str/replace "../" "")
                        (str/replace "Resources/Images/" "/help/images/"))]
        (assoc-in node [1 :src] new-src))
      nil)

    :else
    node))

(defn- find-help-key [file]
  (let [content (-> file (slurp) (hickory/parse) (hickory/as-hickory))]
    (->> content
        (select/select (select/class :metadata))
        (first)
        (:content)
        (first))))

(defn- remove-unwanted-elements [hiccup]
  (postwalk remove-stylesheet-and-class hiccup))

(defn- clean-xhtml-file [file-path]
  (let [parser (hickory/as-hiccup (hickory/parse (slurp file-path)))]
    (remove-unwanted-elements parser)))

;; Usage

(defn clean-topic [filename]
  (let [clean-hiccup (clean-xhtml-file filename)
        help-key     (find-help-key filename)
        content      (-> clean-hiccup
                         (hiccup/html)
                         (hickory/parse)
                         (hickory/as-hickory)
                         (->> (select/select (select/tag :body)))
                         (first)
                         (hr/hickory-to-html)
                         (str/replace #"body" "div")
                         (str/replace #"h4" "h5")
                         (str/replace #"h2" "h4"))]

    ;; Help Page
    {:key help-key :content content}))

(defn clean-variable
  "Given a variable help file, remove extraneous HTML elements and:
   - Modify all H1 to H4"
  [filename]
  (let [topic  (clean-topic filename)
        h4->h5 #(str/replace % #"h4" "h6")
        h1->h4 #(str/replace % #"h1" "h5")]
    (update topic :content (comp h1->h4 h4->h5))))

(comment

  (def topics-dir (io/file ".." "behave-docs" "XHTML_Output" "BehaveAppHelp" "Content"))

  (def topics (fs/find-files topics-dir #".*htm"))

  (def cleaned-topics (mapv clean-variable variable-snippets))
  (spit "topics.edn" (pr-str cleaned-topics))

  (def variable-snippets-dir (io/file ".." "behave-docs" "XHTML_Output" "BehaveAppHelp" "Resources" "Snippets" "Variables"))
  
  (def variable-snippets (fs/glob variable-snippets-dir "*.htm"))

  (def cleaned-variables (map clean-variable variable-snippets))
  (spit "variables.edn" (pr-str cleaned-variables))


  )
