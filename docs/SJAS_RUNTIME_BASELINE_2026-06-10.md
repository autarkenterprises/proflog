# SJAS Namespace Runtime Baseline (2026-06-10)

Per-var envelopes measured for ADR-0088 on the ADR-0090 patched tree
(bulk lane: one JVM, `proflog.focused-test-runner`, alphabetical; durable
log `test-runs/rebaseline/bulk-sweep-20260610T060650Z.log`). The three vars that failed
in this measurement pass were repaired by ADR-0091/ADR-0092 and re-timed
in the heavy table below. Heavy rows ran one JVM per var under
`timeout 1500`.

Bulk lane totals: 137 vars, wall 13:35.26, summary :SUMMARY pass=963 fail=5 error=1.

## Bulk lane (slowest first)

| Var | Elapsed |
|---|---:|
| sjas-level1-group-three-rejects-wrong-public-code-representation | 176.0 s |
| sjas-proof-check-accepts-formula-bearing-distinct-nested-existential-parameters | 129.8 s |
| sjas-proof-check-accepts-formula-bearing-equality-triggered-literal-closures | 56.5 s |
| sjas-proof-check-accepts-formula-bearing-equality-triggered-positive-calls | 55.4 s |
| sjas-proof-check-accepts-formula-bearing-equality-triggered-negative-calls | 38.7 s |
| sjas-u-grounding-subst-code-computes-level1-fixed-point | 22.8 s |
| sjas-tableau-proof-cites-level1-group-three-from-system-code | 20.4 s |
| sjas-arithmetic-runs-through-binary-relations | 17.4 s |
| sjas-u-grounding-bound-code-decoding-uses-byte-cons-relation | 15.7 s |
| sjas-proof-predicate-system-code-reconstruction-walks-equality-state | 15.4 s |
| sjas-proof-check-accepts-formula-bearing-stored-disequality-closures | 13.7 s |
| sjas-composite-examples-distinguish-beta-axioms-from-reflected-procedures | 11.4 s |
| sjas-tableau0-group-three-rejects-wrong-public-code-representation | 11.3 s |
| sjas-symbol-table-is-irrelevant-up-to-signature-isomorphism | 9.5 s |
| sjas-subst-code-relates-structural-substitution-codes | 9.1 s |
| sjas-axiom-member-query-ignores-injected-generated-facts | 7.5 s |
| sjas-source-builder-accepts-prefix-program-sections | 7.3 s |
| sjas-system-builder-generates-groups-and-reflected-boundary | 7.1 s |
| sjas-proof-code-decoder-checks-wide-formula-bearing-tableau-nodes | 7.0 s |
| sjas-proof-predicates-do-not-require-source-preprocessing-registry | 5.9 s |
| sjas-subst-prf-reconstructs-axiom-basis-without-system-registry | 5.9 s |
| sjas-proof-check-accepts-formula-bearing-complementary-literal-closures | 4.9 s |
| sjas-proof-check-accepts-formula-bearing-negated-atomic-duals | 4.7 s |
| sjas-tableau-proof-cites-tableau0-group-three-from-system-code | 4.5 s |
| sjas-proof-check-accepts-formula-bearing-disequality-storage | 4.4 s |
| sjas-proof-check-accepts-formula-bearing-guarded-negative-reflected-bodies | 4.3 s |
| sjas-arithmetic-supports-answer-and-partial-synthesis-modes | 3.9 s |
| sjas-reflected-axiom-member-decodes-application-codes-without-symbol-registry | 3.8 s |
| sjas-tableau0-axiomconj-reconstructs-zero-one-selfcons-target | 3.8 s |
| sjas-proof-check-accepts-formula-bearing-negative-reflected-alternatives | 3.2 s |
| sjas-u-grounding-tableau-proof-checks-numeral-system-theorem-and-proof-codes | 3.2 s |
| sjas-proof-predicates-check-reflected-calls-from-system-code | 3.1 s |
| sjas-tableau-proof-ignores-injected-generated-axiom-member-facts | 3.0 s |
| sjas-proof-predicates-ignore-external-runtime-clauses | 2.9 s |
| sjas-proof-predicates-check-reflected-calls-without-symbol-registry | 2.9 s |
| sjas-proof-check-accepts-formula-bearing-negated-bounded-quantifier-expansions | 2.9 s |
| sjas-tableau-proof-cites-fixed-axiom-groups-from-system-code | 2.9 s |
| sjas-proof-check-accepts-formula-bearing-bounded-quantifier-expansions | 2.8 s |
| sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry | 2.0 s |
| sjas-tableau-proof-accepts-axiom-citation-certificates | 1.8 s |
| sjas-selfcons-demonstration-uses-substantive-proof-targets | 1.7 s |
| sjas-proof-check-accepts-formula-bearing-quantifier-variable-children | 1.6 s |
| sjas-u-grounding-syntax-predicates-decode-numeral-codes | 1.5 s |
| sjas-proof-code-discriminator-splits-axiom-and-substantive-certificates | 1.4 s |
| sjas-proof-check-accepts-formula-bearing-positive-false-arithmetic-closures | 1.3 s |
| sjas-proof-code-decoder-checks-formula-bearing-tableau-nodes | 1.3 s |
| sjas-proof-check-accepts-formula-bearing-negative-reflected-calls | 1.2 s |
| sjas-axiom-conj-reconstructs-fixed-group-one-axioms | 1.1 s |
| sjas-proof-check-accepts-formula-bearing-rigid-disequality-continuations | 1.1 s |
| sjas-proof-check-accepts-formula-bearing-and-true-false-tableaux | 1.1 s |
| sjas-proof-check-accepts-formula-bearing-positive-reflected-calls | 1.0 s |
| sjas-proof-check-accepts-formula-bearing-quantifier-expansions | 1.0 s |
| sjas-syntax-predicates-decode-formula-godel-codes | 0.7 s |
| sjas-proof-code-decoder-round-trips-equality-triggered-atom-closure-evidence | 0.7 s |
 reflected-call-header-match-and-nonmatch-are-explicit-relations | 0.6 s |
