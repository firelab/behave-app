#!/usr/bin/env bb
(ns align-requires
  (:require [babashka.fs      :as fs]
            [clojure.string   :as str]
            [rewrite-clj.zip  :as z]
            [rewrite-clj.node :as n]))

(defn- keyword-entry?
  "True if a require vector has :as or :refer as its second element.
   Uses z/right (skips whitespace) to find the keyword directly."
  [zloc]
  (when-let [second-child (-> zloc z/down z/right)]
    (try (#{:as :refer} (z/sexpr second-child))
         (catch Exception _ nil))))

(defn- require-vectors
  "Returns all zipper locations of [ns-sym :as/:refer ...] vectors
   within the :require clause of the given file zipper."
  [root-zloc]
  (->> root-zloc
       (iterate z/next)
       (take-while (complement z/end?))
       (filter #(and (z/vector? %) (keyword-entry? %)))))

(defn- ns-sym-len
  [entry-zloc]
  (count (str (z/sexpr (z/down entry-zloc)))))

(defn- set-whitespace-after-ns-sym
  [entry-zloc spaces]
  (let [ws-node (n/whitespace-node (str/join (repeat spaces " ")))]
    (-> entry-zloc
        z/down        ; at ns-sym
        z/right*      ; at raw whitespace node between ns-sym and keyword
        (z/replace ws-node)
        z/up)))

(defn- align-file!
  "Aligns all `:imports`/`:requires` dependencies."
  [path]
  (let [content (slurp path)
        zloc    (z/of-string content {:track-position? true})
        entries (require-vectors zloc)]
    (when (seq entries)
      (let [max-len     (apply max (map ns-sym-len entries))
            ;; Re-parse to get fresh zipper for mutations
            aligned     (loop [acc (z/of-string content)]
                          (let [es     (require-vectors acc)
                             ;; Find first entry whose spacing doesn't match target
                                target (fn [e] (- (inc max-len) (ns-sym-len e)))
                                bad-e  (first (filter (fn [e]
                                                        (let [ws (-> e z/down z/right*)]
                                                          (not= (str/join (repeat (target e) " "))
                                                                (z/string ws))))
                                                      es))]
                            (if bad-e
                              (recur (set-whitespace-after-ns-sym bad-e (target bad-e)))
                              acc)))
            new-content (z/root-string aligned)]
        (when (not= content new-content)
          (println "Aligned:" path)
          (spit path new-content))))))

(let [arg   (or (first *command-line-args*) ".")
      paths (if (fs/regular-file? arg)
              [arg]
              (->> (fs/glob arg "**/*.{clj,cljs,cljc}")
                   (map str)
                   (remove #(re-find #"/(target|node_modules|resources/public/cljs)/" %))))]
  (doseq [path paths]
    (align-file! path)))

;; usage
;; bb align_requires.clj <file>
