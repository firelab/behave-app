(ns behave-router.routes.worksheet)

(def worksheet-routes
  ["/worksheets"
   {""    :worksheets-handler
    [:id] :worksheet}])
;   ["/results"      :worksheet-results
;   ["/:module"      :worksheet-module-inputs
;    ["/inputs/"     :worksheet-module-inputs
;     ["/:submodule" :worksheet-submodule-inputs
;    ["/outputs"     :worksheet-module-outputs
;     ["/:submodule" :worksheet-submodule-outputs]]]]]]]])
