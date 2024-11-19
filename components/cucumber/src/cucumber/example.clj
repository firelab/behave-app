(ns cucumber.example
  (:require
   [clojure.test       :refer [is]]
   [cucumber.steps     :refer [run-step Given When Then And]]
   [cucumber.by        :as by]
   [cucumber.element   :as e]
   [cucumber.utils     :as u]
   [cucumber.webdriver :as w]))

(Given "I go to collect.earth"
      (fn [{:keys [driver]}]
        (w/goto driver "https://collect.earth")))

(Given "I am a visitor"
      (fn [{:keys [driver] :as ctx}]
        (w/maximize driver)
        (run-step "I go to collect.earth" ctx)
        (let [wait (w/wait driver 10)]
          (.until wait (w/presence-of (by/css "input"))))))

(When "I go to the login screen"
      (fn [{:keys [driver]}]
        (let [button (e/find-el driver (by/css "button[type='button'].btn.btn-lightgreen.btn-sm"))] ;; seconds
          (e/click! button)
          (println (w/title driver)))))

(And "I login"
     (fn [{:keys [driver]}]
       (let [wait (w/wait driver 10)]
         (.until wait (w/presence-of (by/css "input"))
                 (let [email-input (e/find-el driver (by/id "email"))
                       password-input (e/find-el driver (by/id "password"))
                       submit-btn (e/find-el driver (by/css "button[type='submit']"))]
                   (e/click! email-input)
                   (e/send-keys! email-input "user@example.com")
                   (e/click! password-input)
                   (e/send-keys! password-input "password")
                   (e/click! submit-btn))))))

(When "I search for an institution"
      (fn [{:keys [driver]}]
        (let [input (e/find-el driver (by/id "input"))] ;; seconds
          (e/click! input)
          (e/send-keys! input "Example"))))

(Then "I can see my institutions"
      (fn [{:keys [driver]}]
        (let [_ (w/wait driver 2)]
          (u/sleep 2000)
          (let [tree (e/find-el driver (by/css "ul.tree"))
                institutions (e/find-els tree (by/css "li"))]
            (is (= 1 (count institutions)))))))

(Given "I am a User"
       (fn [ctx]
         (run-step "I go to collect.earth" ctx)
         (run-step "I go to the login screen" ctx)
         (run-step "I login" ctx)))

(Then "I can see matching institutions"
      (fn [{:keys [driver]}]
        (let [_ (w/wait driver 2)]
          (u/sleep 2000)
          (let [tree (e/find-el driver (by/css "ul.tree"))
                institutions (e/find-els tree (by/css "li"))]
            (is (= 12 (count institutions)))))))
