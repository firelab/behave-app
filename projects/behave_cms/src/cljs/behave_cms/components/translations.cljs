(ns behave-cms.components.translations
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [behave-cms.utils :as u]))

(defn- upsert-translation! [{:keys [uuid] :as data}]
  (let [rf-event (if (nil? uuid) :api/create-entity :api/update-entity)]
    (rf/dispatch [rf-event :translations data])))

(defn translation-editor [language-uuid translation-key translation-uuid state]
  [:input.form-control {:type "text"
                        :on-change #(reset! state (u/input-value %))
                        :on-blur #(upsert-translation!
                                    (merge
                                      {:translation-key translation-key
                                       :language-rid (uuid language-uuid)
                                       :translation @state}
                                      (when translation-uuid {:uuid translation-uuid})))
                        :value @state}])

(defn app-translations [translation-prefix]
  (r/with-let [languages       (rf/subscribe [:entities :languages])
               *language       (r/atom nil)
               new-key         (r/atom "")
               new-translation (r/atom "")
               update-field    (fn [field] #(rf/dispatch [:state/set-state [:editors :translation field] %]))
               translations    (rf/subscribe [:translations])
               on-submit       #(do
                                  (rf/dispatch [:api/create-entity
                                                :translations
                                                {:language_rid    (uuid @*language)
                                                 :translation_key @new-key
                                                 :translation     @new-translation}])
                                  (reset! new-key "")
                                  (reset! new-translation ""))]
    [:div
     [:div.mb-3
      [:div {:style {:visibility "hidden" :height "0px"}} @*language]
      [:label.form-label "Language:"]
      [:select.form-select
       {:on-change #(reset! *language (u/input-value %))}
       [:option "Select a language..."]
       (for [{:keys [uuid language shortcode]} (vals @languages)]
         [:option {:value uuid :selected (= @*language uuid)} (str language " (" shortcode ")")])]]

     [:form.row.mb-3
      {:on-submit (u/on-submit on-submit)}
      [:h5 "New Translation"]
      [:div.col-5
       [:label.form-label {:id "translation-key"} "Key"]
       [:input.form-control
        {:id "translation-key"
         :disabled (nil? @*language)
         :type "text"
         :required true
         :value @new-key
         :on-change #(->> % (u/input-value) (reset! new-key))}]]
      [:div.col-6
       [:label.form-label {:id "translation"} "Translation"]
       [:input.form-control
        {:id "translation"
         :disabled (nil? @*language)
         :type "text"
         :required true
         :value @new-translation
         :on-change #(->> % (u/input-value) (reset! new-translation))}]]
      [:div.col-1
       {:style {:display "flex" :align-items "end"}}
       [:button.btn.btn-sm.btn-outline-primary
        {:type     "submit"
         :disabled (or (empty? @*language) (empty? @new-key) (empty? @new-translation))}
        "Create"]]]

     [:table.table.table-hover
      [:thead
       [:tr
        [:th "Language"]
        [:th "Key"]
        [:th "Translation"]]]
      [:tbody
       (for [entry (->> @translations (vals) (filter #(= @*language (str (:language_rid %)))) (sort-by :translation_key))]
         (let [translation (r/atom (:translation entry))]
           [:tr {:key uuid}
            [:td (:language entry)]
            [:td (:translation/key entry)]
            [:td
             [translation-editor
              @*language
              (:translation_key entry)
              (:uuid entry)
              translation]]]))]]]))

(defn all-translations [translation-key]
  (let [languages          (rf/subscribe [:languages])
        translations       (rf/subscribe [:translations translation-key])
        translation-lookup (group-by
                            (fn [t]
                              [(get-in t [:language/_translations :language/shortcode])
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
              translation (r/atom (:translation translation-entry))]
          [:tr {:key uuid}
           [:td language]
           [:td translation-key]
           [:td
            [translation-editor
             language-id
             translation-key
             (:db/id translation-entry)
             translation]]]))]]))
