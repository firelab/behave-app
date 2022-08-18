(ns user
  (:require [datascript.core :as d]
            [re-frame.core :as rf]
            [clojure.edn :as edn]
            [re-posh.core :as rp]
            [re-frisk.core :as re-frisk]
            [datom-compressor.interface :as c]
            [ds-schema-utils.interface :refer [->ds-schema]]
            [ajax.core :refer [ajax-request GET]]
            [ajax.protocols :as pr]
            [behave.schema.core :refer [all-schemas]]))


(re-frisk/enable)

(defn clear! [k]
  (rf/clear-sub k)
  (rf/clear-subscription-cache!))

(comment

  (rf/subscribe [:query '[:find ?e ?name
                          :where [?e :submodule/name ?name]]])
  (rf/subscribe [:pull '[* {:submodule/groups [* {:group/group-variables [* {:variable/_group_variables [*]}]}]}] 2727])

  (rf/subscribe [:query '[:find ?e :where [?e :variable/group-variables]]])

  (rf/subscribe [:pull '[*] 2393])
  (rf/subscribe [:query '[:find ?e ?key
                          :where [?e :help/key ?key]]])

  (def contain (first @(rf/subscribe [:query '[:find [?e] :where [?e :module/name "Contain"]]])))


  (rf/subscribe [:query '[:find  ?content
                          :in    $ ?key
                          :where [?e :help/key ?key]
                                 [?e :help/content ?content]]
                 ["behaveplus:contain:help"]])

  (rf/subscribe [:help/content "behaveplus:contain:help"])
  (rf/subscribe [:help/content "behaveplus:contain:input:fire:help"])
  (rf/subscribe [:help/content "behaveplus:contain:input:fire:help"])

)
