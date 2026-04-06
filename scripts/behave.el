;;; behave.el --- Project-local Emacs helpers for behave-polylith

(defun behave-format-on-save ()
  "Run cljfmt fix + align_requires on save, then silently revert buffer."
  (let* ((project-root (locate-dominating-file buffer-file-name ".dir-locals.el"))
         (align-script (expand-file-name "scripts/align_requires.clj" project-root))
         (file         (buffer-file-name))
         (buf          (current-buffer))
         (cmd          (format "cd %s && bb %s %s && clojure -M:format fix %s"
                               (shell-quote-argument project-root)
                               (shell-quote-argument align-script)
                               (shell-quote-argument file)
                               (shell-quote-argument file))))
    (make-process
     :name    "clj-format-on-save"
     :buffer  nil
     :command (list "bash" "-c" cmd)
     :sentinel (lambda (_proc event)
                 (when (and (string-prefix-p "finished" event)
                            (buffer-live-p buf))
                   (with-current-buffer buf
                     (unless (buffer-modified-p)
                       (revert-buffer t t t))))))))

(defun behave-jack-in-cms ()
  "Start the VMS/CMS REPL via `cider-jack-in-clj&cljs` with the :dev:behave/cms alias."
  (interactive)
  (let ((cider-clojure-cli-aliases ":dev:behave/cms"))
    (cider-jack-in-clj&cljs nil)))

(provide 'behave)
;;; behave.el ends here
