(ns behave-cms.components.translations
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [data-utils.interface :refer [parse-int]]
            [behave-cms.utils :as u]))

(defn- upsert-translation! [data]
  (let [rf-event (cond
                   (nil? (:db/id data))
                   :api/create-entity

                   (empty? (:translation/translation data))
                   :api/delete-entity

                   :else
                   :api/update-entity)]
    (rf/dispatch [rf-event data])))

(defn translation-editor [language-id translation-key translation-id state]
  [:input.form-control {:type      "text"
                        :on-change #(reset! state (u/input-value %))
                        :on-blur   #(upsert-translation!
                                     (merge
                                      {:translation/key         translation-key
                                       :language/_translation   language-id
                                       :translation/translation @state}
                                      (when translation-id {:db/id translation-id})))
                        :value     @state}])

(defn app-translations [translation-prefix]
  (r/with-let [languages       (rf/subscribe [:languages])
               *language       (r/atom nil)
               new-key         (r/atom "")
               new-translation (r/atom "")
               update-field    (fn [field] #(rf/dispatch [:state/set-state [:editors :translation field] %]))
               translations    (rf/subscribe [:all-translations translation-prefix])
               on-submit       #(do
                                  (rf/dispatch [:api/create-entity
                                                {:language/_translation   @*language
                                                 :translation/key         @new-key
                                                 :translation/translation @new-translation}])
                                  (reset! new-key "")
                                  (reset! new-translation ""))]
    [:div
     [:div.mb-3
      [:div {:style {:visibility "hidden" :height "0px"}} @*language]
      [:label.form-label "Language:"]
      [:select.form-select
       {:on-change #(reset! *language (parse-int (u/input-value %)))}
       [:option "Select a language..."]
       (doall (for [{id :db/id language :language/name shortcode :language/shortcode} @languages]
                ^{:key id}
                [:option {:value id :selected (= @*language id)} (str language " (" shortcode ")")]))]]

     [:form.row.mb-3
      {:on-submit (u/on-submit on-submit)}
      [:h5 "New Translation"]
      [:div.col-5
       [:label.form-label {:id "translation-key"} "Key"]
       [:input.form-control
        {:id        "translation-key"
         :disabled  (nil? @*language)
         :type      "text"
         :required  true
         :value     @new-key
         :on-change #(->> % (u/input-value) (reset! new-key))}]]
      [:div.col-6
       [:label.form-label {:id "translation"} "Translation"]
       [:input.form-control
        {:id        "translation"
         :disabled  (nil? @*language)
         :type      "text"
         :required  true
         :value     @new-translation
         :on-change #(->> % (u/input-value) (reset! new-translation))}]]
      [:div.col-1
       {:style {:display "flex" :align-items "end"}}
       [:button.btn.btn-sm.btn-outline-primary
        {:type     "submit"
         :disabled (not (or @*language @new-key @new-translation))}
        "Create"]]]

     [:table.table.table-hover
      [:thead
       [:tr
        [:th "Language"]
        [:th "Key"]
        [:th "Translation"]]]
      [:tbody
       (for [entry (->> @translations (filter #(= @*language (get-in % [:language/_translation 0 :db/id]))) (sort-by :translation/key))]
         (let [id            (:db/id entry)
               language-name (get-in entry [:language/_translation 0 :language/name])
               translation   (r/atom (:translation/translation entry))
               key           (:translation/key entry)]
           ^{:key id}
           [:tr
            [:td language-name]
            [:td key]
            [:td
             [translation-editor
              @*language
              key
              id
              translation]]]))]]]))

(defn all-translations [translation-key]
  (let [languages          (rf/subscribe [:languages])
        translations       (rf/subscribe [:translations translation-key])
        translation-lookup (group-by
                            (fn [t]
                              [(get-in t [:language/_translation 0 :language/shortcode])
                               (:translation/key t)])
                            @translations)]
    [:table.table.table-hover
     [:thead
      [:tr
       [:th "Language"]
       [:th "Key"]
       [:th "Translation"]]]
     [:tbody
      (for [{language-id :db/id language :language/name shortcode :language/shortcode} @languages]
        (let [translation-entry (get-in translation-lookup [[shortcode translation-key] 0] {})
              id                (:db/id translation-entry)
              translation       (r/atom (:translation/translation translation-entry))]
          ^{:key id}
          [:tr
           [:td language]
           [:td translation-key]
           [:td
            [translation-editor
             language-id
             translation-key
             id
             translation]]]))]]))
