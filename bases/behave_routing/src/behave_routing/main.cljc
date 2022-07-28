(ns behave-routing.main
  (:require [clojure.walk :as walk]
            [bidi.bidi :as bidi]))

;; From https://github.com/WorksHub/client/blob/master/common/src/wh/routes.cljc
;; The collection routes defined here are supposed to have trailing
;; slashes. If a URL without the trailing slash is requested,
;; there will be a server-side redirect to the correct one.
(defn- add-trailing-slashes-to-roots
  [routes]
  (walk/postwalk
    (fn [x]
      (if (and (vector? x)
               (some-> x second map?)
               (some-> x second (get "")))
        (update x 1 #(let [root (get % "")]
                       (assoc % "/" root)))
        x))
    routes))

(def result-routes
  ["/results"
   [["" :results]
    [["/" :page] :ws/results]]])

(def module-routes
  ["/modules"
   [[["/" [keyword :module]]
     [[["/" [keyword :io]]
       [[["/" [keyword :submodule]] :ws/wizard]]]]]]])

(def worksheet-routes
  ["worksheets"
   {""           :ws/all
    ["/" :db/id] [["" :ws/overview]
                  module-routes
                  result-routes
                  ["/review" :ws/review]]}])

(def settings-routes
  ["settings"
   {""                    :settings/all
    ["/" [keyword :page]] :settings/page}])

(def tools-routes
  ["tools"
   {"" :tools/all
    ["/" [keyword :page]] :tools/page}])

(def workflow-routes
  ["workflow/"
   {"guided"      :workflow/guided
    "independent" :workflow/independent
    "import"      :workflow/import}])

(def routes
  (add-trailing-slashes-to-roots
    ["/" [["" :home]
          workflow-routes
          worksheet-routes
          settings-routes
          tools-routes]]))


(comment
  (bidi/path-for result-routes :results)
  (bidi/path-for result-routes :results-page :page "derp")

  (bidi/path-for routes :home)

  (bidi/match-route routes "/")
  (bidi/match-route routes "/settings/")
  (bidi/match-route routes "/settings/derp")

  (bidi/match-route routes "/tools")
  (bidi/match-route routes "/tools/derp")

  (bidi/match-route routes "/worksheets")
  (bidi/match-route routes "/worksheets/")
  (bidi/match-route routes "/worksheets/1")
  (bidi/match-route routes "/worksheets/1/results/first")
  (bidi/match-route routes "/worksheets/1/review")

  (bidi/match-route routes "/worksheets/1/modules/contain/output/fire")
  (bidi/match-route routes "/worksheets/2/modules/surface/input/fire")

  )
