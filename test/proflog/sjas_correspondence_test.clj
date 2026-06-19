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
           (:status (correspondence/classify-proof-symbol 'profiled))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol
                      'dsjas-tableau-proof-object))))
    (is (= :relevant
           (:status (correspondence/classify-proof-symbol
                      'dsjas-subst-prf-object))))))

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
                  willard-sjas-subst-proof-check
                  willard-sjas-tab1-proof-check]]
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

(deftest fragment-reachability-audit-covers-the-high-risk-aspects
  (testing "ADR-0099: the per-aspect audit covers exactly the three high-risk relevance-matrix rows with encodable :relevant constructors"
    (is (= #{:equality-extension :procedure-call-expansion :quantifier-instantiation}
           (set (keys correspondence/fragment-reachability-constructor-sets))))
    (doseq [[aspect syms] correspondence/fragment-reachability-constructor-sets]
      (is (seq syms) (str aspect " must name at least one constructor"))
      (is (= #{} (set/difference syms (set sjas-code/proof-symbols)))
          (str aspect " constructors must all be encodable proof symbols"))
      (is (every? #(= :relevant (:status (correspondence/classify-proof-symbol %))) syms)
          (str aspect " constructors must be Track 2a :relevant")))))

(deftest fragment-reachability-audit-flags-tags-and-clears-formula-bearing-certificates
  (testing "ADR-0099: per-aspect tags are reported; formula-bearing and axiom certificates clear all aspects"
    (let [absorbed (correspondence/audit-fragment-reachability '(2 12 7 (1 4)))]
      (is (false? (:reachable? absorbed)))
      (is (every? empty? (vals (:reachable-by-aspect absorbed)))))
    (is (false? (:reachable?
                  (correspondence/audit-fragment-reachability 'sjas-axiom))))
    (let [eq (correspondence/audit-fragment-reachability '(eq-step (refl-close)))
          call (correspondence/audit-fragment-reachability '(neg-call (alt)))
          quant (correspondence/audit-fragment-reachability '(univ (witness)))]
      (is (true? (:reachable? call)))
      (is (= #{'eq-step 'refl-close}
             (:equality-extension (:reachable-by-aspect eq))))
      (is (contains? (:procedure-call-expansion (:reachable-by-aspect call)) 'neg-call))
      (is (contains? (:procedure-call-expansion (:reachable-by-aspect call)) 'alt))
      (is (= #{'univ 'witness}
             (:quantifier-instantiation (:reachable-by-aspect quant)))))))

(deftest track-2a-relevance-matrix-has-no-unresolved-symbols
  (testing "ADR-0099 completion: every classified proof symbol has a resolved Track 2a status, none :unresolved"
    (is (= #{}
           (into #{}
                 (filter #(= :unresolved
                             (:status (correspondence/classify-proof-symbol %))))
                 (keys correspondence/proof-symbol-classifications)))
        "no proof symbol remains :unresolved after the Track 2a completion")))

(deftest path-a-rule-inventory-classifies-narrow-and-excluded-branches
  (testing "ADR-0103 Path A: the literal-Willard proof target has an executable branch inventory"
    (let [audit (correspondence/audit-path-a-narrow-rule-inventory)]
      (is (= 19 (:rule-count audit)))
      (is (= #{}
             (:unclassified-rule-ids audit)))
      (is (set/subset?
            #{:conjunction
              :complementary-literal-closure
              :negation-and-implication
              :disjunction}
            (:direct-willard-rule-ids audit)))
      (is (set/subset?
            #{:literal-save-agenda-continuation
              :not-false-agenda-continuation
              :true-agenda-continuation}
            (:lemma-rule-ids audit)))
      (is (set/subset?
            #{:disequality-progress-and-storage
              :profile-structural-closes
              :equality-triggered-reflected-calls
              :direct-reflected-calls}
            (:excluded-rule-ids audit)))
      (is (contains? (:path-a-open-obligations audit)
                     :agenda-ancestor-preservation)))))

(deftest path-b-dsjas-rule-inventory-covers-extended-apparatus
  (testing "ADR-0103 Path B: D_SJAS names every extended checker family and its remaining blockers"
    (let [audit (correspondence/audit-dsjas-rule-inventory)]
      (is (= 19 (:rule-count audit)))
      (is (= #{}
             (:unclassified-rule-ids audit)))
      (is (set/subset?
            #{:base-tableau
              :branch-bookkeeping
              :truth-normalization
              :quantifier
              :equality-theory
              :arithmetic-profile
              :axiom-membership
              :reflected-call
              :recursive-proof
              :substitution-proof}
            (:rule-families audit)))
      (is (set/subset?
            #{:sjas-axiom-size-accounting
              :recursive-proof-well-foundedness
              :literature-admissibility}
            (:open-obligations audit))))))

(deftest structural-checker-rule-inventory-covers-recorded-branch-lines
  (testing "ADR-0103: the branch inventory covers the ADR-0101 checker audit line ranges"
    (is (= [[6179 6209]
            [6210 6217]
            [6218 6236]
            [6237 6313]
            [6314 6363]
            [6364 6373]
            [6374 6392]
            [6393 6482]
            [6483 6596]
            [6597 6706]
            [6707 6754]
            [6755 6783]
            [6784 6805]
            [6806 6867]
            [6868 6889]
            [6890 6927]
            [6928 6985]
            [6986 7111]
            [7112 7130]]
           (mapv :line-range correspondence/sjas-structural-checker-rule-inventory)))))

(deftest path-a-narrow-correspondence-proof-discharges-lemma-obligations
  (testing "ADR-0103 Path A: the narrowed literal-Willard theorem is no longer only an inventory"
    (let [proof (correspondence/audit-path-a-narrow-correspondence-proof)]
      (is (= :proved (:verdict proof)))
      (is (= #{}
             (:open-obligations proof)))
      (is (= #{:agenda-ancestor-preservation
               :truth-constant-semantics
               :nnf-irrelevance
               :quantifier-freshness
               :gamma-parameter-admissibility
               :bounded-guard-correctness}
             (set (keys (:discharged-obligations proof)))))
      (is (= #{:literal-save-agenda-continuation
               :complementary-literal-closure
               :conjunction
               :forall-once-forall-expansion
               :exists-expansion
               :false-not-true-closure
               :not-false-agenda-continuation
               :double-negation-and-atomic-duals
               :negation-and-implication
               :negated-and-bounded-quantifier-duals
               :disjunction
               :additional-quantifier-expansions
               :true-agenda-continuation}
             (:proved-rule-ids proof)))
      (is (contains? (:excluded-rule-ids proof)
                     :profile-structural-closes)))))

(deftest path-b-extended-apparatus-proof-has-conclusive-track-2b-verdict
  (testing "ADR-0103 Path B: the current extended apparatus is conclusively separated from literal Willard D"
    (let [verdict (correspondence/audit-path-b-correspondence-verdict)]
      (is (= :impossible-for-current-domain
             (:literal-willard-track-2b-verdict verdict)))
      (is (= :track-2c-required
             (:dsjas-verdict verdict)))
      (is (set/subset?
            #{:sjas-axiom-citation-size-counterexample
              :non-willard-extended-rule-families}
            (:blocking-reasons verdict)))
      (is (= :formula-bearing-axiom-leaf-or-combined-object-required
             (:required-size-accounting-repair verdict)))
      (is (= #{}
             (:unclassified-rule-ids verdict))))))

(deftest dsjas-track2c-specification-selects-every-extended-rule-family
  (testing "ADR-0104 Track 2c: D_SJAS is an explicit selected apparatus, not implementation drift"
    (let [spec (correspondence/audit-dsjas-track2c-specification)]
      (is (= :D_SJAS (:apparatus spec)))
      (is (= :selected-apparatus (:status spec)))
      (is (set/subset?
            #{:base-tableau
              :branch-bookkeeping
              :truth-normalization
              :quantifier
              :equality-theory
              :arithmetic-profile
              :axiom-membership
              :reflected-call
              :recursive-proof
              :substitution-proof}
            (:rule-families spec)))
      (is (= #{}
             (:unclassified-rule-ids spec))))))

(deftest dsjas-track2c-combined-proof-object-repairs-axiom-citation-accounting
  (testing "ADR-0104 Track 2c: citations are measured as composite proof objects"
    (let [accounting (correspondence/audit-dsjas-proof-object-accounting)]
      (is (= :combined-proof-object
             (:selected-repair accounting)))
      (is (= #{:system-code :theorem-code :proof-code}
             (:tableau-citation-measured-components accounting)))
      (is (= #{:system-code :substitution-code :theorem-code :proof-code}
             (:substitution-citation-measured-components accounting)))
      (is (= 'dsjas-tableau-proof-object
             (get-in accounting [:proof-object-symbols :tableau-proof])))
      (is (= 'dsjas-subst-prf-object
             (get-in accounting [:proof-object-symbols :substitution-proof])))
      (is (= #{:proof-code}
             (:structural-measured-components accounting)))
      (is (true? (:adr-0102-counterexample-repaired? accounting))))))

(deftest tab1-proof-list-accounting-records-entry-validation
  (testing "ADR-0121: Tab-1 accounting records implemented entry validation"
    (let [accounting (correspondence/audit-dsjas-proof-object-accounting)]
      (is (= 'dsjas-tab1-proof-object
             (get-in accounting [:proof-object-symbols :tab1-proof-list])))
      (is (= #{:system-code :theorem-code :proof-list-code}
             (:tab1-citation-measured-components accounting)))
      (is (= :entry-validation-implemented
             (:tab1-entry-validation-status accounting))))))

(deftest tab1-roadmap-audit-reconciles-rank1-terminology
  (testing "ADR-0120: Tab-1 terminology is reconciled before implementation claims"
    (let [audit (correspondence/audit-tab1-proof-list-roadmap)]
      (is (= :Tab-1 (:apparatus audit)))
      (is (= :willard-sjas-tab1 (:profile audit)))
      (is (= {:pi-star-1 'pi-star-1?
              :sigma-star-1 'sigma-star-1?}
             (:public-intermediate-classifiers audit)))
      (is (= :host-basis-admission-only
             (get-in audit [:host-conveniences :pi-star-1-encodable?])))
      (is (= #{:proof-list-syntax
               :public-proof-object-coding
               :measured-tab1-object
               :tab1-selfcons-relation-symbols
               :terminology-reconciliation}
             (:adr-0120-scope audit)))
      (is (= :implemented
             (:tab1-theorem-reuse-status audit)))
      (is (= #{}
             (:deferred-obligations audit)))
      (is (= #{:Tab-2 :stronger-Tab-k}
             (:boundary-failure-variants audit))))))

(deftest self-extension-survey-selects-reflected-pair-axioms
  (testing "ADR-0123: Workstream C records the data-encoding survey before implementation"
    (let [audit (correspondence/audit-self-extension-data-encoding-survey)]
      (is (= :pairs-first (:selected-demo audit)))
      (is (= :reflected-beta (:implementation-layer audit)))
      (is (= #{:finite-beta-axiomatization
               :level-1-classifier-discipline
               :system-identity-change
               :group-three-selfcons-regeneration
               :focused-sjas-tractability}
             (:survey-criteria audit)))
      (is (= :selected
             (get-in audit [:candidates :fresh-pair-functions :verdict])))
      (is (= :implemented
             (get-in audit [:candidates :lists-from-pairs :verdict])))
      (is (= #{:fresh-pair-functions
               :lists-from-pairs}
             (:implemented-demos audit)))
      (is (= :too-weak
             (get-in audit [:candidates :tagged-constants-only :verdict])))
      (is (= #{:list-recursion
               :encoded-syntax-manipulation}
             (:deferred-self-extension-obligations audit))))))

(deftest boundary-failure-roadmap-keeps-witness-contract-open
  (testing "ADR-0124: Workstream B distinguishes variant surface from completed negative witnesses"
    (let [audit (correspondence/audit-boundary-failure-roadmap)]
      (is (= :workstream-b (:workstream audit)))
      (is (= :total-multiplication (:first-variant audit)))
      (is (= #{:total-multiplication
               :tab-2-or-stronger
               :xtab-or-lem-axiom}
             (:planned-variants audit)))
      (is (= [:reduced-reflected-beta-witness
              :full-generated-selfcons-contradiction-target]
             (:required-witness-stages audit)))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (:final-evidence-required audit)))
      (is (= :full-target-implemented
             (get-in audit [:variant-statuses :total-multiplication])))
      (is (= :full-target-implemented
             (get-in audit [:variant-statuses :tab-2-or-stronger])))
      (is (= :full-target-implemented
             (get-in audit [:variant-statuses :xtab-or-lem-axiom])))
      (is (= #{:reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target}
             (get-in audit [:completed-witness-stages :total-multiplication])))
      (is (= #{:reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target}
             (get-in audit [:completed-witness-stages :tab-2-or-stronger])))
      (is (= #{:reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target}
             (get-in audit [:completed-witness-stages :xtab-or-lem-axiom])))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (get-in audit [:open-obligations :total-multiplication])))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (get-in audit [:open-obligations :tab-2-or-stronger])))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (get-in audit [:open-obligations :xtab-or-lem-axiom])))
      (is (= {:kind :universal-lem-seed
              :witness-relation 'xtab-lem-demo
              :axiom-count 1
              :status :implemented
              :remaining-stage :completed}
             (get-in audit [:reduced-witnesses :xtab-or-lem-axiom])))
      (is (= {:kind :rank2-theorem-shape
              :report-helper 'tab2-or-stronger-reduced-witness-report
              :witness-formula "forall x. exists y. true"
              :outside-tab1-classifiers #{:pi-star-1
                                          :sigma-star-1
                                          :pi-star-1-encodable}
              :ordinary-tableau-validation :implemented
              :status :implemented
              :remaining-stage :completed}
             (get-in audit [:reduced-witnesses :tab-2-or-stronger])))
      (is (= {:kind :generated-selfcons-refutation
              :system-builder 'xtab-lem-reduced-witness-system
              :target-report 'xtab-lem-full-target-report
              :target-shape "AxiomConj(S_xtab_lem) /\\ not(SelfCons(S_xtab_lem))"
              :status :implemented
              :remaining-evidence #{:constructed-certificate
                                    :proof-search-synthesis}}
             (get-in audit [:full-targets :xtab-or-lem-axiom])))
      (is (= {:kind :generated-selfcons-refutation
              :system-builder 'tab2-or-stronger-full-target-system
              :target-report 'tab2-or-stronger-full-target-report
              :target-shape "AxiomConj(S_tab2_boundary) /\\ not(SelfCons(S_tab2_boundary))"
              :status :implemented
              :remaining-evidence #{:constructed-certificate
                                    :proof-search-synthesis}}
             (get-in audit [:full-targets :tab-2-or-stronger])))
      (is (= :implemented
             (get-in audit [:evidence-screens :total-multiplication :status])))
      (is (= :implemented
             (get-in audit
                      [:evidence-verifiers
                       :total-multiplication
                       :constructed-certificate
                       :status])))
      (is (= 'total-multiplication-constructed-certificate-validation
             (get-in audit
                     [:evidence-verifiers
                      :total-multiplication
                      :constructed-certificate
                      :validation-helper])))
      (is (= :implemented
             (get-in audit
                     [:evidence-verifiers
                      :xtab-or-lem-axiom
                      :constructed-certificate
                      :status])))
      (is (= 'xtab-lem-constructed-certificate-validation
             (get-in audit
                     [:evidence-verifiers
                      :xtab-or-lem-axiom
                      :constructed-certificate
                      :validation-helper])))
      (is (= :implemented
             (get-in audit
                     [:evidence-verifiers
                      :tab-2-or-stronger
                      :constructed-certificate
                      :status])))
      (is (= 'tab2-or-stronger-constructed-certificate-validation
             (get-in audit
                     [:evidence-verifiers
                      :tab-2-or-stronger
                      :constructed-certificate
                      :validation-helper])))
      (is (false? (:workstream-complete? audit))))))

