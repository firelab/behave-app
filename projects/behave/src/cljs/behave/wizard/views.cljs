(ns behave.wizard.views
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [string-utils.interface :refer [->kebab]]
            [behave.translate :refer [<t bp]]
            [behave.components.core :as c]
            [behave.components.output-group :refer [output-group]]
            [behave.components.input-group  :refer [input-group]]
            [behave.wizard.events]
            [behave.wizard.subs]))

;;; Helpers

(defn matching-submodule? [io slug submodule]
  (and (= io (:submodule/io submodule))
       (= slug (:slug submodule))))

(defmulti submodule-page (fn [params] (:submodule/io params)))

(defmethod submodule-page :input [{groups :submodule/groups}]
  [:div
   [:h4 @(<t (bp "inputs"))]
   (for [group groups]
     ^{:key (:db/id group)}
     [input-group group])])

(defmethod submodule-page :output [{groups :submodule/groups}]
  [:div
   [:h4 @(<t (bp "outputs"))]
   (for [group groups]
     ^{:key (:db/id group)}
     [output-group group])])

(defn- io-tabs [submodules {:keys [io] :as params}]
  (let [[i-subs o-subs] (partition-by #(:submodule/io %) submodules)
        first-submodule (:slug (first (if (= io :input) o-subs i-subs)))]
    [:div.wizard-header__io-tabs
     [c/tab-group {:variant     "io"
                   :selected-fn #(= io (:tab %))
                   :on-select   #(when (not= io (:tab %))
                                   (rf/dispatch [:wizard/select-tab (assoc params
                                                                           :io (:tab %)
                                                                           :submodule first-submodule)]))
                   :tabs        [{:label "Outputs" :tab :output}
                                 {:label "Inputs" :tab :input}]}]]))

(defn wizard-header [{module-name :module/name} all-submodules {:keys [io submodule] :as params}]
  (let [submodules (filter #(= (:submodule/io %) io) all-submodules)]
    [:div.wizard-header
     [:div.wizard-header__title
      [:h1 module-name]]
     [io-tabs all-submodules params]
     [:div.wizard-header__submodule-tabs
      [c/tab-group {:variant     "io"
                    :selected-fn #(= submodule (:tab %))
                    :on-select   #(rf/dispatch [:wizard/select-tab (assoc params :submodule (:tab %))])
                    :tabs        (map (fn [{s-name :submodule/name slug :slug}]
                                       {:label s-name :tab slug}) submodules)}]]]))

(defn wizard-page [{:keys [module io submodule] :as params}]
  (let [modules         (rf/subscribe [:pull-with-attr :module/name])
        module          (first (filter #(= (str/lower-case (:module/name %)) module) @modules))
        submodules      (rf/subscribe [:pull-children
                                       :module/submodules
                                       (:db/id module)
                                       '[* {:submodule/groups [*]}]])
        submodules      (map #(assoc % :slug (->kebab (:submodule/name %))) @submodules)
        [i-subs o-subs] (partition-by #(:submodule/io %) submodules)]

    [:div.wizard-page
     [wizard-header module submodules params]
     [submodule-page (or (first (filter #(matching-submodule? io submodule %) submodules))
                         (first (if (= io :input) i-subs o-subs)))]]))

(defn root-component [params]
  (let [loaded? (rf/subscribe [:state :loaded?])]
    [:div.wizard
     (if @loaded?
       [wizard-page params]
       [:div.wizard__loading
        [:h2 "Loading..."]])]))

