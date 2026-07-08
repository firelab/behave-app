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

(defcustom behave-test-url "http://localhost:8081/api/test"
  "URL of the behave CLJS test page (served by `behave.handlers').
The route renders `behave.views/render-tests-page', which instantiates
the WASM module and only then loads the `app-testing.js' bundle whose
main is `behave.test-runner'. Prefer this over the figwheel
/figwheel-extra-main/testing page: that host page never sets
`window.Module' before the bundle loads, so the `behave.lib.*'
namespaces that read `js/Module' in top-level defs fail."
  :type 'string
  :group 'behave)

(defun behave-test ()
  "Open the behave CLJS test page in a browser and run the suite.
Requires the behave dev build (compile-dev) to already be running, e.g.
via `cider-jack-in-clj&cljs' with the default :dev:behave/app alias.
Loading `behave-test-url' evaluates `behave.test-runner', which loads
the VMS, runs the tests, and renders results into the `app-testing'
element via cljs-test-display."
  (interactive)
  (browse-url behave-test-url))

(provide 'behave)
;;; behave.el ends here
