(ns proflog.sjas-correspondence-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [proflog.sjas-correspondence :as correspondence]
            [proflog.willard-sjas-code :as sjas-code]))

(deftest proof-symbol-audit-classifies-every-encoded-certificate-symbol
  (testing "the Track 2a audit covers every proof symbol that SJAS can encode"
    (is (= #{}
           (set/difference (set sjas-code/proof-symbols)
                           (set (keys correspondence/proof-symbol-classifications)))))))

(deftest proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  (testing "tableau, equality, procedure, and guarded constructors implemented by the SJAS proof checker are relevant"
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'split))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'atom-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'occurs-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'sjas-equal))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'eq-step))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'free-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'decompose))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'args))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'refl-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'neq-rigid))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'neq-store))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'neq-close))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'skip-true))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'eq-bind))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'eq-refl))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'par-bind))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'pos-call))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'alt))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'guarded-scope-done))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'guarded-seq-done))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol 'profiled))))))

(deftest implemented-proof-checker-constructors-are-relevant
  (testing "constructors consumed by the SJAS proof checker are not stale unresolved gaps"
    (doseq [sym '[eq-step
                  eq-triggered-call
                  eq-triggered-neg-call
                  eq-refl
                  eq-bind
                  par-bind
                  pos-call
                  neg-call
                  neg-call-alt
                  neg-call-guarded-alt
                  alt
                  guarded-alt
                  guarded-neg-alt
                  guarded-neg-alt-saturated
                  guarded-seq-step
                  guarded-seq-last
                  guarded-call-seq-step
                  guarded-residual-seq-step
                  guarded-residual-seq-last
                  guarded-scope-exists
                  guarded-scope-done
                  guarded-seq-done
                  guarded-call-seq-done
                  guarded-residual-seq-done
                  guard-saturation-done
                  guard-eq]]
      (is (= :relevant (:status (correspondence/classify-proof-symbol sym)))
          (str sym " should be classified as SJAS proof-checker structure")))))

(deftest implemented-sjas-profile-layer-markers-are-classified
  (testing "SJAS profile-layer evidence markers are not stale unresolved gaps"
    (doseq [sym '[profiled
                  willard-sjas-tableau0
                  willard-sjas-level1
                  willard-sjas-arithmetic
                  willard-sjas-code
                  willard-sjas-axiom-member
                  willard-sjas-theorem-code
                  willard-sjas-proof-check
                  willard-sjas-subst-code
                  willard-sjas-subst-proof-check]]
      (is (= :relevant (:status (correspondence/classify-proof-symbol sym)))
          (str sym " should be classified as implemented SJAS profile evidence")))
    (doseq [sym '[willard-sjas-fact
                  sjas-generated-axiom-member]]
      (is (= :excluded (:status (correspondence/classify-proof-symbol sym)))
          (str sym " should be classified as obsolete generated-host evidence")))))

(deftest proof-term-audit-reports-obligations-for-actual-proof-trees
  (testing "a decoded proof term can be summarized by Track 2a correspondence obligations"
    (let [audit (correspondence/audit-proof-term
                  '(split
                     close
                     (pos-call (eq-step close))))]
      (is (= #{'split 'close}
             (disj (:relevant-symbols audit) 'pos-call 'eq-step)))
      (is (= #{'split 'close 'pos-call 'eq-step}
             (:relevant-symbols audit)))
      (is (= #{}
             (:unresolved-symbols audit)))
      (is (= #{}
             (:unclassified-symbols audit))))))

(deftest proof-term-audit-has-no-unresolved-markers-for-implemented-sjas-evidence
  (testing "implemented SJAS proof predicate evidence is classified as relevant, not unresolved"
    (let [proof '(profiled willard-sjas-proof-check
                   (sjas-code-bytes)
                   (willard-sjas-theorem-code
                     (sjas-system-code-bytes (sjas-code-bytes)))
                   (conj
                     (profiled willard-sjas-arithmetic
                       (sjas-equal
                         (sjas-read-one)
                         (sjas-read-one)
                         (sjas-bind-done)))))
          audit (correspondence/audit-proof-term proof)]
      (is (= #{}
             (:unresolved-symbols audit)))
      (is (set/subset?
            '#{profiled
               willard-sjas-proof-check
               willard-sjas-theorem-code
               sjas-system-code-bytes
               sjas-code-bytes
               conj
               willard-sjas-arithmetic
               sjas-equal}
            (:relevant-symbols audit)))
      (is (= #{}
             (:unclassified-symbols audit))))))

