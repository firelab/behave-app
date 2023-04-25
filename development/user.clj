(ns user)

(comment
  (require '[behave.core :as core])
  (core/init!)

  (require '[behave-cms.server :as cms])
  (cms/init-datahike!)

  (require '[clj-http.client :as client])
  (client/get "https://behave.sig-gis.com/sync" {:as :byte-array})

  )
