(ns behave-cms.modules
  (:require [clojure.string  :as str]
            [clojure.java.io :as io]
            [clojure.edn     :as edn]))

(def behave-dir (io/file "/Users/rsheperd/Code/sig/hatchet/test/resources/behave"))
(def behave-class-files (filter #(str/ends-with? (.getAbsolutePath %) ".edn") (file-seq behave-dir)))

;; Get all of the classes

(def all-namespaces (reduce (fn [acc cur] (merge-with into acc (edn/read-string (slurp cur)))) {} behave-class-files))

(def filename "cpp_test3.sql")
(spit filename "-- Auto-generated from hatchet data")

(defn k->s [k]
  (str (symbol k)))

(doseq [[_cpp-ns classes] all-namespaces]
  #_(println cpp-ns)
  (spit filename "\n\n" :append true)
  #_(spit filename (format "SELECT cpp.create_namespace('%s');\n\n" (k->s cpp-ns)) :append true)
  (when (map? classes)
    (doseq [[class-name fns] classes]
      #_(println class-name)
      (spit filename (format "\n\n-- %s\n" (k->s class-name)) :append true)
      #_(spit filename (format "\nSELECT cpp.create_class('%s', '%s');\n" (k->s cpp-ns) (k->s class-name)) :append true)
      (when (map? fns)
        (doseq [[_ {:keys [type id parameters] :or {type "void"}}] fns]
          (when id
            (spit filename (format "SELECT cpp.add_class_function('%s', '%s', '%s');\n" (k->s class-name) id type) :append true)
            (let [fname id]
              (doseq [[idx {:keys [type id] :or {type "void"}}] (map-indexed vector parameters)]
                (spit filename (format "SELECT cpp.add_function_parameter('%s', '%s', '%s', '%s', %d);\n" (k->s class-name) fname type id idx) :append true)))))))))
