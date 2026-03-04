;;; clj-align-require.el --- Align :as/:refer columns in Clojure require forms -*- lexical-binding: t; -*-

;; Author: Ryan Sheperd
;; Version: 0.1.0
;; Package-Requires: ((emacs "25.1"))
;; Keywords: clojure, convenience, alignment
;; URL: https://github.com/rsheperd/clj-align-require

;; This file is not part of GNU Emacs.

;;; Commentary:

;; Aligns the `:as` and `:refer` columns in Clojure `:require` and
;; `:import` forms so that all keywords start at the same column.
;;
;; Usage:
;;   M-x clj-align-require
;;
;; Before:
;;   (:require [clojure.string :as str]
;;             [clojure.java.io :as io]
;;             [some.very.long.namespace :as long])
;;
;; After:
;;   (:require [clojure.string           :as str]
;;             [clojure.java.io          :as io]
;;             [some.very.long.namespace :as long])

;;; Code:

(defgroup clj-align-require nil
  "Alignment of Clojure require forms."
  :group 'clojure
  :prefix "clj-align-require-")

(defcustom clj-align-require-padding 1
  "Number of spaces between the longest namespace and its keyword."
  :type 'integer
  :group 'clj-align-require)

;;;###autoload
(defun clj-align-require ()
  "Align `:as` and `:refer` columns in the `:require` form at point."
  (interactive)
  (save-excursion
    (goto-char (point-min))
    (while (re-search-forward "(:\\(require\\|import\\)" nil t)
      (let ((req-start (match-beginning 0))
            (req-end (save-excursion (goto-char (match-beginning 0))
                                     (forward-sexp) (point)))
            entries max-ns-len)
        ;; Collect entries: (vec-start ns-start ns-end ns-len)
        (save-excursion
          (goto-char req-start)
          (down-list)  ; into (:require ...)
          (forward-sexp) ; skip :require keyword
          (setq max-ns-len 0)
          (while (< (point) req-end)
            (skip-chars-forward " \t\n")
            (when (and (< (point) req-end) (looking-at "\\["))
              (let* ((vec-start (point))
                     (ns-start (1+ vec-start)) ; after [
                     (ns-end (save-excursion (goto-char ns-start)
                                             (forward-sexp) (point)))
                     (ns-len (- ns-end ns-start))
                     (has-kw (save-excursion
                               (goto-char ns-end)
                               (skip-chars-forward " \t")
                               (looking-at ":\\(as\\|refer\\|rename\\)"))))
                (when has-kw
                  (push (list vec-start ns-start ns-end ns-len) entries)
                  (when (> ns-len max-ns-len)
                    (setq max-ns-len ns-len)))
                (goto-char vec-start)
                (forward-sexp)))))
        ;; Pad namespace symbols to align keywords (process in reverse to preserve positions)
        (dolist (entry entries)
          (pcase-let ((`(,_vec-start ,_ns-start ,ns-end ,ns-len) entry))
            (goto-char ns-end)
            (when (looking-at "[ \t]+")
              (replace-match ""))
            (insert (make-string (+ clj-align-require-padding (- max-ns-len ns-len)) ?\s))))))))

(provide 'clj-align-require)
;;; clj-align-require.el ends here
