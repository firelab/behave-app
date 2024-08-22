(ns cucumber.by
  (:require [clojure.string :as str])
  (:import [org.openqa.selenium By]))

(defn class-name
  "Select element by class name."
  [s]
  (By/className s))

(defn css
  "Select element by CSS selector."
  [s]
  (By/cssSelector s))

(defn id
  "Select element by ID."
  [s]
  (By/id s))

(defn input-name
  "Select using an input's `name` attribute."
  [s]
  (By/name s))

(defn link-text
  "Select using an link's text."
  [s]
  (By/linkText s))

(defn partial-link-text
  "Select using an link's partial text."
  [s]
  (By/partialLinkText s))

(defn tag-name
  "Select using a tag."
  [s]
  (By/tagName s))

(defn xpath
  "Select using an element's xpath."
  [s]
  (By/xpath s))

;; Inspired by the `attr=`, `attr-contains` in Christophe Grand's enlive
(defn attr=
  "Use `value` of arbitrary attribute `attr` to find an element. You can optionally specify the tag.
   For example: `(attr= :id \"element-id\")`
                `(attr= :div :class \"content\")`"
  ([attr value] (attr= :* attr value)) ; default to * any tag
  ([tag attr value]
   (cond
     (= :class attr) (if (re-find #"\s" value)
                       (let [classes     (str/split value #"\s+")
                             class-query (str/join "." classes)]
                         (css (str (name tag) class-query)))
                       (class-name value))
     (= :id attr)    (id value)
     (= :name attr)  (name value)
     (= :tag attr)   (tag value)
     (= :text attr)  (if (= tag :a)
                       (link-text value)
                       (xpath (str "//"
                                   (name tag)
                                   "[text()"
                                   "=\"" value "\"]")))
     :else           (css (str (name tag)
                               "[" (name attr) "='" value "']")))))