(deftest tab2-certificate-validation-recorded-for-generated-target
  (testing "ADR-0138: Tab-2 constructed-certificate validation helper is advertised but not completed"
    (let [audit (correspondence/audit-boundary-failure-roadmap)
          verifier (get-in audit
                           [:evidence-verifiers
                            :tab-2-or-stronger
                            :constructed-certificate])]
      (is (= 'verify-boundary-constructed-certificate
             (:verifier-helper verifier)))
      (is (= 'tab2-or-stronger-constructed-certificate-validation
             (:validation-helper verifier)))
      (is (= #{:screened-candidate
               :matching-system-code
               :matching-selfcons-code
               :matching-certificate-kind
               :matching-target-formula
               :matching-proof-code
               :successful-proof-validation}
             (:requires verifier)))
      (is (= #{:constructed-certificate}
             (:completes-on-success verifier)))
      (is (= #{:proof-search-synthesis}
             (:leaves-open verifier)))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (get-in audit [:open-obligations :tab-2-or-stronger]))))))

(deftest boundary-proof-search-synthesis-probe-recorded-for-final-evidence
  (testing "ADR-0139: Workstream B advertises a synthesis probe without closing final evidence"
    (let [audit (correspondence/audit-boundary-failure-roadmap)]
      (doseq [variant [:total-multiplication
                       :xtab-or-lem-axiom
                       :tab-2-or-stronger]
              :let [probe (get-in audit
                                   [:evidence-probes
                                    variant
                                    :proof-search-synthesis])]]
        (is (= :implemented (:status probe)))
        (is (= 'boundary-proof-search-synthesis-report
               (:probe-helper probe)))
        (is (= 'boundary-proof-search-synthesis-plan
               (:plan-helper probe)))
        (is (= #{:generated-target
                 :fresh-proof-variable
                 :durable-log-path
                 :screen-boundary-evidence
                 :target-validation-helper}
               (:requires probe)))
        (is (= #{}
               (:completed-obligations probe)))
        (is (= correspondence/boundary-final-evidence-obligations
               (:remaining-obligations probe))))
      (is (false? (:workstream-complete? audit))))))

