(ns packaging
  (:require [clojure.java.io         :as io]
            [clojure.set             :as set]
            [clojure.spec.alpha      :as spec]
            [clojure.string          :as str]
            [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :refer [deploy]]))

;;==============================
;; Specs
;;==============================

(spec/def ::app-name      symbol?)
(spec/def ::lib-name      (spec/and symbol? namespace))
(spec/def ::main-ns       symbol?)
(spec/def ::src-dirs      (spec/coll-of string? :kind vector? :min-count 1 :distinct true))
(spec/def ::resource-dirs (spec/coll-of string? :kind vector? :distinct true))
(spec/def ::bindings      (spec/map-of var? any?))
(spec/def ::compile-opts  (spec/map-of keyword? any?))
(spec/def ::java-opts     (spec/coll-of string? :distinct true))
(spec/def ::manifest      (spec/map-of any? any?))

;;==============================
;; Global Constants
;;==============================

(def build-folder
  "Directory to contain generated JARs and a directory of JAR contents."
  "target")

(def jar-content
  "Directory to contain the JAR's contents before it is packaged."
  (str build-folder "/classes"))

(def basis
  "Basis map, which contains the root and project deps.edn maps plus some additional required keys."
  (b/create-basis {:project "deps.edn"}))

;;==============================
;; Utilities
;;==============================

(defn get-calendar-commit-version
  "Returns the current git commit's date and hash as YYYY.MM.DD-HASH.
  Depends on the `git` command being available on the JVM's `$PATH`.
  Must be run from within a `git` repository."
  []
  (let [date   (b/git-process {:git-args "show -s --format=%cs HEAD"})
        commit (b/git-process {:git-args "rev-parse --short HEAD"})]
    (-> date
        (str/replace "-" ".")
        (str "-" commit))))

(defn print-calendar-commit-version
  "Utility function for use with clojure -X."
  [& _]
  (print (get-calendar-commit-version)))

(defn get-jar-file-name
  "Relative path to the generated JAR file."
  [lib-name version]
  (format "%s/%s-%s.jar" build-folder (name lib-name) version))

(defn get-uberjar-file-name
  "Relative path to the generated UberJAR file."
  [app-name version]
  (format "%s/%s-%s-standalone.jar" build-folder (name app-name) version))

;; Private function from clojure.tools.build.api
(defn assert-required
  "Check that each key in required coll is a key in params and throw if
  required are missing in params, otherwise return nil."
  [task params required]
  (let [missing (set/difference (set required) (set (keys params)))]
    (when (seq missing)
      (throw (ex-info (format "Missing required params for %s: %s"
                              task (vec (sort missing))) (or params {}))))))

;; Private function from clojure.tools.build.api
(defn assert-specs
  "Check that key in params satisfies the spec. Throw if it exists and
  does not conform to the spec, otherwise return nil."
  [task params & key-specs]
  (doseq [[k spec] (partition-all 2 key-specs)]
    (let [v (get params k)]
      (when (and v (not (spec/valid? spec v)))
        (throw (ex-info (format "Invalid param %s in call to %s: got %s, expected %s"
                                k task (pr-str v) (spec/form spec)) {}))))))

;;==============================
;; Build and Deploy Fns
;;==============================

(defn clean
  "Delete the build folder and its contents."
  [_]
  (b/delete {:path build-folder})
  (println (format "Build folder \"%s\" removed." build-folder)))

(defn build-jar
  "Create a JAR suitable for deployment and use as a library."
  [{:keys [lib-name src-dirs resource-dirs]
    :or   {src-dirs      ["src"]
           resource-dirs ["resources"]}
    :as   params}]

  ;; Validate input args
  (assert-required "build-jar"
                   params
                   [:lib-name])

  (assert-specs "build-jar"
                params
                :lib-name      ::lib-name
                :src-dirs      ::src-dirs
                :resource-dirs ::resource-dirs)

  (let [version       (get-calendar-commit-version)
        jar-file-name (get-jar-file-name lib-name version)]

    ;; Delete the jar-content folder
    (b/delete {:path jar-content})

    ;; Copy static files to jar-content folder
    (b/copy-dir {:src-dirs   (concat src-dirs resource-dirs)
                 :target-dir jar-content})

    ;; Create pom.xml
    (b/write-pom {:lib           lib-name
                  :version       version
                  :basis         basis
                  :src-dirs      src-dirs
                  :resource-dirs resource-dirs
                  :class-dir     jar-content})

    ;; Package jar-content into a JAR file
    (b/jar {:class-dir jar-content
            :jar-file  jar-file-name})

    (println (format "JAR file created: \"%s\"" jar-file-name))))

(defn build-uberjar
  "Create an UberJAR suitable for deployment and use as an application."
  [{:keys [app-name main-ns src-dirs resource-dirs bindings compile-opts java-opts manifest]
    :or   {src-dirs      ["src"]
           resource-dirs ["resources"]
           bindings      {}
           compile-opts  {:direct-linking true
                          :elide-meta     [:doc :file :line :added]}
           java-opts     []
           manifest      {}}
    :as    params}]

  ;; Validate input args
  (assert-required "build-uberjar"
                   params
                   [:app-name :main-ns])

  (assert-specs "build-uberjar"
                params
                :app-name      ::app-name
                :main-ns       ::main-ns
                :src-dirs      ::src-dirs
                :resource-dirs ::resource-dirs
                :bindings      ::bindings
                :compile-opts  ::compile-opts
                :java-opts     ::java-opts
                :manifest      ::manifest)

  (let [version           (get-calendar-commit-version)
        uberjar-file-name (get-uberjar-file-name app-name version)]

    ;; Delete the jar-content folder
    (b/delete {:path jar-content})

    ;; Copy static files to jar-content folder
    (b/copy-dir {:src-dirs   (concat src-dirs resource-dirs)
                 :target-dir jar-content})

    ;; Compile Clojure source code to classes in the jar-content folder
    (b/compile-clj {:src-dirs     src-dirs
                    :class-dir    jar-content
                    :basis        basis
                    :bindings     bindings
                    :compile-opts compile-opts
                    :java-opts    java-opts})

    ;; Package jar-content into an UberJAR file
    (b/uber {:class-dir jar-content
             :uber-file uberjar-file-name
             :basis     basis
             :main      main-ns
             :manifest  manifest})

    (println (format "UberJAR file created: \"%s\"" uberjar-file-name))))

(defn deploy-jar
  "Upload a library JAR to https://clojars.org."
  [{:keys [lib-name]
    :as   params}]

  ;; Validate input args
  (assert-required "deploy-jar"
                   params
                   [:lib-name])

  (assert-specs "deploy-jar"
                params
                :lib-name ::lib-name)

  (let [version       (get-calendar-commit-version)
        jar-file-name (get-jar-file-name lib-name version)]

    (cond
      (not (.exists (io/file jar-file-name)))
      (println (format "No JAR can be found corresponding to the current commit for %s. Please run `build-jar` first."
                       lib-name))

      (not (and (System/getenv "CLOJARS_USERNAME")
                (System/getenv "CLOJARS_PASSWORD")))
      (println "CLOJARS_USERNAME and CLOJARS_PASSWORD must be set.")

      :else
      (deploy {:installer      :remote
               :sign-releases? false
               :artifact       jar-file-name}))))
