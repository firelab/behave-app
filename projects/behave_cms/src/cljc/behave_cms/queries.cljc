(ns behave-cms.queries
  (:require [clojure.string :as str]
            #?(:cljs [datascript.core :as d]
               :clj  [datahike.api :as d])))

(def rules
  '[[(module ?a ?m) [?e :application/module ?m]]
    [(submodule ?m ?s) [?m :module/submodule ?s]]
    [(group ?s ?g) [?m :submodule/group ?s]]
    [(subgroup ?g ?sg) [?g :group/children ?sg]]
    [(variable ?g ?v) [?g :group/group-variable ?v]]
    [(language ?code ?l) [?l :language/shortcode ?code]]
    [(translation ?k ?t) [?t :translation/key ?k]]])

(def translation
  '[:find [?translation]
    :in $ % ?key ?short
    :where [language ?short ?l]
           [translation ?key ?t]
           [?t :translation/language ?l]
           [?t :translation/translation ?translation]])

(def translation-key
  '[:find [?k]
    :in $ ?e ?key-attr
    :where [?e ?key-attr ?k]])

(defn app-translation [db application-id language-shortcode]
  (let [[translation-key] (d/q translation-key db application-id :application/translation-key)]
    (d/q translation db rules translation-key language-shortcode)))

(defn app-translation-keys [db application-id]
  (let [[translation-key] (d/q translation-key db application-id :application/translation-key)]
    (->> (d/datoms db :avet :translation/key)
         (filter #(str/starts-with? % translation-key)))))

(comment
  (require '[behave-cms.store :as db]
           '[re-frame.core    :as rf])

  (app-translation-keys @@db/conn 78)

  (d/q '[:find ?t :in $ % ?k ?s :where [?t :translation/key ?k] [?l :language/shortcode ?s] [?t :translation/language ?l]] @@db/conn rules "behaveplus" "en-US")

  (d/q '[:find ?t :in $ % ?k ?s :where [translation ?k ?t] [language ?s ?l] [?t :translation/language ?l]] @@db/conn rules "behaveplus" "en-US")

  (app-translation @@db/conn 78 "en-US")

  (d/q '[:find ?m :in $ % ?e :where [module ?e ?m]] @@db/conn rules 78)


  (rf/dispatch [:transact [{:db/id 78 :application/translation-key "behaveplus"}]])

  (d/transact @db/conn [{:db/id 89 :language/name "English" :language/shortcode "en-US"}])
  (d/transact @db/conn [{:language/name "Portuguese (Portugal)" :language/shortcode "pt-PT"}])

  (d/transact @db/conn [{:translation/language 89 :translation/key "behaveplus" :translation/translation "BehavePlus"}])

  (d/q '[:find ?e ?name
         :where [?e :language/name ?name]] @@db/conn)
  (d/q '[:find ?t ?k :where [?t :translation/key ?k]] @@db/conn)

  (d/pull @@db/conn '[*] 90)

  (d/q '[:find ?e ?name ?translation
         :where [?e :application/name ?name]
         [?e :application/translation-key ?k]
         [?t :translation/key ?k]
         [?t :translation/translation ?translation]]
       @@db/conn)


  (defn module-translations [db application-id]
    (d/q '[:find ?m
           :in $ % ?e
           :where [module ?e ?m]] db rules application-id))

  (module-translations @@db/conn 78)


  ;(d/pull-many [{:module/translation-key [:translation/key :translation/translation]}])))

  (d/q '[:find ?m ?translation
         :in $ ?e
         :where
         [?e :application/module ?m]
         [?m :module/name ?name]
         [?m :module/translation-key ?key]
         [?t :translation/key ?key]
         [?t :translation/translation ?translation]]
       @@db/conn 78)

  (d/transact @db/conn [{:db/id 65 :module/translation-key "behaveplus:contain"}])
  (d/transact @db/conn [{:db/id 66 :module/translation-key "behaveplus:surface"}])
  (d/transact @db/conn [{:db/id 67 :module/translation-key "behaveplus:mortality"}])

  (d/transact @db/conn [{:translation/language 89 :translation/key "behaveplus:surface" :translation/translation "Surface"}])

  (d/transact @db/conn [{:translation/language 89 :translation/key "behaveplus:mortality" :translation/translation "Mortality"}])
  (d/transact @db/conn [{:translation/language 89 :translation/key "behaveplus:contain" :translation/translation "Contain"}])

  (def submodule-translations
    '[:find ?translation
      :in $ ?e
      :where [?e :application/module ?m]
      [?m :module/submodule ?s]
      [?s :submodule/translation-key ?key]
      [?t :translation/key ?key]
      [?t :translation/translation ?translation]])

  (def group-translations
    '[:find ?translation
      :in $ ?e
      :where [?e :application/module ?m]
      [?m :module/submodule ?s]
      [?s :submodule/group ?g]
      [?g :group/translation-key ?key]
      [?t :translation/key ?key]
      [?t :translation/translation ?translation]])

  (def variable-translations
    '[:find ?translation
      :in $ ?e
      :where [?e :application/module ?m]
      [?m :module/submodule ?s]
      [?s :submodule/group ?g]
      [?v :group/variable ?g]
      [?g :group/translation-key ?key]
      [?t :translation/key ?key]
      [?t :translation/translation ?translation]])

  )