(deftest tab2-boundary-surface-distinguishes-negative-proof-list-variant
  (testing "ADR-0129: Tab-2-or-stronger is a Workstream B boundary surface, not Tab-1 completion"
    (let [surface (correspondence/audit-tab2-or-stronger-boundary-surface)]
      (is (= :tab-2-or-stronger (:variant surface)))
      (is (= :workstream-b (:workstream surface)))
      (is (= :surface-implemented (:status surface)))
      (is (= :willard-sjas-tab1 (:implemented-baseline-profile surface)))
      (is (= :rejected-from-workstream-a (:positive-workstream-status surface)))
      (is (= {:minimum-apparatus :Tab-2
              :exceeds-intermediate-classifiers #{:pi-star-1
                                                  :sigma-star-1}
              :risk :stronger-theorem-reuse}
             (:boundary-step surface)))
      (is (= #{:reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target
               :constructed-certificate
               :proof-search-synthesis}
             (:remaining-obligations surface)))
      (is (= #{:tab2-proof-checker
               :reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target}
             (:not-implemented surface)))
      (is (false? (:completion-claimed? surface))))))

(deftest xtab-lem-boundary-surface-distinguishes-axiom-schema-variant
  (testing "ADR-0130: Xtab/LEM-as-axiom is a Workstream B boundary surface, not ordinary tableau behavior"
    (let [surface (correspondence/audit-xtab-or-lem-boundary-surface)]
      (is (= :xtab-or-lem-axiom (:variant surface)))
      (is (= :workstream-b (:workstream surface)))
      (is (= :surface-implemented (:status surface)))
      (is (= :tableau-derived-lem (:baseline-treatment surface)))
      (is (= :not-a-tab1-extension (:positive-workstream-status surface)))
      (is (= {:packaging :logical-axiom-schema
              :source :xtab-or-law-of-excluded-middle
              :risk :lem-as-axiom}
             (:boundary-step surface)))
      (is (= #{:reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target
               :constructed-certificate
               :proof-search-synthesis}
             (:remaining-obligations surface)))
      (is (= #{:xtab-axiom-schema
               :lem-axiom-schema-profile
               :reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target}
             (:not-implemented surface)))
      (is (false? (:completion-claimed? surface))))))