| sjas-subst-source-result-computes-explicit-proof-antecedent | 0.6 s |
| sjas-proof-check-accepts-formula-bearing-negated-quantifier-expansions | 0.6 s |
| sjas-proof-checker-rejects-legacy-proof-rule-tag-certificates | 0.6 s |
| sjas-proof-check-accepts-formula-bearing-equality-continuations | 0.6 s |
| sjas-proof-check-accepts-formula-bearing-negated-disjunction-tableaux | 0.5 s |
| sjas-proof-code-decoder-round-trips-byte-payload-evidence | 0.5 s |
| sjas-proof-check-accepts-formula-bearing-negated-implication-tableaux | 0.5 s |
| sjas-profile-source-audit-rejects-host-proof-checker-route | 0.5 s |
| sjas-tableau-proof-rejects-generic-profiled-sidecar-certificates | 0.5 s |
| sjas-syntax-class-predicates-accept-implies-codes | 0.4 s |
| sjas-syntax-predicates-decode-application-codes-without-symbol-registry | 0.4 s |
| sjas-formula-codes-preserve-trailing-zero-embedded-code-payloads | 0.4 s |
| sjas-proof-check-accepts-formula-bearing-arithmetic-closures | 0.4 s |
| sjas-proof-check-accepts-formula-bearing-negated-conjunction-tableaux | 0.4 s |
| sjas-tableau-proof-rejects-answer-overlay-query-certificates | 0.4 s |
| sjas-proof-check-accepts-formula-bearing-reflexive-disequality-closures | 0.3 s |
| sjas-proof-check-accepts-formula-bearing-implication-tableaux | 0.3 s |
| sjas-proof-check-accepts-formula-bearing-right-first-conjunction-tableaux | 0.3 s |
| sjas-tableau0-selfcons-godel-code-is-publicly-printable | 0.3 s |
| sjas-proof-check-accepts-byte-list-formula-bearing-false-nodes | 0.2 s |
| sjas-proof-check-accepts-formula-bearing-guarded-scope-reflected-bodies | 0.2 s |
| sjas-compact-code-byte-reader-interprets-byte-numerals-arithmetically | 0.2 s |
| sjas-proof-check-accepts-formula-bearing-disjunction-tableaux | 0.2 s |
| sjas-proof-check-accepts-formula-bearing-equality-contradiction-closures | 0.1 s |
| sjas-level1-group-three-restricts-pair-to-pi-star-1 | 0.1 s |
 large-tableau-proof-zero-limit-does-not-materialize-report | 0.1 s |
| sjas-proof-check-accepts-formula-bearing-double-negation-tableaux | 0.1 s |
| sjas-system-does-not-generate-axiom-member-fact-registry | 0.1 s |
| sjas-level1-group-three-uses-substitution-proof-vocabulary | 0.1 s |
| sjas-formal-codes-are-godel-byte-terms | 0.1 s |
| sjas-level1-group-three-uses-selfcons-skeleton-code | 0.1 s |
| sjas-level1-bounded-contradiction-probe-records-timing | 0.1 s |
| sjas-proof-check-preserves-delayed-sibling-scope-after-quantifiers | 0.1 s |
| sjas-proof-codes-encode-formula-bearing-tableau-nodes-without-rule-tags | 0.1 s |
| sjas-system-does-not-generate-proof-antecedent-registry | 0.1 s |
| sjas-tableau0-selfcons-targets-zero-equals-one | 0.1 s |
| sjas-system-rejects-non-pi-star-1-reflected-basis | 0.1 s |
| sjas-system-builder-axiom-formula-includes-fixed-group-one | 0.1 s |
| sjas-object-symbol-index-decoding-separates-reserved-and-user-symbols | 0.1 s |
| sjas-app-arity-decoders-destructure-arity-byte-once | 0.0 s |
| sjas-structural-proof-checker-preserves-delayed-sibling-environments | 0.0 s |
| sjas-u-grounding-code-format-emits-numeral-codes-without-code-constructors | 0.0 s |
| sjas-structural-proof-checker-has-no-proof-rule-tag-shortcuts | 0.0 s |
| sjas-public-compact-code-readers-parse-presented-byte-numerals | 0.0 s |
| sjas-static-code-table-lookups-avoid-membero-scheduling | 0.0 s |
 kernel-proof-hooks-avoid-host-optional-dispatch | 0.0 s |
