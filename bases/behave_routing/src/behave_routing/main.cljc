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

(def ^:private result-routes
  ["/results"
   [["" :ws/results]
    [["/" [keyword :results-page]] :ws/results-settings]]])

(def ^:private module-routes
  ["/modules"
   [[["/" :module]
     [[["/" [keyword :io]]
       [[["/" :submodule] :ws/wizard]]]]]]])

(def ^:private worksheet-routes
  ["worksheets"
   {""              :ws/all
    "/guided"       :ws/guided
    "/independent"  :ws/independent
    "/import"       :ws/import
    ["/" :ws-uuid] [["" :ws/overview]
                     module-routes
                     result-routes
                     ["/review" :ws/review]]}])

(def ^:private settings-routes
  ["settings"
   {""                    :settings/all
    ["/" [keyword :page]] :settings/page}])

(def ^:private tools-routes
  ["tools"
   {"" :tools/all
    ["/" [keyword :page]] :tools/page}])

(def ^:private workflow-routes
  ["workflow/"
   {"guided"      :workflow/guided
    "independent" :workflow/independent
    "import"      :workflow/import}])

(def ^:private demo-routes
  ["demo/"
   {"chart"   :demo/chart
    "diagram" :demo/diagram}])

(def routes
  (add-trailing-slashes-to-roots
   ["/" [["" :home]
         demo-routes
         workflow-routes
         worksheet-routes
         settings-routes
         tools-routes]]))

(defn results-path [ws-uuid]
  (bidi/path-for routes :ws/results :ws-uuid ws-uuid))

(defn review-path [ws-uuid]
  (bidi/path-for routes :ws/review :ws-uuid ws-uuid))

(defn settings-path [page]
  (bidi/path-for routes :settings/page :page page))

(defn tools-path [page]
  (bidi/path-for routes :tools/page :page page))

(defn worksheet-path [ws-uuid]
  (bidi/path-for routes :ws/overview :ws-uuid ws-uuid))

(defn wizard-path [ws-uuid module io submodule]
  (bidi/path-for routes :ws/wizard :ws-uuid ws-uuid :module module :io io :submodule submodule))

(comment
  (bidi/path-for result-routes :results)
  (bidi/path-for result-routes :results-page :page "derp")

  (bidi/path-for routes :home)

  (bidi/path-for routes :ws/results :id 1)

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

  (bidi/match-route routes "/worksheets/1/results")
  (bidi/match-route routes "/worksheets/1/results/settings")

  )
