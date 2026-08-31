(ns behave.test-namespaces
  "Single source of truth for the CLJS test suite.

  Requiring this namespace loads every test namespace (plus the app
  event/sub namespaces the tests exercise). Both [[behave.test-runner]]
  (browser, /api/test) and [[behave.headless-test-runner]] (CI, `bb
  test:ci`) require ONLY this namespace and invoke figwheel's
  `run-tests`/`run-tests-async` with no namespace arguments — those
  macros expand to every loaded namespace containing tests, so the two
  suites cannot drift.

  To add a test namespace: require it here. Nothing else to update."
  (:require
   [behave.contain-test]
   [behave.crown-test]
   [behave.diagram-test]
   [behave.events]
   [behave.help.subs]
   [behave.modal-test]
   [behave.mortality-test]
   [behave.results-table-test]
   [behave.shading-test]
   [behave.solver-test]
   [behave.subs]
   [behave.surface-test]
   [behave.test-solver-generators]
   [behave.test-solver-queries]
   [behave.tests-used-in-fixtures]
   [behave.units-test]
   [behave.utils-test]
   [behave.vms.subs]
   [behave.wizard.events]
   [behave.wizard.subs]
   [behave.worksheet-events-test]
   [behave.worksheet-subs-test]
   [behave.worksheet.events]
   [behave.worksheet.subs]))
