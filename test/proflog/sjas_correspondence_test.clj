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
                  willard-sjas-subst-source-result
                  willard-sjas-subst-exprf
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

(deftest proof-symbol-fragment-boundary-covers-every-encoded-symbol
  (testing "Track 2b fragment admission is explicit for the whole proof-symbol alphabet"
    (is (= #{}
           (set/difference (set sjas-code/proof-symbols)
                           (set (keys correspondence/proof-symbol-fragment-boundaries)))))
    (doseq [[sym boundary] correspondence/proof-symbol-fragment-boundaries]
      (is (contains? #{:sjas-axiom-citation
                       :outside-first-fragment
                       :excluded}
                     (:fragment-status boundary))
          (str sym " must have a recognized fragment status"))
      (is (seq (:fragment-obligation boundary))
          (str sym " must describe its Track 2b obligation")))))

(deftest first-correspondence-fragment-admits-structural-tableaux-and-axiom-citations
  (testing "formula-bearing tableau nodes and bare axiom citations are distinct admitted fragments"
    (is (= :formula-bearing-tableau
           (:fragment-status
             (correspondence/audit-first-correspondence-fragment
               '(2 12 7 (1 4))))))
    (is (= #{}
           (:blocking-symbols
             (correspondence/audit-first-correspondence-fragment
               '(2 12 7 (1 4))))))
    (is (= :sjas-axiom-citation
           (:fragment-status
             (correspondence/audit-first-correspondence-fragment 'sjas-axiom))))
    (is (= #{'sjas-axiom}
           (:admitted-symbols
             (correspondence/audit-first-correspondence-fragment 'sjas-axiom))))))

(deftest legacy-proof-rule-tags-are-classified-but-not-admitted-to-first-fragment
  (testing "encoded legacy proof traces remain outside the formula-bearing correspondence fragment"
    (let [audit (correspondence/audit-first-correspondence-fragment
                  '(conj (false-close)))]
      (is (= :outside-first-fragment
             (:fragment-status audit)))
      (is (= #{'conj 'false-close}
             (:blocking-symbols audit)))
      (is (= :outside-first-fragment
             (:fragment-status
               (correspondence/classify-proof-symbol-fragment 'conj))))
      (is (= :outside-first-fragment
             (:fragment-status
               (correspondence/classify-proof-symbol-fragment 'false-close)))))))

(deftest sidecar-and-answer-overlay-evidence-remain-outside-first-fragment
  (testing "explicitly excluded encoded evidence does not enter the first correspondence fragment"
    (let [sidecar-audit (correspondence/audit-first-correspondence-fragment
                          '(profiled propositional (conj (false-close))))
          query-audit (correspondence/audit-first-correspondence-fragment
                        '(query-neg-call-guarded-alt
                           (guarded-call-seq-defer
                             (guarded-call-seq-done))))]
      (is (= :outside-first-fragment
             (:fragment-status sidecar-audit)))
      (is (= :outside-first-fragment
             (:fragment-status query-audit)))
      (is (contains? (:excluded-symbols sidecar-audit) 'propositional))
      (is (contains? (:excluded-symbols query-audit) 'query-neg-call-guarded-alt))
      (is (contains? (:excluded-symbols query-audit) 'guarded-call-seq-defer)))))

(deftest equality-reachability-audit-covers-the-equality-disequality-alphabet
  (testing "ADR-0098: the audited constructor set is exactly the encoded equality/disequality tags"
    (is (= #{} (set/difference correspondence/equality-disequality-constructor-symbols
                               (set sjas-code/proof-symbols)))
        "every audited equality/disequality constructor is an encodable proof symbol")
    (is (every? #(= :relevant (:status (correspondence/classify-proof-symbol %)))
                correspondence/equality-disequality-constructor-symbols)
        "the equality/disequality constructors are Track 2a :relevant")))

(deftest equality-reachability-audit-flags-tags-and-clears-formula-bearing-certificates
  (testing "ADR-0098: equality/disequality tags are reported; formula-bearing and axiom certificates are clear"
    (let [absorbed (correspondence/audit-equality-reachability '(2 12 7 (1 4)))]
      (is (= #{} (:equality-symbols-present absorbed)))
      (is (false? (:equality-reachable? absorbed))))
    (is (false? (:equality-reachable?
                  (correspondence/audit-equality-reachability 'sjas-axiom))))
    (let [tagged (correspondence/audit-equality-reachability
                   '(eq-step (neq-close (refl-close))))]
      (is (= #{'eq-step 'neq-close 'refl-close}
             (:equality-symbols-present tagged)))
      (is (true? (:equality-reachable? tagged))))))

(deftest structural-proof-tree-audit-reports-flat-node-size-and-shape
  (testing "flat formula-byte nodes expose finite tree and byte-size metrics"
    (let [audit (correspondence/audit-structural-proof-tree
                  '(3 10 11 12
                      (2 20 21)
                      (1 7)))]
      (is (:valid? audit))
      (is (= {:node-count 3
              :leaf-count 2
              :max-depth 2
              :formula-byte-count 6
              :child-counts [2 0 0]}
             (select-keys audit [:node-count
                                 :leaf-count
                                 :max-depth
                                 :formula-byte-count
                                 :child-counts])))
      (is (= [[10 11 12] [20 21] [7]]
             (:formula-byte-payloads audit))))))

(deftest structural-proof-tree-audit-reports-wide-node-size-and-shape
  (testing "wide formula-byte nodes use a non-empty byte list payload"
    (let [audit (correspondence/audit-structural-proof-tree
                  '((10 11 12 13)
                    (1 7)))]
      (is (:valid? audit))
      (is (= {:node-count 2
              :leaf-count 1
              :max-depth 2
              :formula-byte-count 5
              :child-counts [1 0]}
             (select-keys audit [:node-count
                                 :leaf-count
                                 :max-depth
                                 :formula-byte-count
                                 :child-counts])))
      (is (= [[10 11 12 13] [7]]
             (:formula-byte-payloads audit))))))

(deftest structural-proof-tree-audit-rejects-malformed-symbol-free-terms
  (testing "symbol-free is not enough: structural tableaux must have valid byte payloads and children"
    (let [short-flat (correspondence/audit-structural-proof-tree '(3 10 11))
          bad-byte (correspondence/audit-structural-proof-tree '(1 64))
          bad-child (correspondence/audit-structural-proof-tree '(1 7 (1 64)))]
      (is (false? (:valid? short-flat)))
      (is (contains? (:error-reasons short-flat) :flat-byte-count-mismatch))
      (is (false? (:valid? bad-byte)))
      (is (contains? (:error-reasons bad-byte) :invalid-byte))
      (is (false? (:valid? bad-child)))
      (is (contains? (:error-reasons bad-child) :invalid-byte)))))

(deftest first-correspondence-fragment-requires-valid-structural-tableaux
  (testing "the first-fragment audit distinguishes valid structural trees from malformed symbol-free lists"
    (let [valid-audit (correspondence/audit-first-correspondence-fragment
                        '(2 10 11 (1 7)))
          malformed-audit (correspondence/audit-first-correspondence-fragment
                            '(2 10))]
      (is (= :formula-bearing-tableau
             (:fragment-status valid-audit)))
      (is (:valid? (:structural-proof-summary valid-audit)))
      (is (= :malformed-structural-tableau
             (:fragment-status malformed-audit)))
      (is (false? (:valid? (:structural-proof-summary malformed-audit))))
      (is (contains? (:error-reasons (:structural-proof-summary malformed-audit))
                     :flat-byte-count-mismatch)))))
