# SJAS SelfCons Formula-Bearing Proof

## Context

ADR-0073 Track 1 requires the public SJAS proof predicate to accept the concrete
Tableau-0 Group-3 self-consistency statement through a formula-bearing semantic
tableau proof code. The certificate must not cite `sjas-axiom` as a shortcut and
must not route through the host proof checker.

## Red Evidence

- `sjas-proof-check-accepts-in-memory-formula-bearing-selfcons-tableau` failed
  before the final proof-shape repair. The proof checker rejected the full
  `AxiomConj(s) /\ not(SelfCons)` proof even though the smaller Group-3 core
  was already closing.
- Leaf comparison isolated the last axiom mismatch to leaf 6:
  the object target reconstructed `(once-forall [a_0] (or (neq ...) (pos ...)))`,
  while the certificate still encoded `(once-forall [a_0] (implies (eq ...) (pos ...)))`.
- The source audit briefly failed after the proof-node matcher introduced
  `conda`; the audit regex was scanning from `sjas-axiom-membero` into later
  unrelated top-level forms instead of only the axiom-member body.

## Implementation Notes

- The structural proof checker now accepts formula-bearing child nodes whose
  decoded formulas match the visible branch formula by exact equality,
  binder-renaming, compound recursive matching, or alpha-equivalence.
- Literal continuation and literal closure are guided by decoded child formulas
  so a fixed proof tree can select the intended pending branch formula without
  proof-rule tags.
- Universal and existential formula-bearing proof steps can descend through
  pre-instantiated child formulas using the canonical branch names carried by
  proof-code formula bytes.
- The SelfCons fixture now reconstructs the proof target inside the logic query,
  mirrors the kernel proof-antecedent transformation for axiom proof nodes, and
  encodes the conjunction path from `AxiomConj(s)` to the Group-3 antecedent.
- The source audit regex for `sjas-tableau-proof-closeo` and
  `sjas-axiom-membero` is now bounded to each top-level form, preserving the
  committed-choice guard without flagging unrelated matcher code.

## Green Evidence

- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-selfcons-core-tableau`
  passed: 1 test, 7 assertions, 0 failures, elapsed 1:59.05, maxrss 543532KB.
- A direct matcher probe after proof-side antecedent normalization reported
  `:all-exact true`, `:leaf6-match (true)`, and `:axiom-match (true)`.
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-in-memory-formula-bearing-selfcons-tableau`
  passed: 1 test, 8 assertions, 0 failures, elapsed 5:43.40, maxrss 690248KB.
- `lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-formula-bearing-selfcons-tableau`
  passed: 1 test, 8 assertions, 0 failures, elapsed 6:15.65, maxrss 1199264KB.
- `lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-formula-bearing-selfcons-certificate`
  passed: 1 test, 8 assertions, 0 failures, elapsed 10:36.34, maxrss 793644KB.
- `lein test :only proflog.willard-sjas-test/sjas-structural-proof-checker-has-no-proof-rule-tag-shortcuts`
  passed: 1 test, 37 assertions, 0 failures, elapsed 0:15.56, maxrss 259272KB.
- `lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route`
  passed after the bounded-regex repair: 1 test, 128 assertions, 0 failures,
  elapsed 0:12.76, maxrss 256000KB.
- `lein test-proflog-fast` passed: 165 tests, 656 assertions, 0 failures,
  elapsed 1:25.08, maxrss 359580KB.
- `lein test-proflog-extended` passed: 68 tests, 203 assertions, 0 failures,
  elapsed 3:16.22, maxrss 608336KB.
- `git diff --check` passed.

## Residual Follow-Up

- `lein test-proflog-sjas-focused` was progress-visible and advanced through
  the early SJAS tests, including
  `sjas-axiom-member-query-ignores-injected-generated-facts` in 198618.088 ms.
  It was stopped while running
  `sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry`
  after that selector exceeded the preceding axiom-member runtime envelope.
- The isolated selector
  `lein test :only proflog.willard-sjas-test/sjas-beta-axiom-member-decodes-application-codes-without-symbol-registry`
  was then run with a 420-second timeout and exited with code 124 without a
  failure body. That path is beta axiom-member system-code decoding rather than
  the SelfCons proof-check route repaired here.
