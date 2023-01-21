(ns behave-cms.languages.subs
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [re-posh.core :as rp]))

;;; Languages

(rf/reg-sub
 :languages
 (fn [_]
   (rf/subscribe [:pull-with-attr :language/name]))
 identity)

;;; Translations

(rf/reg-sub
 :all-translations
 (fn [_]
   (rf/subscribe [:pull-with-attr :translation/key '[* {:language/_translation [:language/shortcode :language/name :db/id]}]]))
 (fn [translations [_ prefix]]
   (filter #(str/starts-with? (:translation/key %) prefix) translations)))

(rf/reg-sub
 :translation-ids
 (fn [[_ translation-key]]
   (rf/subscribe [:query
                  '[:find  [?e ...]
                    :in    $ ?translation-key 
                    :where [?e :translation/key ?translation-key]]
                  [translation-key]]))
 identity)

(rf/reg-sub
 :translations
 (fn [[_ translation-key]]
   (rf/subscribe [:translation-ids translation-key]))

 (fn [results]
   @(rf/subscribe [:pull-many '[* {:language/_translation [*]}] results])))
