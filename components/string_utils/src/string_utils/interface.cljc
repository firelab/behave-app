(ns string-utils.interface
  (:require [string-utils.core :as c]))

(def ^{:argslist '[s]
       :doc "Converts `s` to a string. Removes the colon in front of keywords."}
  ->str c/->str)

(def ^{:argslist '[s]
       :doc "Converts `s` to a a snake_case string."}
  ->snake c/->snake)

(def ^{:argslist '[s]
       :doc "Converts `s` to a a kebab-case string."}
  ->kebab c/->kebab)

(def ^{:argslist '[s]
       :doc "Converts `s` in camelCase to a a snake_case string."}
  camel->snake c/camel->snake)

(def ^{:argslist '[s]
       :doc "Converts `s` in camelCase to a a kebab-case string."}
  camel->kebab c/camel->kebab)

(def ^{:argslist '[s]
       :doc "Converts `s` in capitalizeCase to a a 'sentence case' string."}
  capitalize->sentence c/capitalize->sentence)

(def ^{:argslist '[s]
       :doc      "Converts `s` with all capitalized words."}
  capitalize->words c/capitalize-words)

(def ^{:argslist '[s]
       :doc "Converts `s` in kebab-case to CapitalizeCase string."}
  kebab->capitalize c/kebab->capitalize)

(def ^{:argslist '[s]
       :doc "Converts `s` in snake-case to CapitalizeCase string."}
  snake->capitalize c/snake->capitalize)

(def ^{:argslist '[s]
       :doc "Converts `s` in kebab-case to camelCase string."}
  kebab->camel c/kebab->camel)

(def ^{:argslist '[s]
       :doc "Converts `s` in snake-case to camelCase string."}
  snake->camel c/snake->camel)

(def ^{:argslist '[& s]
       :doc "Converts multiple strings into a snake-cased, colon-delimited string.

            Example:
            (snake-key \"Hello World\" \"App Name\") ; => \"hello_world:app_name\""}
  snake-key c/snake-key)

(def ^{:argslist '[& s]
       :doc "Converts multiple strings into a kebab-cased, colon-delimited string.

            Example:
            (kebab-key \"Hello World\" \"App Name\") ; => \"hello-world:app-name\""}
  kebab-key c/kebab-key)

(def ^{:argslist '[s end]
       :doc "Prepends `start` to `s` as long as `s` doesn't already start with `start`."}
  start-with c/start-with)

(def ^{:argslist '[s end]
       :doc "Appends `end` to `s` as long as `s` doesn't already end with `end`."}
  end-with c/end-with)

(def ^{:argslist '[s]
       :doc
       "Splits `s` on commas and/or spaces.
        For example:
       `(split-commas-or-spaces \"15, 20,25 30\") ; => '(\"15\" \"20\" \"25\" \"30\")`"}
  split-commas-or-spaces #'c/split-commas-or-spaces)
