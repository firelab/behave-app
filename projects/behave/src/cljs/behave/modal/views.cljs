(ns behave.modal.views
  (:require [behave.components.core :as c]
            [behave.modal.core      :refer [modal-content]]
            [behave.modal.events]
            [behave.modal.subs]
            [re-frame.core          :as rf]))

(defn modal-root
  "Renders the top-most modal on the stack. Mounted once, in the app shell."
  []
  (when-let [{:keys [modal/title modal/size modal/icon] :as current}
             @(rf/subscribe [:modal/current])]
    [c/modal {:title          title
              :icon           icon
              :size           (or size :medium)
              :close-on-click #(rf/dispatch [:modal/close])
              :content        [modal-content current]}]))
