(ns dita.xhtml-cleaner
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [hickory.core :as hickory]
            [hickory.select :as select]
            [hickory.render :as hr]
            [hiccup.core :as hiccup]
            [me.raynes.fs :as fs]))

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

    ;; Remove empty spaces & new lines
    (and (string? node)
         (re-matches #"\n\s+" node))
    nil

    ;; Remove images with empty :src
    (and (vector? node)
         (map? (second node))
         (= :img (first node))
         (empty? (-> node (second) (:src))))
    nil

    ;; Remap <tdiv> to <div>
    (= :tdiv node)
    :div

    ;; Combined node transforms
    (and (vector? node)
         (map? (second node)))
    (cond-> node
      ;; Remove style tags
      (get-in node [1 :style])
      (update 1 dissoc :style)

      ;; Ensure all <a> elements open in new tab
      (= :a (first node))
      (assoc-in [1 :target] "_blank")

      ;; Remap :src attr of all <img> elements
      (and (= :img (first node))
           (some? (-> node (second) (:src))))
      (update-in [1 :src]
                 #(-> %
                      (str/replace "../" "")
                      (str/replace "Resources/Images/" "/help/images/"))))

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

(defn clean-variables
  "Given a variable help file, remove extraneous HTML elements and:
   - Modify all H4 to H6
   - Modify all H1 to H4

  Returns a vector of maps of `{:key <help-key> :content <content>}`"
  [filename]
  (let [topic   (clean-topic filename)
        ks      (-> topic (:key) (str/split #" "))
        h4->h6  #(str/replace % #"h4" "h6")
        h1->h4  #(str/replace % #"h1" "h5")
        content (-> topic (:content) (h1->h4) (h4->h6))]
    (mapv (fn [k] {:key k :content content}) ks)))

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