(deftest xtab-lem-reduced-witness-records-reflected-beta-stage
  (testing "ADR-0133: Xtab/LEM reduced witness records the finite reflected beta seed"
    (let [audit (correspondence/audit-boundary-failure-roadmap)
          witness (get-in audit [:reduced-witnesses :xtab-or-lem-axiom])]
      (is (= :full-target-implemented
             (get-in audit [:variant-statuses :xtab-or-lem-axiom])))
      (is (= #{:reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target}
             (get-in audit [:completed-witness-stages :xtab-or-lem-axiom])))
      (is (= {:kind :universal-lem-seed
              :witness-relation 'xtab-lem-demo
              :axiom-count 1
              :status :implemented
              :remaining-stage :completed}
             witness))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (get-in audit [:open-obligations :xtab-or-lem-axiom])))
      (is (= :surface-implemented
             (:status (get-in audit [:variant-surfaces :xtab-or-lem-axiom]))))
      (is (false? (:workstream-complete? audit))))))

(deftest xtab-lem-full-target-records-generated-selfcons-stage
  (testing "ADR-0134: Xtab/LEM full target records generated SelfCons refutation stage"
    (let [audit (correspondence/audit-boundary-failure-roadmap)
          target (get-in audit [:full-targets :xtab-or-lem-axiom])]
      (is (= :full-target-implemented
             (get-in audit [:variant-statuses :xtab-or-lem-axiom])))
      (is (= #{:reduced-reflected-beta-witness
               :full-generated-selfcons-contradiction-target}
             (get-in audit [:completed-witness-stages :xtab-or-lem-axiom])))
      (is (= {:kind :generated-selfcons-refutation
              :system-builder 'xtab-lem-reduced-witness-system
              :target-report 'xtab-lem-full-target-report
              :target-shape "AxiomConj(S_xtab_lem) /\\ not(SelfCons(S_xtab_lem))"
              :status :implemented
              :remaining-evidence #{:constructed-certificate
                                    :proof-search-synthesis}}
             target))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (get-in audit [:open-obligations :xtab-or-lem-axiom])))
      (is (false? (:workstream-complete? audit))))))