(deftest proof-term-audit-classifies-reachable-code-reader-and-free-closure-tags
  (testing "reachable code-reader and free-constructor closure evidence must be part of the explicit correspondence audit"
    (let [audit (correspondence/audit-proof-term
                  '(conj
                     (sjas-code-arg 1 sjas-code-args-end)
                     (free-close)))]
      (is (= #{}
             (:unencodable-symbols audit)))
      (is (= #{}
             (:unclassified-symbols audit)))
      (is (= #{'conj 'free-close 'sjas-code-arg 'sjas-code-args-end}
             (:relevant-symbols audit)))
      (is (= #{}
             (:unresolved-symbols audit))))))

(deftest proof-term-audit-classifies-u-grounding-canonical-byte-evidence
  (testing "U-Grounding byte-reader evidence must stay inside the explicit proof-code alphabet"
    (let [audit (correspondence/audit-proof-term
                  '(sjas-ug-code-canonical-byte
                     7
                     (sjas-ug-code-byte-cons
                       (sjas-ug-code-mul64-shift)
                       (sjas-ug-code-canonical-byte))))]
      (is (= #{}
             (:unencodable-symbols audit)))
      (is (= #{}
             (:unclassified-symbols audit)))
      (is (= #{'sjas-ug-code-canonical-byte
               'sjas-ug-code-byte-cons
               'sjas-ug-code-mul64-shift}
             (:relevant-symbols audit))))))

(deftest profile-wrapper-audit-is-path-sensitive
  (testing "profiled wrappers have different relevance depending on their payload role"
    (is (= :probably-irrelevant
           (:status (correspondence/classify-profile-form
                      '(profiled willard-sjas-tableau0 (conj close))))))
    (is (= :relevant
           (:status (correspondence/classify-profile-form
                      '(profiled willard-sjas-arithmetic
                         (sjas-equal (sjas-read-one)
                                     (sjas-read-one)
                                     (sjas-bind-done)))))))
    (is (= :excluded
           (:status (correspondence/classify-profile-form
                      '(profiled first-order (close))))))))

(deftest generic-sidecar-proof-forms-are-explicitly-excluded
  (testing "optimized generic sidecars are not admitted by the SJAS proof predicate"
    (is (= :excluded
           (:status (correspondence/classify-proof-symbol 'propositional))))
    (is (= :excluded
           (:status (correspondence/classify-proof-symbol 'first-order))))
    (is (= :excluded
           (:status (correspondence/classify-proof-symbol 'lem-close))))
    (is (= :excluded
           (:status (correspondence/classify-proof-symbol 'skolemized))))
    (let [proof '(profiled propositional (conj (false-close)))
          audit (correspondence/audit-proof-term proof)]
      (is (= #{'propositional}
             (:excluded-symbols audit)))
      (is (= #{proof}
             (:excluded-profile-forms audit))))))

(deftest answer-overlay-proof-forms-are-explicitly-excluded
  (testing "answer-export proof constructors are not admitted by the SJAS proof predicate"
    (is (= :excluded
           (:status (correspondence/classify-proof-symbol 'query-pos-call))))
    (is (= :excluded
           (:status (correspondence/classify-proof-symbol 'query-neg-call))))
    (is (= :excluded
           (:status (correspondence/classify-proof-symbol 'query-neg-call-guarded-alt))))
    (is (= :excluded
           (:status (correspondence/classify-proof-symbol 'guarded-call-seq-defer))))
    (let [audit (correspondence/audit-proof-term
                  '(query-neg-call-guarded-alt
                     (guarded-call-seq-defer
                       (guarded-call-seq-done))))]
      (is (= #{'query-neg-call-guarded-alt 'guarded-call-seq-defer}
             (:excluded-symbols audit)))
      (is (= #{'guarded-call-seq-done}
             (set/intersection #{'guarded-call-seq-done}
                               (:relevant-symbols audit))))
      (is (= #{}
             (:unresolved-symbols audit))))))

(deftest proof-check-profile-wrapper-audit-allows-relation-specific-payloads
  (testing "SJAS proof-check profile forms carry relation-specific payload arity"
    (is (= :relevant
           (:status (correspondence/classify-profile-form
                      '(profiled willard-sjas-subst-proof-check
                         (sjas-code-bytes)
                         (willard-sjas-subst-code)
                         sjas-axiom)))))
    (let [audit (correspondence/audit-proof-term
                  '(profiled willard-sjas-level1
                     (profiled willard-sjas-subst-proof-check
                       (sjas-code-bytes)
                       (willard-sjas-subst-code)
                       sjas-axiom)))]
      (is (= #{'(profiled willard-sjas-subst-proof-check
                  (sjas-code-bytes)
                  (willard-sjas-subst-code)
                  sjas-axiom)}
             (:relevant-profile-forms audit))))))
