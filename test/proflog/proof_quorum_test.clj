(ns proflog.proof-quorum-test
  "Quorum proof-checking (ADR-0117): cross-validate the kernel's closure verdicts
   with three independent oracles over each genuine proof term, and require them
   to agree.

   1. kernel-as-prover  — `kernel/prove` (and `prove-program`) generates a proof;
   2. kernel-as-checker — the SAME relation run with the `proof` argument *bound*
      (the user's insight: if the kernel is correct it already is a checker);
   3. independent       — `proflog.proof-check/check`, a non-relational structural
      validator sharing no code with the kernel.

   Genuine proofs must be accepted by all oracles; mutated certificates must be
   rejected. Any disagreement localizes a bug in the kernel, the relational
   check-mode, the proof-term format, or the independent checker."
  (:refer-clojure :exclude [==])
  (:require [clojure.core.logic :as logic]
            [clojure.test :refer [deftest is testing]]
            [proflog.ast :as ast]
            [proflog.kernel :as kernel]
            [proflog.language :as language]
            [proflog.normalize :as normalize]
            [proflog.proof-check :as pc]
            [proflog.query :as query]
            [proflog.fitting-programs :as fitting]))

;; --- oracle 2: the kernel run as a checker (proof bound) -------------------

(defn- kernel-accepts?
  "Run the pure kernel relation with `proof` bound: succeeds iff the candidate is
   consistent with a closing search for `fml`."
  [fml proof]
  (boolean (seq (logic/run 1 [q]
                  (kernel/proveo fml '() '() '() proof)
                  (logic/== q true)))))

(defn- kernel-program-accepts?
  [prog fml proof fuel]
  (boolean (seq (logic/run 1 [q]
                  (kernel/prove-programo fml '() '() '() prog fuel proof)
                  (logic/== q true)))))

;; --- mutation operators ----------------------------------------------------

(defn- garble [_proof] (list 'bogus-rule))
(defn- drop-sub [proof] (cons (first proof) (butlast (rest proof))))
(defn- non-leaf? [proof]
  (and (sequential? proof) (seq proof) (seq (rest proof))))

;; --- corpus: genuine raw-kernel proofs (rich core + equality tags) ---------

(defn- genuine-raw
  "Returns [label formula proof] triples for raw-kernel (program-free) closures."
  []
  (ast/nom x
    (let [cases
          [[:free-clash
            (ast/eq-lit (ast/app-term 'zero) (ast/app-term 'one))]
           [:occurs-var
            (ast/eq-lit (ast/var-term x) (ast/app-term 'f (ast/var-term x)))]
           [:decompose
            (ast/eq-lit (ast/app-term 'pair (ast/app-term 'a) (ast/app-term 'b))
                        (ast/app-term 'pair (ast/app-term 'a) (ast/app-term 'c)))]
           [:conj-eq-neq
            (ast/and-form
              (ast/eq-lit (ast/app-term 'succ (ast/var-term x))
                          (ast/app-term 'succ (ast/app-term 'a)))
              (ast/neq-lit (ast/var-term x) (ast/app-term 'a)))]
           [:split
            (ast/or-form (ast/eq-lit (ast/app-term 'zero) (ast/app-term 'one))
                         (ast/eq-lit (ast/app-term 'zero) (ast/app-term 'one)))]
           [:witness-complementary
            (ast/exists-form x
              (ast/and-form (ast/pos-lit (ast/app-term 'p (ast/var-term x)))
                            (ast/neg-lit (ast/app-term 'p (ast/var-term x)))))]]]
      (mapv (fn [[label fml]] [label fml (first (kernel/prove fml 1))]) cases))))

;; --- corpus: genuine program proofs (call / guarded-alt tags) --------------

(defn- genuine-program
  "Returns [label program proven-formula proof fuel] for P1/P2 closures.
   query-succeeds proves the query by closing its negation, so we prove that
   negation directly to obtain a certificate we can re-check."
  []
  (let [p1 (fitting/p1-program)
        p2 (fitting/p2-program)
        mk (fn [label prog query fuel]
             (let [fml (normalize/negate-formula
                         (language/validate-query (:language prog) query))]
               [label prog fml (first (kernel/prove-program prog fml 1 fuel)) fuel]))]
    [(mk :p1-odd-1 p1 (ast/pos-lit (fitting/app 'odd (fitting/numeral 1))) 16)
     (mk :p2-win-4 p2 (ast/pos-lit (fitting/app 'win (fitting/numeral 4))) 64)]))

;; --- tests -----------------------------------------------------------------

(deftest quorum-accepts-genuine-raw-proofs
  (doseq [[label fml proof] (genuine-raw)]
    (testing (str "genuine raw proof: " label)
      (is (some? proof) (str label " should produce a proof"))
      (is (empty? (pc/unrecognized-tags proof))
          (str label " proof has only known tags: " (pr-str (pc/unrecognized-tags proof))))
      (is (pc/check proof) (str label " independent checker accepts"))
      (is (kernel-accepts? fml proof) (str label " kernel-as-checker accepts")))))

(deftest quorum-rejects-mutated-raw-proofs
  (doseq [[label fml proof] (genuine-raw)]
    (testing (str "garbage mutant of " label)
      (is (not (pc/check (garble proof))) "independent checker rejects garbage tag")
      (is (not (kernel-accepts? fml (garble proof))) "kernel-as-checker rejects garbage tag"))
    (when (non-leaf? proof)
      (testing (str "arity-truncated mutant of " label)
        (is (not (pc/check (drop-sub proof)))
            "independent checker rejects a dropped subproof")))))

(deftest quorum-accepts-genuine-program-proofs
  (doseq [[label prog fml proof fuel] (genuine-program)]
    (testing (str "genuine program proof: " label)
      (is (some? proof) (str label " should produce a proof"))
      (is (empty? (pc/unrecognized-tags proof))
          (str label " proof has only known tags: " (pr-str (pc/unrecognized-tags proof))))
      (is (pc/check proof) (str label " independent checker accepts"))
      (is (kernel-program-accepts? prog fml proof fuel)
          (str label " kernel-as-checker accepts")))))

(deftest quorum-rejects-mutated-program-proofs
  (doseq [[label prog fml proof fuel] (genuine-program)]
    (testing (str "garbage mutant of " label)
      (is (not (pc/check (garble proof))) "independent checker rejects garbage tag")
      (is (not (kernel-program-accepts? prog fml (garble proof) fuel))
          "kernel-as-checker rejects garbage tag"))))

;; --- the proof-term adequacy finding, made explicit ------------------------

(deftest proof-terms-are-pure-tag-trees-no-embedded-context
  (testing "every node is (tag subproof*) with a known tag — no formulas/terms/witnesses"
    ;; This is the evidence for ADR-0117's proof-term-adequacy limitation: because
    ;; the independent checker validates with a tag->arity grammar ALONE and still
    ;; accepts every genuine proof, the certificates carry no semantic context an
    ;; independent oracle could re-check against. The kernel-as-checker is what
    ;; supplies the semantic re-validation (by re-running the relation).
    (doseq [[label _ proof] (genuine-raw)]
      (is (every? symbol? (pc/all-tags proof))
          (str label " proof nodes are tagged only by rule symbols")))))