(deftest xtab-lem-certificate-validation-recorded-for-generated-target
  (testing "ADR-0135: Xtab/LEM constructed-certificate validation helper is advertised but not completed"
    (let [audit (correspondence/audit-boundary-failure-roadmap)
          verifier (get-in audit
                           [:evidence-verifiers
                            :xtab-or-lem-axiom
                            :constructed-certificate])]
      (is (= :implemented (:status verifier)))
      (is (= 'verify-boundary-constructed-certificate
             (:verifier-helper verifier)))
      (is (= 'xtab-lem-constructed-certificate-validation
             (:validation-helper verifier)))
      (is (= #{:screened-candidate
               :matching-system-code
               :matching-selfcons-code
               :matching-certificate-kind
               :matching-target-formula
               :matching-proof-code
               :successful-proof-validation}
             (:requires verifier)))
      (is (= #{:constructed-certificate}
             (:completes-on-success verifier)))
      (is (= #{:proof-search-synthesis}
             (:leaves-open verifier)))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (get-in audit [:open-obligations :xtab-or-lem-axiom]))))))

(deftest boundary-evidence-screen-rejects-trivial-selfcons-evidence
  (testing "ADR-0127: ordinary SelfCons proofs are not boundary-failure evidence"
    (let [target {:variant :total-multiplication
                  :system-code 'S-total
                  :group-three-code 'SelfCons-total}
          citation {:variant :total-multiplication
                    :system-code 'S-total
                    :selfcons-code 'SelfCons-total
                    :evidence-kind :constructed-certificate
                    :certificate-kind :sjas-axiom
                    :theorem-code 'SelfCons-total
                    :uses-reduced-witness? false}
          structural {:variant :total-multiplication
                      :system-code 'S-total
                      :selfcons-code 'SelfCons-total
                      :evidence-kind :constructed-certificate
                      :certificate-kind :ordinary-selfcons-tableau
                      :uses-reduced-witness? true}
          wrong-target {:variant :total-multiplication
                        :system-code 'other-system
                        :selfcons-code 'other-SelfCons
                        :evidence-kind :constructed-certificate
                        :certificate-kind :selfcons-contradiction-certificate
                        :uses-reduced-witness? true}
          citation-screen (correspondence/screen-boundary-evidence
                            target
                            citation)
          structural-screen (correspondence/screen-boundary-evidence
                              target
                              structural)
          wrong-target-screen (correspondence/screen-boundary-evidence
                                target
                                wrong-target)]
      (is (= :rejected (:result citation-screen)))
      (is (contains? (:reasons citation-screen)
                     :ordinary-selfcons-citation))
      (is (contains? (:reasons citation-screen)
                     :missing-reduced-witness))
      (is (= :rejected (:result structural-screen)))
      (is (contains? (:reasons structural-screen)
                     :ordinary-selfcons-tableau))
      (is (= :rejected (:result wrong-target-screen)))
      (is (= #{:wrong-system-code
               :wrong-selfcons-code}
             (:reasons wrong-target-screen)))
      (is (= #{}
             (:completed-obligations citation-screen))))))

