(ns behave.wizard.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  :submodule-ids
  (fn [[_ module-id io]]
    (rf/subscribe [:query
                   '[:find [?e ...]
                     :in $ ?m ?io
                     :where [?m :module/submodules ?e]
                            [?e :submodule/io ?io]]
                   [module-id io]]))
  (fn [ids _] ids))
