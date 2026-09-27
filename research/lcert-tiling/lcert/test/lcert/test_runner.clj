(ns lcert.test-runner
  "Runs the fast or the extended suite and exits non-zero on any failure.

  Usage (see bin/test-fast and bin/test-extended):
    clojure -M:test   -m lcert.test-runner fast [ns ...]
    clojure -M:ansatz -m lcert.test-runner extended [ns ...]

  With namespace arguments only those namespaces run, which is how a single
  red/green step is exercised.  The extended suite first loads the
  Ansatz-verified kernel and switches the runtime measures to it, so every
  language test is re-run on the verified functions."
  (:require [clojure.test :as t]))

(def fast-namespaces
  "The language's own tests, which need no Ansatz, and every example of
  TUTORIAL.md (lcert.tutorial-test), so the tutorial cannot drift.  The last
  two are the assessor's reflect experiments (proflog ADR-0143 setup):
  reflect at ordinary types, and the fixed-cap failure at a resource type."
  '[lcert.syntax-test
    lcert.encode-test
    lcert.reduce-test
    lcert.typing-test
    lcert.check-test
    lcert.eval-test
    lcert.core-test
    lcert.tutorial-test
    lcert.ordinary-reflect-test
    lcert.resource-reflect-test
    lcert.charged-reflect-test
    lcert.caser-test
    lcert.lazy-supply-test
    lcert.minting-agent-test
    lcert.world-reflect-test])

(def extended-namespaces
  "Tests of the Ansatz kernel itself, run before the fast suite is repeated,
  and the slower probes: the embedding of PA (step 1 of Proposition 5), and
  the assessor's higher-order reflect shapes with the delegation chain
  (proflog ADR-0143 setup)."
  '[lcert.verified-test lcert.charged-test lcert.supply-test lcert.kripke-test lcert.pa-test
    lcert.higher-order-reflect-test])

(defn -main [suite & only]
  (let [nss (cond
              (seq only)             (map symbol only)
              (= suite "fast")       fast-namespaces
              (= suite "extended")   (concat extended-namespaces fast-namespaces)
              :else (throw (ex-info "suite must be fast or extended" {:suite suite})))]
    (when (= suite "extended")
      ;; Loading the kernel runs the Ansatz elaborator and kernel on every
      ;; definition and theorem; a failure there aborts the suite.
      (require 'lcert.verified 'lcert.charged 'lcert.supply 'lcert.kripke)
      ((requiring-resolve 'lcert.kernel/use-verified!)))
    (apply require nss)
    (let [{:keys [fail error]} (apply t/run-tests nss)]
      (shutdown-agents)
      (System/exit (if (zero? (+ fail error)) 0 1)))))
