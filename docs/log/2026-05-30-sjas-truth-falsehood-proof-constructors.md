# SJAS Truth and Falsehood Proof Constructors

Date: 2026-05-30
Branch: `adr-0073-sjas-correspondence-program`

## Track 1 Slice

This Track 1 slice expands the SJAS-local proof checker from the generated
certificate subset toward the ordinary semantic-tableau proof constructor set.
The checker already handled conjunction, disjunction, quantifiers,
literal saving, complementary closure, equality, disequality, reflected calls,
and arithmetic closure, but it did not consume the ordinary `false-close` and
`skip-true` proof constructors even though both are part of the encoded proof
alphabet used by Proflog's tableau kernels.

`false-close` is the branch closure for an explicit falsehood formula.
`skip-true` removes a truth formula and continues the branch with the remaining
agenda. Both are object-level tableau steps, not host-kernel shortcuts.

## Red/Green Evidence

The focused red test supplied a decoded target `(and true false)` with proof:

```clojure
(conj (skip-true (false-close)))
```

Before implementation the SJAS proof checker returned no result. After the
implementation it accepts the proof while `kernel/prove-programo` is redefined
to throw.

The end-to-end public regression encodes a proof certificate for the theorem
`true`; its negated theorem branch is `false`, and the checker accepts the
encoded `(conj (false-close))` certificate without delegating to the host proof
kernel.

The correspondence audit now classifies `skip-true` as relevant tableau
structure rather than an unresolved optimization-layer symbol. It preserves the
proof tree's treatment of truth formulas and therefore belongs with the
semantic-tableau constructors that Track 1 implements directly.

## Verification

```text
timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-truth-and-falsehood-constructors-without-kernel-validator
  Red before implementation:
  FAIL, expected successful proof-check result but got ().

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-proof-check-accepts-truth-and-falsehood-constructors-without-kernel-validator
  Ran 1 tests containing 1 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-tableau-proof-accepts-false-close-certificates
  Ran 1 tests containing 2 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.sjas-correspondence-test/proof-symbol-audit-exposes-relevant-and-unresolved-constructors
  Ran 1 tests containing 19 assertions.
  0 failures, 0 errors.

timeout -k 5s 180s lein test :only proflog.willard-sjas-test/sjas-profile-source-audit-rejects-host-proof-checker-route
  Ran 1 tests containing 32 assertions.
  0 failures, 0 errors.

timeout -k 10s 900s lein test :only proflog.willard-sjas-test/sjas-selfcons-demonstration-uses-substantive-proof-targets
  Ran 1 tests containing 6 assertions.
  0 failures, 0 errors.

timeout -k 10s 900s lein test-proflog-fast
  Ran 159 tests containing 595 assertions.
  0 failures, 0 errors.

timeout -k 10s 1200s lein test-proflog-extended
  Ran 68 tests containing 203 assertions.
  0 failures, 0 errors.
```
