(ns behave.modal.core
  "App-wide modal stack. Open one with `[:modal/open id ctx]` and register what
  that id renders with [[modal-content]]; [[behave.modal.views/modal-root]] is
  mounted once in the app shell, so features mount nothing themselves.")

(defmulti modal-content
  "Hiccup rendered inside the modal, dispatched on the `:modal/id` given to
  `:modal/open`. The rest of the map is the feature's context."
  :modal/id)

(defmethod modal-content :default [{id :modal/id}]
  [:div (str "No modal content registered for " (pr-str id))])
