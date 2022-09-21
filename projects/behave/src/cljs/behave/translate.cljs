(ns behave.translate
  (:require [clojure.string :as str]
            [ajax.core :refer [GET]]
            [re-frame.core :refer [dispatch-sync subscribe]]))

;;; Configuration

(def ^:private supported #{"en-US" "pt-PT"})
(def ^:private default "en-US")

;;; Helpers

(defn- get-translations [shortcode handler]
  (GET (str "/i18n/" shortcode ".csv") {:handler handler}))

(defn- csv->map [s]
  (into {} (map #(str/split % #",") (str/split-lines s))))

(defn browser-lang []
  (.. js/window -navigator -language))

;;; Public Fns

(defn bp
  [& s]
  (apply str "behaveplus:" s))

(defn <t
  "Returns the translation for `translation-key`.

  Example:
  ```
  (defn my-component []
    [:btn @(<t \"success\")])
  ```"
  [translation-key]
  (subscribe [:t translation-key]))

(defn load-translations! []
  (let [browser   (browser-lang)
        shortcode (if (contains? supported browser) browser default)]
    (get-translations shortcode #(dispatch-sync [:translations/load shortcode (csv->map %1)]))))
