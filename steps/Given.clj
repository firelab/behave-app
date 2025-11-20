(ns Given
  (:require [cucumber.steps :refer [Given]]
            [steps.worksheet :as ws]))

(Given "I have started a new Surface Worksheet in Guided Mode"
       (partial ws/start-worksheet [:surface]))

(Given "I have started a new Surface & Mortality Worksheet in Guided Mode"
       (partial ws/start-worksheet [:surface :mortality]))

(Given "I have started a new Surface & Crown Worksheet in Guided Mode"
       (partial ws/start-worksheet [:surface :crown]))

(Given "I have started a new Surface & Contain Worksheet in Guided Mode"
       (partial ws/start-worksheet [:surface :contain]))
