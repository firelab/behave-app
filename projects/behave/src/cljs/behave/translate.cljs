(ns behave.translate
  (:require [behave.schema.group :refer [repeat-translation-key]]
            [re-frame.core       :refer [dispatch-sync subscribe]]))

;;; Configuration

(def ^:private supported #{"en-US"})
(def ^:private default "en-US")

;;; Helpers

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

(defn repeat-item-name
  "Name for a single item of a repeating group (\"Resource\"), falling back to the
  group's own translation (\"Resources\") when the repeat translation is unset."
  [translation-key]
  (or @(<t (repeat-translation-key translation-key))
      @(<t translation-key)))

(defn load-translations! []
  (let [browser   (browser-lang)
        shortcode (if (contains? supported browser) browser default)]
    (dispatch-sync [:translations/load shortcode])))