(deftest boundary-evidence-screen-keeps-nontrivial-candidates-pending
  (testing "ADR-0127: screened-in candidates still need proof verification"
    (let [target {:variant :total-multiplication
                  :system-code 'S-total
                  :group-three-code 'SelfCons-total}
          constructed {:variant :total-multiplication
                       :system-code 'S-total
                       :selfcons-code 'SelfCons-total
                       :evidence-kind :constructed-certificate
                       :certificate-kind :selfcons-contradiction-certificate
                       :uses-reduced-witness? true}
          synthesis-without-log (assoc constructed
                                       :evidence-kind :proof-search-synthesis)
          constructed-screen (correspondence/screen-boundary-evidence
                               target
                               constructed)
          synthesis-screen (correspondence/screen-boundary-evidence
                             target
                             synthesis-without-log)]
      (is (= :verification-required (:result constructed-screen)))
      (is (= #{}
             (:reasons constructed-screen)))
      (is (= #{}
             (:completed-obligations constructed-screen)))
      (is (= #{:constructed-certificate
               :proof-search-synthesis}
             (:remaining-obligations constructed-screen)))
      (is (= :rejected (:result synthesis-screen)))
      (is (= #{:missing-durable-synthesis-log}
             (:reasons synthesis-screen)))
      (is (= #{}
             (:completed-obligations synthesis-screen))))))

