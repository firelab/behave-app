(ns behave.modal.core
  "A single, app-wide modal stack.

  A feature opens a modal by dispatching an id and a context map:

  ```clojure
  (rf/dispatch [:modal/open :graph {:ws-uuid ws-uuid :output-uuid output-uuid}])
  ```

  and registers what that id renders with [[modal-content]]:

  ```clojure
  (defmethod modal-content :graph [{:keys [ws-uuid output-uuid]}] [graph-view …])
  ```

  Nothing has to be mounted per feature — [[behave.modal.views/modal-root]] is
  mounted once in the app shell and renders whatever is on the stack.")

(defmulti modal-content
  "Returns the hiccup rendered inside the modal for `:modal/id`.

  Dispatches on the `:modal/id` of the map passed to `:modal/open`; the rest of
  the map is the context the feature needs to render itself."
  :modal/id)

(defmethod modal-content :default [{id :modal/id}]
  [:div (str "No modal content registered for " (pr-str id))])
