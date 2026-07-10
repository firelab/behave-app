(ns cucumber.element
  (:import [org.openqa.selenium By WebElement WrapsDriver JavascriptExecutor
            ElementClickInterceptedException]))

(defn attr-value
  "Get an element attribute value."
  [^WebElement e k]
  (.getAttribute e k))

(defn clear!
  "Clear an element."
  [^WebElement e]
  (.clear e))

(defn click!
  "Click on an element, robust to overlap by a fixed element (e.g. the app's fixed
   `page__footer`). Scrolls the element to the viewport center so it clears the footer,
   then clicks; if the native click is still intercepted, falls back to a JS click."
  [^WebElement e]
  (let [driver (.getWrappedDriver ^WrapsDriver e)]
    (try
      (.executeScript ^JavascriptExecutor driver
                      "arguments[0].scrollIntoView({block:'center',inline:'center'});"
                      (into-array Object [e]))
      (catch Exception _ nil))
    (try
      (.click e)
      (catch ElementClickInterceptedException _
        (.executeScript ^JavascriptExecutor driver
                        "arguments[0].click();"
                        (into-array Object [e]))))))

(defn css-value
  "Get an element's CSS Value."
  [^WebElement e k]
  (.getAttribute e k))

(defn displayed?
  "Get whether element is displayed."
  [^WebElement e]
  (.isDisplayed e))

(defn dom-attr
  "Get an element's DOM attribute."
  [^WebElement e k]
  (.domAttribute e k))

(defn dom-property
  "Get an element's DOM property."
  [^WebElement e k]
  (.domProperty e k))

(defn find-el
  "Find an element."
  [e ^By by]
  (.findElement e by))

(defn find-els
  "Find elements."
  [e ^By by]
  (.findElements e by))

(defn enabled?
  "Whether an element is enabled."
  [^WebElement e]
  (.isEnabled e))

(defn location
  "Get an element's location."
  [^WebElement e]
  (.getLocation e))

(defn rect
  "Get an element's coordinates."
  [^WebElement e]
  (.getRect e))

(defn selected?
  "Whether an element is selected."
  [^WebElement e]
  (.isSelected e))

(defn send-keys!
  "Send keys into element."
  [^WebElement e s]
  (.sendKeys e (into-array "" [s])))

(defn size
  "Get an element's size."
  [^WebElement e]
  (.getSize e))

(defn submit!
  "Submit an element."
  [^WebElement e]
  (.submit e))

(defn tag-name
  "Get an element's tag name."
  [^WebElement e]
  (.tagName e))

(defn text
  "Get an element's text."
  [^WebElement e]
  (.text e))