(deftest boundary-constructed-certificate-verifier-requires-screen-and-proof-validation
  (testing "ADR-0131: constructed certificate evidence needs both screen and proof-validation success"
    (let [target {:variant :total-multiplication
                  :system-code 'S-total
                  :group-three-code 'SelfCons-total
                  :target-formula '(and AxiomConj-total
                                        (not SelfCons-total))}
          candidate {:variant :total-multiplication
                     :system-code 'S-total
                     :selfcons-code 'SelfCons-total
                     :target-formula '(and AxiomConj-total
                                           (not SelfCons-total))
                     :evidence-kind :constructed-certificate
                     :certificate-kind :structural-tableau
                     :proof-code 'P-total
                     :uses-reduced-witness? true}
          validation {:variant :total-multiplication
                      :system-code 'S-total
                      :selfcons-code 'SelfCons-total
                      :target-formula '(and AxiomConj-total
                                             (not SelfCons-total))
                      :proof-code 'P-total
                      :certificate-kind :structural-tableau
                      :proof-valid? true
                      :validator :tableau-proof}
          verified (correspondence/verify-boundary-constructed-certificate
                     target
                     candidate
                     validation)
          screened-out (correspondence/verify-boundary-constructed-certificate
                         target
                         (assoc candidate :uses-reduced-witness? false)
                         validation)
          wrong-target (correspondence/verify-boundary-constructed-certificate
                         target
                         candidate
                         (assoc validation
                                :target-formula '(and OtherAxiomConj
                                                       (not SelfCons-total))))
          wrong-candidate-target
          (correspondence/verify-boundary-constructed-certificate
            target
            (assoc candidate
                   :target-formula '(and OtherAxiomConj
                                          (not SelfCons-total)))
            validation)
          wrong-proof (correspondence/verify-boundary-constructed-certificate
                        target
                        candidate
                        (assoc validation :proof-code 'OtherP))
          wrong-kind (correspondence/verify-boundary-constructed-certificate
                       target
                       candidate
                       (assoc validation :certificate-kind :sjas-axiom))
          wrong-target-code
          (correspondence/verify-boundary-constructed-certificate
            (assoc target :target-code 'TargetCode)
            (assoc candidate :target-code 'TargetCode)
            (assoc validation :target-code 'OtherTargetCode))
          failed-proof (correspondence/verify-boundary-constructed-certificate
                         target
                         candidate
                         (assoc validation :proof-valid? false))]
      (is (= :verified-intermediate-evidence (:result verified)))
      (is (= #{:constructed-certificate}
             (:completed-obligations verified)))
      (is (= #{:proof-search-synthesis}
             (:remaining-obligations verified)))
      (is (= :screen (:verification-stage screened-out)))
      (is (= :rejected (:result screened-out)))
      (is (contains? (:reasons screened-out)
                     :missing-reduced-witness))
      (is (= :proof-validation (:verification-stage wrong-target)))
      (is (= #{:wrong-target-formula}
             (:reasons wrong-target)))
      (is (= #{:wrong-target-formula}
             (:reasons wrong-candidate-target)))
      (is (= #{:wrong-proof-code}
             (:reasons wrong-proof)))
      (is (= #{:wrong-certificate-kind}
             (:reasons wrong-kind)))
      (is (= #{:wrong-target-formula}
             (:reasons wrong-target-code)))
      (is (= #{:proof-validation-failed}
             (:reasons failed-proof))))))

(deftest dsjas-track2c-size-lower-bound-covers-citation-and-structural-objects
  (testing "ADR-0104 Track 2c: the combined size repair has an explicit lower-bound argument"
    (let [audit (correspondence/audit-dsjas-combined-size-lower-bound)]
      (is (= :proved-under-code-injectivity
             (:status audit)))
      (is (= #{:tableau-axiom-citation
               :substitution-axiom-citation
               :formula-bearing-structural-tree}
             (:covered-proof-object-kinds audit)))
      (is (= #{}
             (:uncovered-proof-object-kinds audit)))
      (is (= :theorem-code-payload
             (get-in audit [:kind-arguments :tableau-axiom-citation :j-source])))
      (is (= :theorem-code-payload
             (get-in audit [:kind-arguments
                            :substitution-axiom-citation
                            :j-source])))
      (is (= #{:system-code :substitution-code :theorem-code :proof-code}
             (get-in audit [:kind-arguments
                            :substitution-axiom-citation
                            :measured-components])))
      (is (= :proof-code-formula-node-payloads
             (get-in audit [:kind-arguments :formula-bearing-structural-tree :j-source]))))))

(deftest dsjas-track2c-recursive-proof-and-subst-measure-is-explicit
  (testing "ADR-0104 Track 2c: recursive proof predicates have a discharged finite-call-graph proof"
    (let [audit (correspondence/audit-dsjas-recursive-well-foundedness)]
      (is (= :proved-for-finite-acyclic-proof-call-graphs
             (:status audit)))
      (is (= :least-fixed-point-over-proof-call-graph
             (:recursive-semantics audit)))
      (is (= :decoded-proof-code-payload
             (:primary-measure audit)))
      (is (= #{:proof-call-graph-height
               :decoded-proof-code-payload
               :structural-proof-node-count}
             (:well-founded-measures audit)))
      (is (= :not-a-proof-measure
             (:runtime-fuel-role audit)))
      (is (= :no-finite-derivation
             (:cyclic-call-policy audit)))
      (is (= #{:tableau-proof-structural-core
               :subst-prf-structural-core
               :dsjas-tableau-proof-structural-core
               :dsjas-subst-prf-structural-core}
             (:recursive-branches audit)))
      (is (= #{}
             (:unmeasured-recursive-branches audit)))
      (is (= #{:same-proof-code-self-call
               :mutual-proof-code-cycle
               :non-subtree-proof-code-reference}
             (:discharged-risks audit))))))

