(ns behave.stories.utils
  (:require [reagent.core :as r]))

(defn ->params
  [^js args]
  (js->clj args :keywordize-keys true))

(defn ->default
  [options]
  (-> options (update :component r/reactify-component) clj->js))

