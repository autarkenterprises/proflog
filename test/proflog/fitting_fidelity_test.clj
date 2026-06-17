(ns proflog.fitting-fidelity-test
  "Interrogation tests anchoring the greenfield kernel to Melvin Fitting's
   'Tableaus for Logic Programming' (repo copy LPTableaus.pdf), section by
   section. Companion to docs/FITTING_FIDELITY_AUDIT.md.

   Each test reproduces a condition Fitting states, so that a fidelity verdict
   rests on a passing (or, for a documented divergence, a referenced failing)
   test rather than an assertion. These tests deliberately target gaps NOT
   already covered by equality_test.clj / fitting_programs_test.clj (which
   between them already establish the §2 worked programs P1/P2, the §8 move
   non-example, and most of the §5 equality battery)."
  (:refer-clojure :exclude [==])
  (:require [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.normalize :as normalize]
            [proflog.query :as query]
            [proflog.fitting-programs :as fitting]))

(defn- refutable?
  "Fitting's *disproof* direction: a closed tableau exists for `formula` itself."
  [formula]
  (seq (kernel/prove formula 1)))

(defn- open?
  "No closed tableau for `formula` within the proof bound `n`: the branch stays
   open, i.e. the formula is neither closed nor refuted by the raw kernel."
  ([formula] (open? formula 1))
  ([formula n] (empty? (kernel/prove formula n))))

;; ---------------------------------------------------------------------------
;; §3 Semantics — the supervaluation occurs-check subtlety (Fitting p.6, s_∅)
;; ---------------------------------------------------------------------------
;;
;; Fitting's empty-program model s_∅ makes ground `t = f(t)` FALSE, yet leaves
;; `(∃x)(x = f(x))` at ⊥ — "the smallest supervaluation model remains
;; uncommitted on the occurs check issue, even though it behaves correctly on
;; each instance." The greenfield engine realizes this as an asymmetry between
;; gamma proof-variables (occurs => close) and delta parameters (occurs =>
;; cannot bind AND cannot clash => branch stays open).

(deftest sec3-occurs-ground-and-variable-close-but-existential-stays-open
  (testing "ground occurs-shape c = f(c) closes (false) by free-constructor clash"
    (is (refutable? (ast/eq-lit (ast/app-term 'c)
                                (ast/app-term 'f (ast/app-term 'c))))))
  (testing "gamma-variable occurs-cycle x = f(x) closes by the occurs check"
    (ast/nom x
      (is (refutable? (ast/eq-lit (ast/var-term x)
                                  (ast/app-term 'f (ast/var-term x)))))))
  (testing "existential (∃x)(x = f(x)) stays OPEN — Fitting's ⊥, not closure"
    (ast/nom x
      (is (open? (ast/exists-form x
                   (ast/eq-lit (ast/var-term x)
                               (ast/app-term 'f (ast/var-term x)))))))))

;; ---------------------------------------------------------------------------
;; §3 Semantics — supervaluation truth vs ⊥ under program P1 (Fitting p.6)
;; ---------------------------------------------------------------------------
;;
;; "(∀x)(even(x) ∨ ¬even(x)) is true in the semantics ... On the other hand,
;;  (∀x)(even(x) ∨ odd(x)) is ⊥, essentially because there are weak Herbrand
;;  models in which there are 'non-standard' members."

(deftest sec3-p1-classical-tautology-succeeds
  (testing "(∀x)(even(x) ∨ ¬even(x)) succeeds: closes propositionally for the witness"
    (let [p1 (fitting/p1-program)]
      (ast/nom x
        (is (seq (query/query-succeeds
                   p1
                   (ast/forall-form x
                     (ast/or-form
                       (ast/pos-lit (fitting/app 'even (ast/var-term x)))
                       (ast/neg-lit (fitting/app 'even (ast/var-term x)))))
                   1 16)))))))

(deftest sec3-p1-even-or-odd-is-undefined
  (testing "(∀x)(even(x) ∨ odd(x)) is ⊥ under P1: neither succeeds nor fails"
    (let [p1 (fitting/p1-program)]
      (ast/nom x
        (is (= :unresolved
               (query/query-status
                 p1
                 (ast/forall-form x
                   (ast/or-form
                     (ast/pos-lit (fitting/app 'even (ast/var-term x)))
                     (ast/pos-lit (fitting/app 'odd (ast/var-term x)))))
                 {:timeout-ms 1500 :max-fuel 6 :poll-ms 0})))))))

;; ---------------------------------------------------------------------------
;; §4 Tableau rules — NNF realizes Fitting's uniform-notation negation duals
;; ---------------------------------------------------------------------------
;;
;; Table 1: ¬(X∧Y) is β, ¬(X∨Y) is α, ¬∀ is δ (∃), ¬∃ is γ (∀); plus ¬¬Z → Z.
;; The kernel has no runtime ¬ connective; normalize/negate-formula supplies the
;; duals, with the negated-existential clause body becoming the single-use
;; `once-forall` (the γ that must not re-enqueue).

(deftest sec4-nnf-realizes-fitting-negation-duals
  (ast/nom x
    (let [p (ast/pos-lit (ast/app-term 'p (ast/var-term x)))
          q (ast/pos-lit (ast/app-term 'q (ast/var-term x)))]
      (testing "¬(X∧Y) is disjunctive (β)"
        (is (= 'or (ast/tag-of (normalize/negate-formula (ast/and-form p q))))))
      (testing "¬(X∨Y) is conjunctive (α)"
        (is (= 'and (ast/tag-of (normalize/negate-formula (ast/or-form p q))))))
      (testing "¬∀ is existential (δ)"
        (is (= 'exists
               (ast/tag-of (normalize/negate-formula (ast/forall-form x p))))))
      (testing "¬∃ is the single-use universal once-forall (γ)"
        (is (= 'once-forall
               (ast/tag-of (normalize/negate-formula (ast/exists-form x p))))))
      (testing "¬¬Z normalizes back to Z"
        (is (= p (normalize/to-nnf (ast/not-form (ast/not-form p)))))))))

;; ---------------------------------------------------------------------------
;; §7 Soundness of reporting — query-status is never :inconsistent
;; ---------------------------------------------------------------------------
;;
;; For a consistent program no query may both succeed and fail. query-status
;; returns :inconsistent only if a closed tableau is found for BOTH A and ¬A,
;; which would be a soundness violation. It must never occur on Fitting's
;; programs.

(deftest sec7-query-status-is-never-inconsistent-on-fitting-programs
  (let [p1 (fitting/p1-program)
        p2 (fitting/p2-program)]
    (doseq [[label prog q]
            [["even(2)" p1 (ast/pos-lit (fitting/app 'even (fitting/numeral 2)))]
             ["odd(3)"  p1 (ast/pos-lit (fitting/app 'odd (fitting/numeral 3)))]
             ["win(4)"  p2 (ast/pos-lit (fitting/app 'win (fitting/numeral 4)))]
             ["win(3)"  p2 (ast/pos-lit (fitting/app 'win (fitting/numeral 3)))]]]
      (is (not= :inconsistent
                (query/query-status prog q
                                    {:timeout-ms 2000 :max-fuel 64 :poll-ms 0}))
          (str label " must not be reported inconsistent")))))
