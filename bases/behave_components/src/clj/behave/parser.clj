(ns behave.parser
  (:require [clojure.string  :as str]
            [clojure.java.io :as io]
            [hickory.core    :as h]))

(defn- ->hiccup [html]
  (-> html (h/parse) (h/as-hiccup)))

(defn- ->hickory [html]
  (-> html (h/parse) (h/as-hickory)))

(defn- remove-id [{:keys [attrs tag] :as el}]
  (if (= tag :clipppath)
    el
    (assoc el :attrs (dissoc attrs :id))))

(defn- remove-attr [{:keys [attrs] :as el} attr]
  (assoc el :attrs (dissoc attrs attr)))

(defn- add-style-map [{:keys [attrs] :as el}]
  (if-let [style (:style attrs)]
    (assoc-in el [:attrs :style] (->> (str/split style #";")
                                      (reduce (fn [acc cur] (let [[k v]  (str/split cur #":")]
                                                              (assoc acc (keyword k) v))) {})))
    el))

(defn- walk-hickory [{:keys [content] :as el} xforms]
  (-> el
      (xforms)
      (assoc :content (if (vector? content)
                        (vec (map #(walk-hickory % xforms)
                                  (filter map? content)))
                        content))))

(defn- hickory->hiccup [{:keys [type tag attrs content]}]
  (when (not= type :comment)
    (condp = type
      :document (some #(when (not (nil? %)) %)
                      (mapv hickory->hiccup content))
      :element  (vec (concat (if (some? attrs) [tag attrs] [tag]) (vec (map hickory->hiccup content)))))))

(defn clean-svg [html]
  (-> (->hickory html)
      (walk-hickory #(-> % (remove-id) (remove-attr :data-name) (add-style-map)))
      (hickory->hiccup)))

(defn str->style-map [s]
  (persistent!
    (as-> s $
      (str/split $ #";")
      (map #(str/split % #":") $)
      (reduce (fn [acc [k v]] (assoc! acc (keyword k) (str/trim v))) (transient {}) $))))

(defn walk-hiccup [hiccup f]
  (when (vector? hiccup)
    (if (map? (second hiccup))
      (let [attrs    (-> hiccup (second) (f))
            children (filter some? (map #(walk-hiccup % f) (rest (rest hiccup))))]
        (into [(first hiccup) attrs] children))
      (into [(first hiccup)] (filter some? (map #(walk-hiccup % f) (rest (rest hiccup))))))))

(defn fix-styles [{:keys [style] :as attrs}]
  (if (string? style)
    (assoc attrs :style (str->style-map style))
    attrs))

(defn fix-attrs [attrs]
  (-> attrs
      (dissoc :data-name)
      (fix-styles)))

(defn convert-svg-icons [base-dir output-file]
  (let [files (filter #(and (.isFile %) (str/ends-with? (.getAbsolutePath %) ".svg")) (file-seq (io/file base-dir)))]
    (for [f files]
      (let [var-name (str/replace (.. f toPath getFileName toString) #"\.svg" "")
            svg      (-> (slurp f) (clean-svg) (nth 2) (second))]
        (spit output-file
              (str "(def ^:private " var-name "\n  " (prn-str svg) ")\n\n")
              :append true)))))


(defn convert-html-files [base-dir output-dir]
  (let [files (filter #(and (.isFile %) (str/ends-with? (.getAbsolutePath %) ".html")) (file-seq (io/file base-dir)))]
    (for [f files]
      (let [html-file (str/replace (.. f toPath getFileName toString) #"\.html" "")
            output-file (str output-dir html-file ".cljs")]
        (io/make-parents output-file)
        (some->> (slurp f)
                 (->hiccup)
                 (prn-str)
                 (str "(ns behave.components." html-file ")\n\n")
                 (spit (str output-dir html-file ".cljs")))))))

(comment

  (def icon-dir "/Users/rsheperd/code/sig/behave-components/resources/public/bhp-icons/modules")
  (def icon-dir "/home/kcheung/work/code/behave-polylith/bases/behave_components/resources/public/icons")

  (def output-file "derp.cljs")

  (convert-svg-icons icon-dir output-file)

  (def html-dir "/Users/rsheperd/code/sig/behave-components/resources/public/html/")
  (def output-components-dir "behave-test/components/")

  (convert-html-files html-dir output-components-dir)
  )
