(ns behave.wizard
  (:require [re-frame.core :as rf]
            [behave.translate :refer [<t bp]]
            [behave.components.output-group :refer [output-group]]
            [behave.components.input-group  :refer [input-group]]))

(defmulti submodule-page (fn [params] (:io params)))

(defmethod submodule-page :input [{groups :submodule/groups}]
  [:div
   [:h4 (<t (bp "inputs"))]
   (for [group groups]
     (let [id (:db/id group)]
       ^{:key id}
       (input-group group)))])

(defmethod submodule-page :output [{groups :submodule/groups}]
  [:div
   [:h4 (<t (bp "outputs"))]
   (for [group groups]
     (let [id (:db/id group)]
       ^{:key id}
       (output-group group)))])

(defn root-component [params]
  (let [module     (rf/subscribe [:pull-with-attr :module/name (:module params)])
        submodules (rf/subscribe [:pull-children :module/submodules (:db/id @module)])]

    [:div
     [:h1 (str (<t (bp "module")) ":" (<t (:module/translation-key @module)))]
     [:h2 (str "Submodule:" (:submodule module))]
     [:h3 (str "Worksheet ID:" (:db/id module))]

     [submodule-page module]]))