(deftest dsjas-track2c-literature-admissibility-is-explicitly-audited
  (testing "ADR-0104 Track 2c: literature admissibility is tracked per selected rule family"
    (let [audit (correspondence/audit-dsjas-literature-admissibility)]
      (is (= :proved-for-selected-dsjas-variant (:status audit)))
      (is (= :IS#_D_SJAS_beta (:apparatus-label audit)))
      (is (true? (:not-literal-willard-d? audit)))
      (is (set/subset?
            #{:natural-tree-coding
              :bounded-object-relations
              :semantic-tableau-shape
              :selected-apparatus-labeling
              :combined-proof-size-discipline
              :recursive-proof-well-foundedness
              :d-parametric-proof-predicate
              :system-code-reconstruction
              :primitive-or-bounded-macro-rules}
            (:discharged-criteria audit)))
      (is (= #{}
             (:open-criteria audit)))
      (is (= #{}
             (:inadmissible-rule-families audit)))
      (is (= #{:base-tableau
               :branch-bookkeeping
               :truth-normalization
               :quantifier
               :equality-theory
               :arithmetic-profile
               :axiom-membership
               :reflected-call
               :recursive-proof
               :substitution-proof}
             (set (keys (:rule-family-admissibility audit))))))))

(deftest dsjas-quantitative-ea-stability-proves-selected-combined-measure
  (testing "ADR-0108: D_SJAS is quantitatively EA-stable only for the selected combined proof-object measure"
    (let [audit (correspondence/audit-dsjas-quantitative-ea-stability)]
      (is (= :proved-for-selected-combined-proof-measure
             (:status audit)))
      (is (= :D_SJAS
             (:apparatus audit)))
      (is (= :IS#_D_SJAS_beta
             (:configuration audit)))
      (is (= {:proof-code-only :refuted
              :selected-combined-proof-measure :proved}
             (:measurement-verdicts audit)))
      (is (= {:sigma 1
              :tau 1
              :lambda 1/2
              :mu 0}
             (get-in audit [:quantitative-constants :a-stability])))
      (is (= {:sigma 1
              :tau 1
              :lambda 1/2
              :mu -1}
             (get-in audit [:quantitative-constants :e-stability])))
      (is (= :log-dsjas
             (get-in audit [:selected-length-measure :name])))
      (is (= #{:system-code :theorem-code :proof-code}
             (get-in audit [:selected-length-measure
                            :tableau-citation-measured-components])))
      (is (= #{:system-code :substitution-code :theorem-code :proof-code}
             (get-in audit [:selected-length-measure
                            :substitution-citation-measured-components])))
      (is (= #{:proof-code}
             (get-in audit [:selected-length-measure
                            :structural-measured-components])))
      (is (= :sjas-axiom-citation
             (get-in audit [:proof-code-only-counterexample
                            :counterexample])))
      (is (= 18
             (get-in audit [:proof-code-only-counterexample
                            :proof-bits])))
      (is (true?
            (get-in audit [:proof-code-only-counterexample
                           :unbounded-formula-payload?])))
      (is (= #{}
             (:open-obligations audit))))))

(deftest dsjas-quantitative-ea-stability-discharges-each-rule-family
  (testing "ADR-0108: every selected D_SJAS family has a bounded-satisfaction preservation clause"
    (let [audit (correspondence/audit-dsjas-quantitative-ea-stability)]
      (is (= #{:normed-open-branch-generalization
               :size-to-u-height-bound
               :a-stability-contradiction
               :e-stability-contradiction
               :selfcons1-consequence}
             (set (keys (:proof-lemmas audit)))))
      (is (= #{:base-tableau
               :branch-bookkeeping
               :truth-normalization
               :quantifier
               :equality-theory
               :arithmetic-profile
               :axiom-membership
               :reflected-call
               :recursive-proof
               :substitution-proof}
             (set (keys (:rule-family-preservation audit)))))
      (is (= #{}
             (into #{}
                   (keep (fn [[family clause]]
                           (when (not= :proved (:status clause))
                             family)))
                   (:rule-family-preservation audit))))
      (is (= :proved-under-code-injectivity
             (get-in audit [:proof-lemmas
                            :size-to-u-height-bound
                            :status])))
      (is (= :proved-by-dsjas-rule-preservation
             (get-in audit [:proof-lemmas
                            :normed-open-branch-generalization
                            :status]))))))

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
