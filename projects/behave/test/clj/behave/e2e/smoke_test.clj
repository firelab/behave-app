(ns behave.e2e.smoke-test
  "E2E smoke tests for the BehavePlus application."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [behave.test.cdp :as cdp]
            [behave.test.dom :as dom]
            [behave.test.fixture :as fix]
            [behave.test.helpers :as h]
            [behave.test.lifecycle :as lifecycle]))

(use-fixtures :once fix/e2e-fixture)

(defn- session [] (fix/session))

(defn- wait-for-url-change!
  "Polls until the URL hash changes from `previous-url`."
  [cdp-session previous-url timeout-ms]
  (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [current (cdp/evaluate! cdp-session "window.location.href")]
        (if (not= current previous-url)
          current
          (do
            (when (> (System/currentTimeMillis) deadline)
              (throw (ex-info "URL did not change" {:expected-change-from previous-url})))
            (Thread/sleep 250)
            (recur)))))))

;;; Tests

(deftest app-launches-test
  (println "[smoke] app-launches-test: checking .page__main exists")
  (testing "app starts and displays the main page"
    (is (dom/exists? (session) ".page__main")
        "Main page container should exist"))
  (println "[smoke] app-launches-test: DONE"))

(deftest create-worksheet-test
  (println "[smoke] create-worksheet-test: START")

  (testing "home page shows New Run / Open cards"
    (println "[smoke]   navigating home...")
    (h/navigate-home! (session))
    (lifecycle/wait-for-app-ready! (session) 60000)
    (is (dom/wait-for! (session) ".card-group" 10000)
        "Card group should be visible")
    (let [titles (cdp/evaluate! (session)
                                "Array.from(document.querySelectorAll('.card__header__title'))
                      .map(function(el) { return el.textContent.trim(); })")]
      (println "[smoke]   home page card titles:" titles)
      (is (seq titles) "Should have card titles")))

  (testing "select New Run and navigate to workflow selection"
    (println "[smoke]   selecting 'New Run'...")
    (let [selected (h/select-card! (session) "New Run")]
      (is selected "Should be able to click New Run card"))
    (Thread/sleep 500)
    (is (dom/exists? (session) ".card--selected")
        "New Run card should be selected")
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[smoke]   clicking Next -> workflow selection")
      (h/click-next! (session))
      (wait-for-url-change! (session) url-before 5000)))

  (testing "workflow selection shows Guided/Standard options"
    (dom/wait-for! (session) ".workflow-select" 5000)
    (Thread/sleep 500)
    (let [titles (cdp/evaluate! (session)
                                "Array.from(document.querySelectorAll('.card__header__title'))
                      .map(function(el) { return el.textContent.trim(); })")]
      (println "[smoke]   workflow card titles:" titles)
      (is (some #(re-find #"(?i)standard" %) titles)
          "Should show Standard workflow option")))

  (testing "select Standard workflow and navigate to module selection"
    (println "[smoke]   selecting 'Standard'...")
    (let [selected (h/select-card! (session) "Standard")]
      (is selected "Should be able to select Standard workflow"))
    (Thread/sleep 500)
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[smoke]   clicking Next -> module selection")
      (h/click-next! (session))
      (wait-for-url-change! (session) url-before 5000)))

  (testing "module selection shows Surface, Crown, etc."
    (Thread/sleep 500)
    (let [titles (cdp/evaluate! (session)
                                "Array.from(document.querySelectorAll('.card__header__title'))
                      .map(function(el) { return el.textContent.trim(); })")]
      (println "[smoke]   module card titles:" titles)
      (is (some #(re-find #"(?i)surface" %) titles)
          "Should show Surface module option")))

  (testing "select Surface Only module"
    (println "[smoke]   selecting 'Surface Only'...")
    (let [selected (h/select-card! (session) "Surface Only")]
      (is selected "Should be able to select Surface Only"))
    (Thread/sleep 500)
    (is (dom/exists? (session) ".card--selected")
        "Surface Only card should be selected")
    (let [selected-title (h/selected-card-title (session))]
      (println "[smoke]   selected module:" selected-title)
      (is (re-find #"(?i)surface" (or selected-title ""))
          "Selected card should be Surface")))

  (testing "click Next to create worksheet"
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[smoke]   clicking Next -> create worksheet")
      (h/click-next! (session))
      (let [new-url (wait-for-url-change! (session) url-before 5000)]
        (println "[smoke]   worksheet URL:" new-url))))

  (println "[smoke] create-worksheet-test: DONE"))

(deftest open-existing-worksheet-test
  (println "[smoke] open-existing-worksheet-test: START")

  (testing "navigate to home and select Open existing run"
    (println "[smoke]   navigating home...")
    (h/navigate-home! (session))
    (lifecycle/wait-for-app-ready! (session) 60000)
    (dom/wait-for! (session) ".card-group" 10000)
    (println "[smoke]   selecting 'existing'...")
    (let [selected (h/select-card! (session) "existing")]
      (is selected "Should be able to click Open existing run card"))
    (Thread/sleep 500)
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[smoke]   clicking Next -> workflow selection")
      (h/click-next! (session))
      (wait-for-url-change! (session) url-before 5000)))

  (testing "select Standard workflow"
    (dom/wait-for! (session) ".workflow-select" 5000)
    (Thread/sleep 500)
    (println "[smoke]   selecting 'Standard'...")
    (let [selected (h/select-card! (session) "Standard")]
      (is selected "Should be able to select Standard workflow"))
    (Thread/sleep 500)
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[smoke]   clicking Next -> import page")
      (h/click-next! (session))
      (wait-for-url-change! (session) url-before 5000)))

  (testing "import page shows and we can inject a bp7 file"
    (let [url (cdp/evaluate! (session) "window.location.href")]
      (println "[smoke]   on import page:" url))
    (is (re-find #"import" (cdp/evaluate! (session) "window.location.href"))
        "Should be on the import page")
    (Thread/sleep 500)
    (println "[smoke]   importing behave-test.bp7...")
    (h/open-worksheet-file! (session) "/behave-test.bp7")
    (println "[smoke]   checking sync-loaded?...")
    (is (cdp/evaluate! (session)
                       "(function() {
            try {
              var db = re_frame.db.app_db.state;
              var k = cljs.core.keyword('sync-loaded?');
              return cljs.core.get_in(db, cljs.core.vector(cljs.core.keyword('state'), k));
            } catch(e) { return false; }
          })()")
        "sync-loaded? should be true after import"))

  (testing "click Next to open the imported worksheet"
    (let [url-before (cdp/evaluate! (session) "window.location.href")]
      (println "[smoke]   clicking Next -> open worksheet")
      (h/click-next! (session))
      (let [new-url (wait-for-url-change! (session) url-before 10000)]
        (println "[smoke]   opened worksheet URL:" new-url)
        (is (re-find #"worksheets/.+/standard" new-url)
            "Should navigate to the worksheet"))))

  (testing "worksheet is loaded with Surface outputs"
    (Thread/sleep 2000)
    (let [working-area (cdp/evaluate! (session)
                                      "document.querySelector('.working-area') ?
                           document.querySelector('.working-area').innerText.substring(0, 300) :
                           'no working-area'")]
      (println "[smoke]   working area (first 300 chars):" working-area)
      (is (re-find #"(?i)surface" (or working-area ""))
          "Worksheet should contain Surface module content"))

    (let [progress-steps (cdp/evaluate! (session)
                                        "Array.from(document.querySelectorAll('.progress-nob__label'))
                           .map(function(el) { return el.textContent.trim(); })")]
      (println "[smoke]   progress steps:" progress-steps)
      (is (seq progress-steps)
          "Should show wizard progress steps")))

  (println "[smoke] open-existing-worksheet-test: DONE"))
