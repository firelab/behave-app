(ns manage
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- replace-name [s replacement]
  (str/replace s #"__name__" replacement))

(defn- replace-in-file [file replacement]
  (-> (slurp file)
      (replace-name replacement)))

(defn- template-dir
  "Templates for `basedir`, outside the brick folders so poly ignores them."
  [basedir]
  (io/file "scripts" "templates" basedir))

(defn replace-filename [f dir replacement]
  (let [snake (str/replace replacement #"-" "_")
        depth (count (str/split (str (template-dir dir)) #"\/"))]
    (as-> f %
      (.getPath %)
      (replace-name % snake)
      (str/split % #"\/")
      (drop depth %)
      (apply io/file dir snake %))))

(defn- copy-template [basedir new-name]
  (let [files (map (fn [f] [(replace-filename f basedir new-name) f])
                   (filter #(.isFile %) (file-seq (template-dir basedir))))]

    (doall (map (fn [[new-file template-file]]
                  (let [contents (replace-in-file template-file new-name)]
                    (io/make-parents new-file)
                    (spit new-file contents))) files))))

(defn new-component [component]
  (copy-template "components" component))

(defn new-base [base]
  (copy-template "bases" base))

(defn new-project [project]
  (copy-template "projects" project))