| sjas-structural-recursive-proof-predicate-closures-use-object-relations | 0.0 s |
| sjas-structural-proof-checker-uses-proof-free-equality-progression | 0.0 s |
| sjas-structural-arithmetic-closure-uses-proof-free-arithmetic | 0.0 s |
| sjas-proof-facing-dispatch-does-not-use-committed-choice | 0.0 s |
| sjas-profile-languages-have-binary-u-grounding-shape | 0.0 s |
| sjas-structural-recursive-proof-predicate-closures-avoid-answer-proof-wrappers | 0.0 s |
| sjas-structural-proof-checker-does-not-duplicate-guided-branches | 0.0 s |
| sjas-embedded-payload-decoders-check-header-before-payload-fresh | 0.0 s |
| sjas-structural-proof-checker-does-not-consume-runtime-fuel | 0.0 s |
| sjas-subst-prf-substitution-axiom-branch-validates-system-code | 0.0 s |
| sjas-complementary-literal-closure-uses-proof-free-atom-unifier | 0.0 s |
| sjas-proof-codes-encode-equality-triggered-positive-call-evidence | 0.0 s |
| sjas-code-format-dispatch-does-not-read-source-registry | 0.0 s |
| sjas-proof-codes-encode-existential-guarded-scope-evidence | 0.0 s |
| sjas-proof-codes-encode-nested-equality-closure-evidence | 0.0 s |
| sjas-numerals-are-binary-composed-terms | 0.0 s |
| sjas-proof-codes-encode-u-grounding-canonical-byte-evidence | 0.0 s |
| sjas-proof-codes-encode-recursive-guarded-call-sequence-evidence | 0.0 s |
| sjas-proof-certificates-preserve-generic-profiled-sidecar-evidence | 0.0 s |
| sjas-proof-codes-are-byte-strings-with-symbol-bit-lower-bound | 0.0 s |
| sjas-proof-codes-encode-occurs-check-closure-evidence | 0.0 s |
| sjas-proof-codes-encode-answer-overlay-evidence | 0.0 s |
| sjas-proof-codes-encode-equality-triggered-atom-closure-evidence | 0.0 s |
| sjas-formula-classifiers-cover-bounded-and-unbounded-shapes | 0.0 s |
| sjas-proof-codes-encode-guard-equality-saturation-evidence | 0.0 s |
| sjas-proof-codes-encode-byte-payload-evidence | 0.0 s |
| sjas-byte-codes-preserve-sequence-length-and-trailing-zeroes | 0.0 s |
| sjas-proof-codes-encode-saturated-guarded-negative-call-evidence | 0.0 s |
| sjas-proof-codes-encode-guarded-negative-call-evidence | 0.0 s |
| sjas-proof-codes-encode-equality-triggered-negative-call-evidence | 0.0 s |
| sjas-proof-codes-encode-positive-equality-step-evidence | 0.0 s |
| sjas-proof-codes-encode-proof-variable-disequality-closure-evidence | 0.0 s |
| sjas-proof-codes-encode-negative-call-alternative-evidence | 0.0 s |
| sjas-formula-classifiers-close-delta-star-0-under-connectives | 0.0 s |
| sjas-proof-codes-encode-minimal-arithmetic-close-certificates | 0.0 s |
| sjas-u-grounding-codes-preserve-trailing-zero-byte-sequences | 0.0 s |

## Heavy lane (final tree, one JVM per var, timeout 1500)

| Var | Result | Elapsed |
|---|---|---:|
| `sjas-structural-code-predicates-accept-non-generated-formula-codes` | Ran 1 tests containing 6 assertions. | 1:32.78 |
| `sjas-subst-code-computes-general-formula-code-substitution` | Ran 1 tests containing 6 assertions. | 1:44.21 |
| `sjas-subst-code-decodes-user-symbols-without-symbol-registry` | Ran 1 tests containing 3 assertions. | 1:38.41 |
| `sjas-subst-prf-checks-selfcons-fixed-point-certificate` | exceeds envelope (timeout 1500) | 25:00+ |
| `sjas-subst-prf-rejects-selfcons-complement-axiom-certificate` | exceeds envelope (timeout 1500) | 25:00+ |
| `sjas-subst-prf-uses-substitution-code-independently-of-theorem-code` | Ran 1 tests containing 3 assertions. | 0:51.91 |
| `sjas-tableau0-and-level1-query-generated-axioms-through-selected-profile` | Ran 1 tests containing 8 assertions. | 1:03.39 |
| `sjas-tableau0-selfcons-negating-witness-separates-external-proflog` | Ran 1 tests containing 2 assertions. | 0:44.21 |
| `sjas-tableau-proof-accepts-formula-bearing-true-theorem-certificates` | Ran 1 tests containing 6 assertions. | 1:01.65 |
