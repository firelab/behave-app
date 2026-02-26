(ns behave.print.views
  (:require [re-frame.core                          :refer [subscribe]]
            [reagent.dom                             :as rd]
            [behave.print.subs]
            [behave.components.results.graphs       :refer [result-graphs]]
            [behave.components.results.diagrams     :refer [result-diagrams]]
            [behave.components.results.matrices     :refer [result-matrices]]
            [behave.components.results.inputs.views :refer [inputs-table]]
            [behave.components.results.table        :refer [pivot-tables search-tables]]))

(defn- wizard-notes [notes]
  (when (seq notes)
    [:div.wizard-notes
     [:div.wizard-print__header "Run Notes"]
     (doall (for [[id & _rest :as note] notes]
              ^{:key id}
              (let [[_note-id note-name note-content] note]
                [:div.wizard-note
                 [:div.wizard-note__name note-name]
                 [:div.wizard-note__content note-content]])))]))

(defn- epoch->date-string [epoch]
  (.toDateString (js/Date. epoch)))

(defn print-page [{:keys [ws-uuid]}]
  (let [worksheet           @(subscribe [:worksheet ws-uuid])
        ws-date-created     (:worksheet/created worksheet)
        ws-version          (:worksheet/version worksheet)
        ws-description      (:worksheet/run-description worksheet)
        notes               @(subscribe [:wizard/notes ws-uuid])
        graph-data          @(subscribe [:worksheet/result-table-cell-data ws-uuid])]
    [:div.print
     [:div.print__header
      [:img {:src "/images/logo.svg"}]
      [:div.print__header__info
       (when ws-version [:div (str "Version: " ws-version)])
       [:div (str "Created: " (epoch->date-string ws-date-created))]]]
     (when ws-description
       [:div
        [:div.wizard-print__header "Run Description"]
        [:div ws-description]])
     [:div.wizard-print__header "Inputs"]
     [inputs-table ws-uuid]
     [wizard-notes notes]
     [:div.wizard-print__results
      [:div.wizard-print__header "Results"]
      [search-tables ws-uuid]
      [pivot-tables ws-uuid]
      [result-matrices ws-uuid]
      [result-graphs ws-uuid graph-data]
      [result-diagrams ws-uuid]]]))

(defn print-iframe! [ws-uuid]
  (let [overlay    (.createElement js/document "div")
        dialog     (.createElement js/document "div")
        header     (.createElement js/document "div")
        title      (.createElement js/document "span")
        btn-bar    (.createElement js/document "div")
        print-btn  (.createElement js/document "button")
        cancel-btn (.createElement js/document "button")
        iframe     (.createElement js/document "iframe")
        cleanup!   #(.remove overlay)]
    (set! (.-className overlay) "print-preview")
    (set! (.-className dialog) "print-preview__dialog")
    (set! (.-className header) "print-preview__header")
    (set! (.-textContent title) "Print Preview")
    (set! (.-className title) "print-preview__title")
    (set! (.-className btn-bar) "print-preview__buttons")
    (set! (.-textContent print-btn) "Print")
    (set! (.-className print-btn) "print-preview__btn print-preview__btn--print")
    (set! (.-textContent cancel-btn) "Cancel")
    (set! (.-className cancel-btn) "print-preview__btn print-preview__btn--cancel")
    (.appendChild header title)
    (.appendChild btn-bar print-btn)
    (.appendChild btn-bar cancel-btn)
    (.appendChild header btn-bar)
    (.appendChild dialog header)
    (let [page (.createElement js/document "div")]
      (set! (.-className page) "print-preview__page")
      (set! (.-className iframe) "print-preview__iframe")
      (.appendChild page iframe)
      (.appendChild dialog page))
    (.appendChild overlay dialog)
    (.appendChild (.querySelector js/document ".app-shell") overlay)
    (let [idoc  (.-contentDocument iframe)
          ihead (.-head idoc)
          ibody (.-body idoc)
          base  (.createElement idoc "base")]
      (set! (.-href base) (.-origin js/location))
      (.appendChild ihead base)
      (doseq [link (array-seq (.querySelectorAll js/document "link[rel=stylesheet]"))]
        (.appendChild ihead (.cloneNode link true)))
      (doseq [style (array-seq (.querySelectorAll js/document "style"))]
        (.appendChild ihead (.cloneNode style true)))
      (let [container (.createElement idoc "div")]
        (.appendChild ibody container)
        (rd/render [print-page {:ws-uuid ws-uuid}] container)))
    (.addEventListener cancel-btn "click" cleanup!)
    (.addEventListener print-btn "click"
      (fn []
        (.addEventListener (.-contentWindow iframe) "afterprint" cleanup!)
        (.print (.-contentWindow iframe))))))
